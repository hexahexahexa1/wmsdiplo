package com.wmsdipl.core.service.workflow;

import com.wmsdipl.contracts.dto.RecordScanRequest;
import com.wmsdipl.core.domain.Location;
import com.wmsdipl.core.domain.LocationType;
import com.wmsdipl.core.domain.Pallet;
import com.wmsdipl.core.domain.PalletStatus;
import com.wmsdipl.core.domain.Receipt;
import com.wmsdipl.core.domain.ReceiptStatus;
import com.wmsdipl.core.domain.Scan;
import com.wmsdipl.core.domain.Task;
import com.wmsdipl.core.domain.TaskStatus;
import com.wmsdipl.core.domain.TaskType;
import com.wmsdipl.core.repository.PalletRepository;
import com.wmsdipl.core.repository.ReceiptRepository;
import com.wmsdipl.core.repository.ScanRepository;
import com.wmsdipl.core.repository.TaskRepository;
import com.wmsdipl.core.service.DuplicateScanDetectionService;
import com.wmsdipl.core.service.ReceiptWorkflowBlockerService;
import com.wmsdipl.core.service.StockMovementService;
import com.wmsdipl.core.service.TaskLifecycleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ShippingWorkflowService {

    private final ReceiptRepository receiptRepository;
    private final TaskRepository taskRepository;
    private final TaskLifecycleService taskLifecycleService;
    private final PalletRepository palletRepository;
    private final ScanRepository scanRepository;
    private final StockMovementService stockMovementService;
    private final DuplicateScanDetectionService duplicateScanDetectionService;
    private final ReceiptWorkflowBlockerService receiptWorkflowBlockerService;

    public ShippingWorkflowService(
            ReceiptRepository receiptRepository,
            TaskRepository taskRepository,
            TaskLifecycleService taskLifecycleService,
            PalletRepository palletRepository,
            ScanRepository scanRepository,
            StockMovementService stockMovementService,
            DuplicateScanDetectionService duplicateScanDetectionService,
            ReceiptWorkflowBlockerService receiptWorkflowBlockerService
    ) {
        this.receiptRepository = receiptRepository;
        this.taskRepository = taskRepository;
        this.taskLifecycleService = taskLifecycleService;
        this.palletRepository = palletRepository;
        this.scanRepository = scanRepository;
        this.stockMovementService = stockMovementService;
        this.duplicateScanDetectionService = duplicateScanDetectionService;
        this.receiptWorkflowBlockerService = receiptWorkflowBlockerService;
    }

    @Transactional
    public int startShipping(Long receiptId) {
        Receipt receipt = receiptRepository.findById(receiptId)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Receipt not found"));

        ensureCrossDockState(receipt);

        if (receipt.getStatus() != ReceiptStatus.READY_FOR_SHIPMENT) {
            throw new ResponseStatusException(BAD_REQUEST,
                "Only cross-dock receipts in READY_FOR_SHIPMENT can start shipping");
        }

        receiptWorkflowBlockerService.assertNoSkuStatusBlockers(receipt, "startShipping");

        List<Pallet> pallets = palletRepository.findByReceipt(receipt);
        if (pallets.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "No pallets found for receipt");
        }

        boolean hasUnplacedPallets = pallets.stream().anyMatch(pallet ->
            pallet.getLocation() == null
                || pallet.getLocation().getLocationType() != LocationType.CROSS_DOCK
                || pallet.getStatus() != PalletStatus.PLACED
        );
        if (hasUnplacedPallets) {
            throw new ResponseStatusException(BAD_REQUEST,
                "Complete cross-dock placement first: all pallets must be PLACED in CROSS_DOCK locations");
        }

        List<Task> existingShippingTasks = taskRepository.findByReceiptIdAndTaskType(receiptId, TaskType.SHIPPING);
        boolean hasOpenShippingTasks = existingShippingTasks.stream()
            .anyMatch(task -> task.getStatus() != TaskStatus.COMPLETED && task.getStatus() != TaskStatus.CANCELLED);
        if (hasOpenShippingTasks) {
            receipt.setStatus(ReceiptStatus.SHIPPING_IN_PROGRESS);
            receiptRepository.save(receipt);
            return 0;
        }

        int createdCount = 0;
        for (Pallet pallet : pallets) {
            Task task = new Task();
            task.setReceipt(receipt);
            task.setTaskType(TaskType.SHIPPING);
            task.setStatus(TaskStatus.NEW);
            task.setPalletId(pallet.getId());
            task.setSourceLocationId(pallet.getLocation() != null ? pallet.getLocation().getId() : null);
            task.setTargetLocationId(null);
            task.setQtyAssigned(pallet.getQuantity());
            taskRepository.save(task);
            pallet.setStatus(PalletStatus.PICKING);
            palletRepository.save(pallet);
            createdCount++;
        }

        receipt.setStatus(ReceiptStatus.SHIPPING_IN_PROGRESS);
        receiptRepository.save(receipt);
        return createdCount;
    }

    @Transactional
    public void completeShipping(Long receiptId) {
        Receipt receipt = receiptRepository.findById(receiptId)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Receipt not found"));
        ensureCrossDockState(receipt);

        if (receipt.getStatus() != ReceiptStatus.SHIPPING_IN_PROGRESS) {
            throw new ResponseStatusException(BAD_REQUEST, "Receipt is not in shipping state");
        }

        List<Task> shippingTasks = taskRepository.findByReceiptIdAndTaskType(receiptId, TaskType.SHIPPING);
        if (shippingTasks.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "No shipping tasks exist for receipt");
        }

        boolean allCompleted = shippingTasks.stream().allMatch(task -> task.getStatus() == TaskStatus.COMPLETED);
        if (!allCompleted) {
            throw new ResponseStatusException(BAD_REQUEST, "All shipping tasks must be completed first");
        }

        receipt.setStatus(ReceiptStatus.SHIPPED);
        receiptRepository.save(receipt);
    }

    @Transactional
    public void autoCompleteShippingIfAllTasksCompleted(Long receiptId) {
        Receipt receipt = receiptRepository.findById(receiptId).orElse(null);
        if (receipt == null || receipt.getStatus() != ReceiptStatus.SHIPPING_IN_PROGRESS) {
            return;
        }

        List<Task> shippingTasks = taskRepository.findByReceiptIdAndTaskType(receiptId, TaskType.SHIPPING);
        if (shippingTasks.isEmpty()) {
            return;
        }

        boolean allCompleted = shippingTasks.stream().allMatch(task -> task.getStatus() == TaskStatus.COMPLETED);
        if (allCompleted) {
            receipt.setStatus(ReceiptStatus.SHIPPED);
            receiptRepository.save(receipt);
        }
    }

    @Transactional
    public Scan recordShipping(Long taskId, RecordScanRequest request) {
        Task task = taskLifecycleService.getTask(taskId);
        validateShippingRequest(task, request);

        String requestId = normalizeRequestId(request.requestId());
        BigDecimal qtyDecimal = request.qty() != null ? new BigDecimal(request.qty()) : BigDecimal.ZERO;

        if (requestId != null) {
            Scan existingScan = scanRepository.findByTaskIdAndRequestId(taskId, requestId).orElse(null);
            if (existingScan != null) {
                return markAsReplay(existingScan, true, "Idempotent replay: request already processed");
            }
        }

        DuplicateScanDetectionService.ScanResult duplicateResult =
            duplicateScanDetectionService.checkScan(request.palletCode());
        if (duplicateResult.isDuplicate()) {
            Scan recentScan = scanRepository.findFirstByTaskIdAndPalletCodeOrderByScannedAtDesc(
                taskId,
                request.palletCode()
            ).orElse(null);
            if (recentScan != null
                && recentScan.getQty() != null
                && recentScan.getQty().compareTo(qtyDecimal) == 0
                && recentScan.getScannedAt() != null
                && Duration.between(recentScan.getScannedAt(), LocalDateTime.now()).getSeconds() <= 5) {
                return markAsReplay(recentScan, false, "Potential duplicate scan detected within 5 seconds");
            }
        }

        Pallet pallet = palletRepository.findByCode(request.palletCode())
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Pallet not found: " + request.palletCode()));

        if (task.getPalletId() == null || !task.getPalletId().equals(pallet.getId())) {
            throw new ResponseStatusException(BAD_REQUEST,
                "Pallet " + request.palletCode() + " does not match task assignment");
        }

        BigDecimal currentQty = pallet.getQuantity() != null ? pallet.getQuantity() : BigDecimal.ZERO;
        if (qtyDecimal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(BAD_REQUEST, "Quantity must be greater than zero");
        }
        if (qtyDecimal.compareTo(currentQty) > 0) {
            throw new ResponseStatusException(BAD_REQUEST,
                "Shipped quantity exceeds pallet quantity. Available: " + currentQty + ", scanned: " + qtyDecimal);
        }

        Scan scan = new Scan();
        scan.setTask(task);
        scan.setRequestId(requestId);
        scan.setPalletCode(request.palletCode());
        scan.setSscc(request.sscc());
        scan.setBarcode(request.barcode());
        scan.setQty(qtyDecimal);
        scan.setDeviceId(request.deviceId());
        scan.setDiscrepancy(false);
        Scan savedScan = scanRepository.save(scan);

        Location fromLocation = pallet.getLocation();
        stockMovementService.recordPick(
            pallet,
            fromLocation,
            qtyDecimal,
            task.getAssignee() != null ? task.getAssignee() : "system",
            taskId,
            savedScan.getId()
        );

        BigDecimal remainingQty = currentQty.subtract(qtyDecimal);
        pallet.setQuantity(remainingQty);
        if (remainingQty.compareTo(BigDecimal.ZERO) <= 0) {
            pallet.setStatus(PalletStatus.SHIPPED);
            pallet.setLocation(null);
        } else {
            pallet.setStatus(PalletStatus.PICKING);
        }
        palletRepository.save(pallet);

        BigDecimal currentDone = task.getQtyDone() != null ? task.getQtyDone() : BigDecimal.ZERO;
        task.setQtyDone(currentDone.add(qtyDecimal));
        taskLifecycleService.autoStartIfNeeded(task);
        taskRepository.save(task);

        return savedScan;
    }

    private void ensureCrossDockState(Receipt receipt) {
        if (!Boolean.TRUE.equals(receipt.getCrossDock())) {
            throw new ResponseStatusException(BAD_REQUEST, "Shipping workflow is available only for cross-dock receipts");
        }
    }

    private void validateShippingRequest(Task task, RecordScanRequest request) {
        if (task.getTaskType() != TaskType.SHIPPING) {
            throw new ResponseStatusException(BAD_REQUEST, "Only shipping tasks can use this endpoint");
        }
        if (task.getStatus() != TaskStatus.IN_PROGRESS && task.getStatus() != TaskStatus.ASSIGNED) {
            throw new ResponseStatusException(BAD_REQUEST,
                "Task must be in ASSIGNED or IN_PROGRESS status to record shipping");
        }
        if (request.palletCode() == null || request.palletCode().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Pallet code is required");
        }
    }

    private String normalizeRequestId(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            return null;
        }
        String normalized = requestId.trim();
        if (normalized.length() > 128) {
            throw new ResponseStatusException(BAD_REQUEST, "requestId length must not exceed 128 characters");
        }
        return normalized;
    }

    private Scan markAsReplay(Scan existingScan, boolean idempotentReplay, String warning) {
        existingScan.setDuplicate(true);
        existingScan.setIdempotentReplay(idempotentReplay);
        List<String> warnings = new ArrayList<>();
        warnings.add(warning);
        existingScan.setWarnings(warnings);
        return existingScan;
    }
}
