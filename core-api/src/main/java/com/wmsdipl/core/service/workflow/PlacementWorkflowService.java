package com.wmsdipl.core.service.workflow;

import com.wmsdipl.contracts.dto.RecordScanRequest;
import com.wmsdipl.core.domain.Location;
import com.wmsdipl.core.domain.Pallet;
import com.wmsdipl.core.domain.PalletStatus;
import com.wmsdipl.core.domain.Receipt;
import com.wmsdipl.core.domain.ReceiptStatus;
import com.wmsdipl.core.domain.Scan;
import com.wmsdipl.core.domain.Task;
import com.wmsdipl.core.domain.TaskStatus;
import com.wmsdipl.core.domain.TaskType;
import com.wmsdipl.core.repository.LocationRepository;
import com.wmsdipl.core.repository.PalletRepository;
import com.wmsdipl.core.repository.ReceiptRepository;
import com.wmsdipl.core.repository.ScanRepository;
import com.wmsdipl.core.repository.TaskRepository;
import com.wmsdipl.core.service.DuplicateScanDetectionService;
import com.wmsdipl.core.service.PutawayService;
import com.wmsdipl.core.service.ReceiptWorkflowBlockerService;
import com.wmsdipl.core.service.TaskLifecycleService;
import com.wmsdipl.core.service.StockMovementService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Service for managing placement workflow (putaway tasks).
 * Handles moving pallets from receiving/transit locations to storage locations.
 * 
 * Process:
 * 1. Validate task is PLACEMENT type and in correct status
 * 2. Validate pallet matches task assignment
 * 3. Move pallet from source location to target location
 * 4. Update pallet status to PLACED
 * 5. Record movement in pallet_movements table
 * 6. Create scan record for audit trail
 * 7. Auto-complete task
 */
@Service
public class PlacementWorkflowService {

    private final TaskRepository taskRepository;
    private final PalletRepository palletRepository;
    private final LocationRepository locationRepository;
    private final ScanRepository scanRepository;
    private final TaskLifecycleService taskLifecycleService;
    private final ReceiptRepository receiptRepository;
    private final PutawayService putawayService;
    private final StockMovementService stockMovementService;
    private final DuplicateScanDetectionService duplicateScanDetectionService;
    private final ReceiptWorkflowBlockerService receiptWorkflowBlockerService;

    public PlacementWorkflowService(
            TaskRepository taskRepository,
            PalletRepository palletRepository,
            LocationRepository locationRepository,
            ScanRepository scanRepository,
            TaskLifecycleService taskLifecycleService,
            ReceiptRepository receiptRepository,
            PutawayService putawayService,
            StockMovementService stockMovementService,
            DuplicateScanDetectionService duplicateScanDetectionService,
            ReceiptWorkflowBlockerService receiptWorkflowBlockerService
    ) {
        this.taskRepository = taskRepository;
        this.palletRepository = palletRepository;
        this.locationRepository = locationRepository;
        this.scanRepository = scanRepository;
        this.taskLifecycleService = taskLifecycleService;
        this.receiptRepository = receiptRepository;
        this.putawayService = putawayService;
        this.stockMovementService = stockMovementService;
        this.duplicateScanDetectionService = duplicateScanDetectionService;
        this.receiptWorkflowBlockerService = receiptWorkflowBlockerService;
    }

    /**
     * Records a placement scan and completes the placement workflow.
     * 
     * @param taskId The placement task ID
     * @param request The scan request containing pallet code and quantity
     * @return The created scan record
     */
    @Transactional
    public Scan recordPlacement(Long taskId, RecordScanRequest request) {
        // === 1. VALIDATE TASK ===
        Task task = taskLifecycleService.getTask(taskId);
        validatePlacementRequest(task, request);
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
                && Duration.between(recentScan.getScannedAt(), java.time.LocalDateTime.now()).getSeconds() <= 5) {
                return markAsReplay(recentScan, false, "Potential duplicate scan detected within 5 seconds");
            }
        }

        // === 2. VALIDATE PALLET ===
        Pallet pallet = palletRepository.findByCode(request.palletCode())
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, 
                "Pallet not found: " + request.palletCode()));

        // Ensure pallet matches task assignment
        if (task.getPalletId() == null || !task.getPalletId().equals(pallet.getId())) {
            throw new ResponseStatusException(BAD_REQUEST,
                "Pallet " + request.palletCode() + " does not match task assignment");
        }

        // === 3. GET SOURCE AND TARGET LOCATIONS ===
        Location sourceLocation = null;
        if (task.getSourceLocationId() != null) {
            sourceLocation = locationRepository.findById(task.getSourceLocationId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND,
                    "Source location not found: " + task.getSourceLocationId()));
        }

        if (task.getTargetLocationId() == null) {
            throw new ResponseStatusException(BAD_REQUEST,
                "Task has no target location assigned");
        }

        Location targetLocation = locationRepository.findById(task.getTargetLocationId())
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND,
                "Target location not found: " + task.getTargetLocationId()));

        // === 3.5 CHECK CAPACITY ===
        long currentPallets = palletRepository.countByLocation(targetLocation);
        
        // If the pallet is already in this location, it shouldn't block itself
        boolean alreadyThere = pallet.getLocation() != null && pallet.getLocation().getId().equals(targetLocation.getId());
        
        if (!alreadyThere && currentPallets >= targetLocation.getMaxPallets()) {
            throw new ResponseStatusException(BAD_REQUEST,
                "Ячейка " + targetLocation.getCode() + " переполнена (макс. паллет: " + targetLocation.getMaxPallets() + ")");
        }

        // === 4. MOVE PALLET TO TARGET LOCATION ===
        pallet.setLocation(targetLocation);
        pallet.setStatus(PalletStatus.PLACED);
        Pallet savedPallet = palletRepository.save(pallet);

        // === 5. CREATE SCAN RECORD ===
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

        // === 6. RECORD MOVEMENT ===
        stockMovementService.recordPlacement(
            savedPallet,
            sourceLocation,
            targetLocation,
            task.getAssignee() != null ? task.getAssignee() : "system",
            taskId,
            savedScan.getId()
        );

        // === 7. UPDATE TASK QUANTITY (FOR TRACKING) ===
        BigDecimal currentDone = task.getQtyDone() != null ? task.getQtyDone() : BigDecimal.ZERO;
        task.setQtyDone(currentDone.add(qtyDecimal));

        // Auto-start if needed
        taskLifecycleService.autoStartIfNeeded(task);
        taskRepository.save(task);

        return savedScan;
    }

    /**
     * Validates that the placement request is valid for the given task.
     */
    private void validatePlacementRequest(Task task, RecordScanRequest request) {
        if (task.getTaskType() != TaskType.PLACEMENT) {
            throw new ResponseStatusException(BAD_REQUEST,
                "Only placement tasks can use this endpoint");
        }

        if (task.getStatus() != TaskStatus.IN_PROGRESS && task.getStatus() != TaskStatus.ASSIGNED) {
            throw new ResponseStatusException(BAD_REQUEST,
                "Task must be in ASSIGNED or IN_PROGRESS status to record placement");
        }

        // Validate palletCode is required
        if (request.palletCode() == null || request.palletCode().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST,
                "Pallet code is required");
        }
        
        // Validate locationCode for PLACEMENT tasks
        if (request.locationCode() == null || request.locationCode().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST,
                "Location code is required for placement tasks");
        }
        
        // Validate that scanned location matches target location
        if (task.getTargetLocationId() != null) {
            Location targetLocation = locationRepository.findById(task.getTargetLocationId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND,
                    "Target location not found: " + task.getTargetLocationId()));
            
            if (!targetLocation.getCode().equals(request.locationCode())) {
                throw new ResponseStatusException(BAD_REQUEST,
                    "Неверная ячейка. Ожидается: " + targetLocation.getCode() + 
                    ", отсканировано: " + request.locationCode());
            }
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

    /**
     * Starts the placement process: ACCEPTED/READY_FOR_PLACEMENT → PLACING
     * Automatically generates placement tasks for all received pallets.
     * Only transitions to PLACING if tasks were successfully created.
     * 
     * @return Number of placement tasks created
     */
    @Transactional
    public int startPlacement(Long receiptId) {
        Receipt receipt = receiptRepository.findById(receiptId)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Receipt not found"));

        boolean crossDockReady = Boolean.TRUE.equals(receipt.getCrossDock())
            && receipt.getStatus() == ReceiptStatus.READY_FOR_PLACEMENT;
        boolean regularReady = receipt.getStatus() == ReceiptStatus.ACCEPTED;
        if (!regularReady && !crossDockReady) {
            throw new ResponseStatusException(BAD_REQUEST,
                "Only ACCEPTED receipts or cross-dock READY_FOR_PLACEMENT receipts can start placement");
        }

        receiptWorkflowBlockerService.assertNoSkuStatusBlockers(receipt, "startPlacement");

        // Generate placement tasks using PutawayService
        List<Task> createdTasks = putawayService.generatePlacementTasks(receiptId);

        if (createdTasks.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST,
                "No placement tasks could be generated. Check that pallets are in RECEIVED status.");
        }

        // Auto-transition to PLACING status since tasks were created
        receipt.setStatus(ReceiptStatus.PLACING);
        receiptRepository.save(receipt);
        
        return createdTasks.size();
    }

    /**
     * Completes the placement workflow: PLACING → STOCKED
     * Checks if all placement tasks are completed.
     */
    @Transactional
    public void completePlacement(Long receiptId) {
        Receipt receipt = receiptRepository.findById(receiptId)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Receipt not found"));

        if (receipt.getStatus() != ReceiptStatus.PLACING) {
            throw new ResponseStatusException(BAD_REQUEST,
                "Receipt is not in placement state");
        }

        // Check if all PLACEMENT tasks are completed
        java.util.List<Task> placementTasks = taskRepository.findByReceiptIdAndTaskType(
            receiptId, TaskType.PLACEMENT);

        if (placementTasks.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST,
                "No placement tasks exist for receipt");
        }

        boolean allCompleted = placementTasks.stream()
            .allMatch(t -> t.getStatus() == TaskStatus.COMPLETED);

        if (!allCompleted) {
            throw new ResponseStatusException(BAD_REQUEST,
                "All placement tasks must be completed first");
        }

        if (Boolean.TRUE.equals(receipt.getCrossDock())) {
            receipt.setStatus(ReceiptStatus.READY_FOR_SHIPMENT);
        } else {
            receipt.setStatus(ReceiptStatus.STOCKED);
        }
        receiptRepository.save(receipt);
    }

    /**
     * Automatically completes receipt to STOCKED if all placement tasks are completed.
     * Called after each placement task completion to check if workflow is done.
     * 
     * @param receiptId The receipt ID to check
     */
    @Transactional
    public void autoCompleteReceiptIfAllTasksCompleted(Long receiptId) {
        Receipt receipt = receiptRepository.findById(receiptId)
            .orElse(null);
        
        if (receipt == null || receipt.getStatus() != ReceiptStatus.PLACING) {
            return; // Not in placement state, nothing to do
        }

        // Check if all PLACEMENT tasks are completed
        List<Task> placementTasks = taskRepository.findByReceiptIdAndTaskType(
            receiptId, TaskType.PLACEMENT);

        if (placementTasks.isEmpty()) {
            return; // No tasks, cannot auto-complete
        }

        boolean allCompleted = placementTasks.stream()
            .allMatch(t -> t.getStatus() == TaskStatus.COMPLETED);

        if (allCompleted) {
            // All tasks completed - transition depends on receipt flow.
            if (Boolean.TRUE.equals(receipt.getCrossDock())) {
                receipt.setStatus(ReceiptStatus.READY_FOR_SHIPMENT);
            } else {
                receipt.setStatus(ReceiptStatus.STOCKED);
            }
            receiptRepository.save(receipt);
        }
    }
}
