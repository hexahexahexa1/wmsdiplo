package com.wmsdipl.core.service;

import com.wmsdipl.contracts.dto.UndoLastScanResultDto;
import com.wmsdipl.core.domain.Location;
import com.wmsdipl.core.domain.Pallet;
import com.wmsdipl.core.domain.PalletMovement;
import com.wmsdipl.core.domain.PalletStatus;
import com.wmsdipl.core.domain.Scan;
import com.wmsdipl.core.domain.Task;
import com.wmsdipl.core.domain.TaskStatus;
import com.wmsdipl.core.domain.TaskType;
import com.wmsdipl.core.repository.DiscrepancyRepository;
import com.wmsdipl.core.repository.LocationRepository;
import com.wmsdipl.core.repository.PalletMovementRepository;
import com.wmsdipl.core.repository.PalletRepository;
import com.wmsdipl.core.repository.ScanRepository;
import com.wmsdipl.core.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

import static org.springframework.http.HttpStatus.CONFLICT;

@Service
public class TaskScanUndoService {

    private final TaskLifecycleService taskLifecycleService;
    private final TaskRepository taskRepository;
    private final ScanRepository scanRepository;
    private final PalletRepository palletRepository;
    private final PalletMovementRepository palletMovementRepository;
    private final DiscrepancyRepository discrepancyRepository;
    private final LocationRepository locationRepository;
    private final AuditLogService auditLogService;

    public TaskScanUndoService(
        TaskLifecycleService taskLifecycleService,
        TaskRepository taskRepository,
        ScanRepository scanRepository,
        PalletRepository palletRepository,
        PalletMovementRepository palletMovementRepository,
        DiscrepancyRepository discrepancyRepository,
        LocationRepository locationRepository,
        AuditLogService auditLogService
    ) {
        this.taskLifecycleService = taskLifecycleService;
        this.taskRepository = taskRepository;
        this.scanRepository = scanRepository;
        this.palletRepository = palletRepository;
        this.palletMovementRepository = palletMovementRepository;
        this.discrepancyRepository = discrepancyRepository;
        this.locationRepository = locationRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public UndoLastScanResultDto undoLastScan(Long taskId, String actor) {
        Task task = taskLifecycleService.getTask(taskId);
        ensureUndoAllowed(task);

        Scan scan = scanRepository.findFirstByTaskOrderByScannedAtDescIdDesc(task)
            .orElseThrow(() -> new ResponseStatusException(CONFLICT, "No scans found for task"));

        PalletMovement movement = palletMovementRepository.findByScanId(scan.getId()).orElse(null);
        Pallet pallet = resolvePallet(task, scan);

        BigDecimal qtyDoneBefore = zero(task.getQtyDone());
        BigDecimal scanQty = zero(scan.getQty());
        BigDecimal qtyDoneAfter = qtyDoneBefore.subtract(scanQty);
        if (qtyDoneAfter.compareTo(BigDecimal.ZERO) < 0) {
            qtyDoneAfter = BigDecimal.ZERO;
        }
        task.setQtyDone(qtyDoneAfter);

        String palletStatusBefore = pallet != null && pallet.getStatus() != null ? pallet.getStatus().name() : null;
        String palletLocationBefore = locationCode(pallet != null ? pallet.getLocation() : null);
        BigDecimal palletQtyBefore = pallet != null ? pallet.getQuantity() : null;

        rollbackPalletByTaskType(task, scanQty, movement, pallet);

        int discrepanciesRolledBack = Math.toIntExact(discrepancyRepository.deleteByScanId(scan.getId()));
        boolean movementRolledBack = false;
        if (movement != null) {
            palletMovementRepository.delete(movement);
            movementRolledBack = true;
        }

        scanRepository.delete(scan);
        taskRepository.save(task);

        String resolvedActor = (actor == null || actor.isBlank()) ? "system" : actor;
        auditLogService.logUpdate("TASK", task.getId(), resolvedActor, "qtyDone",
            formatDecimal(qtyDoneBefore), formatDecimal(qtyDoneAfter));
        if (pallet != null) {
            String palletStatusAfter = pallet.getStatus() != null ? pallet.getStatus().name() : null;
            String palletLocationAfter = locationCode(pallet.getLocation());
            BigDecimal palletQtyAfter = pallet.getQuantity();
            if (!java.util.Objects.equals(palletStatusBefore, palletStatusAfter)) {
                auditLogService.logUpdate("PALLET", pallet.getId(), resolvedActor, "status",
                    palletStatusBefore, palletStatusAfter);
            }
            if (!java.util.Objects.equals(formatDecimal(palletQtyBefore), formatDecimal(palletQtyAfter))) {
                auditLogService.logUpdate("PALLET", pallet.getId(), resolvedActor, "quantity",
                    formatDecimal(palletQtyBefore), formatDecimal(palletQtyAfter));
            }
            if (!java.util.Objects.equals(palletLocationBefore, palletLocationAfter)) {
                auditLogService.logLocationChange("PALLET", pallet.getId(), resolvedActor,
                    palletLocationBefore, palletLocationAfter);
            }
        }

        return new UndoLastScanResultDto(
            task.getId(),
            scan.getId(),
            task.getTaskType() != null ? task.getTaskType().name() : null,
            qtyDoneBefore,
            qtyDoneAfter,
            scan.getPalletCode(),
            movementRolledBack,
            discrepanciesRolledBack
        );
    }

    private void ensureUndoAllowed(Task task) {
        if (task.getStatus() == TaskStatus.COMPLETED) {
            throw new ResponseStatusException(CONFLICT, "Undo is not allowed for COMPLETED tasks");
        }
        if (task.getStatus() != TaskStatus.ASSIGNED && task.getStatus() != TaskStatus.IN_PROGRESS) {
            throw new ResponseStatusException(CONFLICT,
                "Undo is allowed only for ASSIGNED or IN_PROGRESS tasks. Current status: " + task.getStatus());
        }
    }

    private Pallet resolvePallet(Task task, Scan scan) {
        if (scan.getPalletCode() != null && !scan.getPalletCode().isBlank()) {
            Pallet pallet = palletRepository.findByCode(scan.getPalletCode()).orElse(null);
            if (pallet != null) {
                return pallet;
            }
        }
        if (task.getPalletId() != null) {
            return palletRepository.findById(task.getPalletId()).orElse(null);
        }
        return null;
    }

    private void rollbackPalletByTaskType(Task task, BigDecimal scanQty, PalletMovement movement, Pallet pallet) {
        TaskType taskType = task.getTaskType();
        if (taskType == TaskType.RECEIVING) {
            rollbackReceiving(pallet, scanQty);
            return;
        }
        if (taskType == TaskType.PLACEMENT) {
            rollbackPlacement(task, movement, pallet);
            return;
        }
        if (taskType == TaskType.SHIPPING) {
            rollbackShipping(task, movement, pallet, scanQty);
            return;
        }
        throw new ResponseStatusException(CONFLICT, "Undo is not supported for task type: " + taskType);
    }

    private void rollbackReceiving(Pallet pallet, BigDecimal scanQty) {
        if (pallet == null) {
            throw new ResponseStatusException(CONFLICT, "Pallet not found for receiving undo");
        }

        BigDecimal currentQty = zero(pallet.getQuantity());
        BigDecimal newQty = currentQty.subtract(scanQty);
        if (newQty.compareTo(BigDecimal.ZERO) < 0) {
            newQty = BigDecimal.ZERO;
        }

        pallet.setQuantity(newQty);
        if (newQty.compareTo(BigDecimal.ZERO) == 0) {
            pallet.setStatus(PalletStatus.EMPTY);
            pallet.setLocation(null);
            pallet.setSkuId(null);
            pallet.setReceipt(null);
            pallet.setReceiptLine(null);
            pallet.setLotNumber(null);
            pallet.setExpiryDate(null);
            pallet.setUom(null);
        } else {
            pallet.setStatus(PalletStatus.RECEIVING);
        }
        palletRepository.save(pallet);
    }

    private void rollbackPlacement(Task task, PalletMovement movement, Pallet pallet) {
        if (pallet == null) {
            throw new ResponseStatusException(CONFLICT, "Pallet not found for placement undo");
        }
        Location restoredLocation = resolveFromLocation(task, movement);
        if (restoredLocation == null) {
            throw new ResponseStatusException(CONFLICT, "Cannot resolve source location for placement undo");
        }
        pallet.setLocation(restoredLocation);
        pallet.setStatus(PalletStatus.RECEIVED);
        palletRepository.save(pallet);
    }

    private void rollbackShipping(Task task, PalletMovement movement, Pallet pallet, BigDecimal scanQty) {
        if (pallet == null) {
            throw new ResponseStatusException(CONFLICT, "Pallet not found for shipping undo");
        }
        Location restoredLocation = resolveFromLocation(task, movement);
        if (restoredLocation == null) {
            throw new ResponseStatusException(CONFLICT, "Cannot resolve source location for shipping undo");
        }

        pallet.setQuantity(zero(pallet.getQuantity()).add(scanQty));
        pallet.setLocation(restoredLocation);
        pallet.setStatus(PalletStatus.PLACED);
        palletRepository.save(pallet);
    }

    private Location resolveFromLocation(Task task, PalletMovement movement) {
        if (movement != null && movement.getFromLocation() != null) {
            return movement.getFromLocation();
        }
        if (task.getSourceLocationId() != null) {
            return locationRepository.findById(task.getSourceLocationId()).orElse(null);
        }
        return null;
    }

    private BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String formatDecimal(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.stripTrailingZeros().toPlainString();
    }

    private String locationCode(Location location) {
        return location == null ? null : location.getCode();
    }
}
