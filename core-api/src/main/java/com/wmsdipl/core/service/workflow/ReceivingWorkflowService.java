package com.wmsdipl.core.service.workflow;

import com.wmsdipl.contracts.dto.DiscrepancyType;
import com.wmsdipl.contracts.dto.RecordScanRequest;
import com.wmsdipl.core.domain.Discrepancy;
import com.wmsdipl.core.domain.Location;
import com.wmsdipl.core.domain.LocationStatus;
import com.wmsdipl.core.domain.LocationType;
import com.wmsdipl.core.domain.Pallet;
import com.wmsdipl.core.domain.PalletStatus;
import com.wmsdipl.core.domain.Receipt;
import com.wmsdipl.core.domain.ReceiptLine;
import com.wmsdipl.core.domain.ReceiptStatus;
import com.wmsdipl.core.domain.Scan;
import com.wmsdipl.core.domain.Sku;
import com.wmsdipl.core.domain.Task;
import com.wmsdipl.core.domain.TaskStatus;
import com.wmsdipl.core.domain.TaskType;
import com.wmsdipl.core.repository.DiscrepancyRepository;
import com.wmsdipl.core.repository.LocationRepository;
import com.wmsdipl.core.repository.PalletRepository;
import com.wmsdipl.core.repository.ReceiptRepository;
import com.wmsdipl.core.repository.ScanRepository;
import com.wmsdipl.core.repository.SkuRepository;
import com.wmsdipl.core.repository.TaskRepository;
import com.wmsdipl.core.service.DuplicateScanDetectionService;
import com.wmsdipl.core.service.TaskLifecycleService;
import com.wmsdipl.core.service.StockMovementService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.ArrayList;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Service for managing receiving workflow: DRAFT → CONFIRMED → IN_PROGRESS → ACCEPTED
 * Handles scan recording, discrepancy detection, and workflow state transitions.
 * 
 * Optimized to use TaskLifecycleService for task state management.
 */
@Service
public class ReceivingWorkflowService {

    private final ReceiptRepository receiptRepository;
    private final TaskRepository taskRepository;
    private final ScanRepository scanRepository;
    private final DiscrepancyRepository discrepancyRepository;
    private final TaskLifecycleService taskLifecycleService;
    private final PalletRepository palletRepository;
    private final LocationRepository locationRepository;
    private final SkuRepository skuRepository;
    private final StockMovementService stockMovementService;
    private final DuplicateScanDetectionService duplicateScanDetectionService;

    public ReceivingWorkflowService(
            ReceiptRepository receiptRepository,
            TaskRepository taskRepository,
            ScanRepository scanRepository,
            DiscrepancyRepository discrepancyRepository,
            TaskLifecycleService taskLifecycleService,
            PalletRepository palletRepository,
            LocationRepository locationRepository,
            SkuRepository skuRepository,
            StockMovementService stockMovementService,
            DuplicateScanDetectionService duplicateScanDetectionService
    ) {
        this.receiptRepository = receiptRepository;
        this.taskRepository = taskRepository;
        this.scanRepository = scanRepository;
        this.discrepancyRepository = discrepancyRepository;
        this.taskLifecycleService = taskLifecycleService;
        this.palletRepository = palletRepository;
        this.locationRepository = locationRepository;
        this.skuRepository = skuRepository;
        this.stockMovementService = stockMovementService;
        this.duplicateScanDetectionService = duplicateScanDetectionService;
    }

    /**
     * Starts the receiving process: CONFIRMED → IN_PROGRESS
     * Creates RECEIVING tasks for each receipt line.
     * 
     * Multi-Pallet Auto-Split:
     * - If SKU has palletCapacity defined and qtyExpected > palletCapacity,
     *   creates multiple tasks to distribute work across pallets
     * - Otherwise, creates 1 task per line (legacy behavior)
     */
    @Transactional
    public int startReceiving(Long receiptId) {
        Receipt receipt = receiptRepository.findById(receiptId)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Receipt not found"));
        
        if (receipt.getStatus() != ReceiptStatus.CONFIRMED) {
            throw new ResponseStatusException(BAD_REQUEST, "Only confirmed receipts can start receiving");
        }
        
        List<Task> existing = taskRepository.findByReceiptIdAndTaskType(receiptId, TaskType.RECEIVING);
        if (!existing.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "Receiving tasks already exist for this receipt");
        }
        
        if (receipt.getLines() == null || receipt.getLines().isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "Receipt has no lines to receive");
        }
        
        // Create tasks for each line (with multi-pallet auto-split if needed)
        int[] count = {0};
        receipt.getLines().forEach(line -> {
            count[0] += createTasksForLine(receipt, line);
        });
        
        receipt.setStatus(ReceiptStatus.IN_PROGRESS);
        return count[0];
    }
    
    /**
     * Creates one or more tasks for a receipt line based on SKU pallet capacity.
     * 
     * @param receipt the receipt
     * @param line the receipt line
     * @return number of tasks created
     */
    private int createTasksForLine(Receipt receipt, ReceiptLine line) {
        BigDecimal qtyExpected = line.getQtyExpected();
        int tasksCreated = 0;
        
        // Try to get SKU pallet capacity
        BigDecimal palletCapacity = null;
        if (line.getSkuId() != null) {
            Sku sku = skuRepository.findById(line.getSkuId()).orElse(null);
            if (sku != null) {
                palletCapacity = sku.getPalletCapacity();
            }
        }
        
        // If pallet capacity is defined and qty exceeds it, split into multiple tasks
        if (palletCapacity != null && palletCapacity.compareTo(BigDecimal.ZERO) > 0 
            && qtyExpected.compareTo(palletCapacity) > 0) {
            
            // Calculate number of tasks needed
            int taskCount = qtyExpected.divide(palletCapacity, 0, java.math.RoundingMode.UP).intValue();
            BigDecimal remaining = qtyExpected;
            
            for (int i = 1; i <= taskCount; i++) {
                BigDecimal qtyForThisTask = remaining.min(palletCapacity);
                
                Task task = new Task();
                task.setReceipt(receipt);
                task.setLine(line);
                task.setTaskType(TaskType.RECEIVING);
                task.setStatus(TaskStatus.NEW);
                task.setQtyAssigned(qtyForThisTask);
                taskRepository.save(task);
                tasksCreated++;
                
                remaining = remaining.subtract(qtyForThisTask);
            }
        } else {
            // Create single task (legacy behavior)
            Task task = new Task();
            task.setReceipt(receipt);
            task.setLine(line);
            task.setTaskType(TaskType.RECEIVING);
            task.setStatus(TaskStatus.NEW);
            task.setQtyAssigned(qtyExpected);
            taskRepository.save(task);
            tasksCreated++;
        }
        return tasksCreated;
    }

    /**
     * Records a barcode scan for a receiving task with pallet-based tracking.
     * 
     * Process:
     * 1. Validate task and request
     * 2. Fetch and validate pallet by code
     * 3. Get expected SKU from task line
     * 4. Validate no mixed SKUs on pallet
     * 5. Validate scanned barcode matches expected SKU.code
     * 6. Initialize pallet on first use (set SKU, status, location)
     * 7. Accumulate quantity on pallet
     * 8. Update task quantity
     * 9. Detect discrepancies (BARCODE_MISMATCH, OVER_QTY, SSCC_MISMATCH)
     * 10. Create and save scan record
     * 11. Create discrepancy record if needed
     * 
     * Auto-starts task if in NEW or ASSIGNED status.
     */
    @Transactional
    public Scan recordScan(Long taskId, RecordScanRequest request) {
        // === 1. VALIDATE TASK AND REQUEST ===
        Task task = taskLifecycleService.getTask(taskId);
        validateScanRequest(task, request);
        String requestId = normalizeRequestId(request.requestId());
        BigDecimal qtyDecimal = new BigDecimal(request.qty());

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
        
        // === 2. FETCH AND VALIDATE PALLET ===
        Pallet pallet = palletRepository.findByCode(request.palletCode())
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, 
                "Pallet not found: " + request.palletCode()));
        
        // === 3. GET EXPECTED SKU ===
        ReceiptLine line = task.getLine();
        if (line == null || line.getSkuId() == null) {
            throw new ResponseStatusException(BAD_REQUEST, 
                "Task has no SKU assigned");
        }
        Long expectedSkuId = line.getSkuId();
        
        // === 4. CHECK NO MIXED SKUs ON PALLET ===
        if (pallet.getSkuId() != null && !pallet.getSkuId().equals(expectedSkuId)) {
            throw new ResponseStatusException(BAD_REQUEST,
                "Pallet " + pallet.getCode() + " already contains different SKU (ID: " + 
                pallet.getSkuId() + "). Mixed-SKU pallets are not allowed.");
        }
        
        // === 5. VALIDATE BARCODE MATCHES EXPECTED SKU.CODE ===
        boolean barcodeMatches = true;
        if (request.barcode() != null && !request.barcode().isBlank()) {
            Sku expectedSku = skuRepository.findById(expectedSkuId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, 
                    "Expected SKU not found: " + expectedSkuId));
            
            if (!request.barcode().equals(expectedSku.getCode())) {
                barcodeMatches = false;
            }
        }
        
        // === 6. INITIALIZE PALLET ON FIRST USE ===
        boolean isNewPallet = (pallet.getSkuId() == null);
        Location transitLocation = null;
        
        if (isNewPallet) {
            pallet.setSkuId(expectedSkuId);
            pallet.setStatus(PalletStatus.RECEIVING);
            pallet.setReceipt(task.getReceipt());
            pallet.setReceiptLine(line);
            
            // Set transit location
            transitLocation = findTransitLocation();
            pallet.setLocation(transitLocation);
            
            // Initialize quantity
            if (pallet.getQuantity() == null) {
                pallet.setQuantity(BigDecimal.ZERO);
            }
        }

        // UPDATE ATTRIBUTES ON EVERY SCAN (Not just first scan)
        // Update lot/expiry if provided
        if (request.lotNumber() != null) {
            pallet.setLotNumber(request.lotNumber());
        }
        if (request.expiryDate() != null) {
            pallet.setExpiryDate(request.expiryDate());
        }
        
        // Check for damage flag and update pallet status if damaged
        // If ANY scan is damaged, the whole pallet is considered DAMAGED
        if (Boolean.TRUE.equals(request.damageFlag())) {
            pallet.setStatus(PalletStatus.DAMAGED);
        }
        
        // === 7. ACCUMULATE QUANTITY ON PALLET ===
        BigDecimal currentPalletQty = pallet.getQuantity() != null 
            ? pallet.getQuantity() : BigDecimal.ZERO;
        pallet.setQuantity(currentPalletQty.add(qtyDecimal));
        Pallet savedPallet = palletRepository.save(pallet);
        
        // === 7.1. RECORD STOCK MOVEMENT (only for first scan on this pallet) ===
        if (isNewPallet && transitLocation != null) {
            stockMovementService.recordReceive(
                savedPallet, 
                transitLocation, 
                task.getAssignee() != null ? task.getAssignee() : "system", 
                task.getId()
            );
        }
        
        // === 8. UPDATE TASK QUANTITY ===
        Receipt receipt = task.getReceipt();
        BigDecimal currentDone = zeroIfNull(task.getQtyDone());
        BigDecimal newTotal = currentDone.add(qtyDecimal);
        task.setQtyDone(newTotal);

        BigDecimal expectedQty = line.getQtyExpected();

        // === 9. DETECT DISCREPANCIES ===
        boolean hasDiscrepancy = false;
        DiscrepancyType discrepancyType = null;
        
        // BARCODE_MISMATCH
        if (!barcodeMatches) {
            hasDiscrepancy = true;
            discrepancyType = DiscrepancyType.BARCODE_MISMATCH;
        }
        
        // OVER_QTY
        if (expectedQty != null && newTotal.compareTo(expectedQty) > 0) {
            hasDiscrepancy = true;
            discrepancyType = discrepancyType != null ? discrepancyType : DiscrepancyType.OVER_QTY;
        }
        
        // UNDER_QTY (detected only when task is being completed with less than expected)
        // Note: We don't flag UNDER_QTY during individual scans, only when completing the task
        // This allows receiving to continue and accumulate quantity
        
        // SSCC_MISMATCH
        if (line.getSsccExpected() != null && request.sscc() != null
            && !line.getSsccExpected().equals(request.sscc())) {
            hasDiscrepancy = true;
            discrepancyType = discrepancyType != null ? discrepancyType : DiscrepancyType.SSCC_MISMATCH;
        }
        
        // NEW: DAMAGE - Damaged goods detected
        if (Boolean.TRUE.equals(request.damageFlag())) {
            hasDiscrepancy = true;
            discrepancyType = discrepancyType != null ? discrepancyType : DiscrepancyType.DAMAGE;
        }
        
        // NEW: EXPIRED_PRODUCT - Expiry date is in the past
        if (request.expiryDate() != null && request.expiryDate().isBefore(java.time.LocalDate.now())) {
            hasDiscrepancy = true;
            discrepancyType = discrepancyType != null ? discrepancyType : DiscrepancyType.EXPIRED_PRODUCT;
        }
        
        // NEW: LOT_MISMATCH - Lot number doesn't match expected
        if (line.getLotNumberExpected() != null && request.lotNumber() != null
            && !line.getLotNumberExpected().equals(request.lotNumber())) {
            hasDiscrepancy = true;
            discrepancyType = discrepancyType != null ? discrepancyType : DiscrepancyType.LOT_MISMATCH;
        }

        // === 10. CREATE AND SAVE SCAN ===
        Scan scan = new Scan();
        scan.setTask(task);
        scan.setRequestId(requestId);
        scan.setPalletCode(request.palletCode());
        scan.setSscc(request.sscc());
        scan.setBarcode(request.barcode());
        scan.setQty(qtyDecimal);
        scan.setDeviceId(request.deviceId());
        scan.setDiscrepancy(hasDiscrepancy);
        
        // NEW: Save damage information
        if (request.damageFlag() != null) {
            scan.setDamageFlag(request.damageFlag());
        }
        if (request.damageType() != null) {
            scan.setDamageType(request.damageType());
        }
        if (request.damageDescription() != null) {
            scan.setDamageDescription(request.damageDescription());
        }
        
        // NEW: Save lot tracking information
        if (request.lotNumber() != null) {
            scan.setLotNumber(request.lotNumber());
        }
        if (request.expiryDate() != null) {
            scan.setExpiryDate(request.expiryDate());
        }
        
        Scan savedScan = scanRepository.save(scan);

        // === 11. AUTO-START TASK ===
        taskLifecycleService.autoStartIfNeeded(task);
        taskRepository.save(task);

        // === 12. CREATE DISCREPANCY RECORD ===
        if (hasDiscrepancy) {
            createDiscrepancyRecord(receipt, line, discrepancyType, 
                expectedQty, newTotal, request.comment());
        }

        return savedScan;
    }

    /**
     * Completes the receiving workflow: IN_PROGRESS → ACCEPTED or READY_FOR_SHIPMENT
     * 
     * Operator confirms all discrepancies during task completion via dialog in desktop client.
     * Backend does not check for unresolved discrepancies - operator already made the decision.
     */
    @Transactional
    public Receipt completeReceiving(Long receiptId) {
        Receipt receipt = receiptRepository.findById(receiptId)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Receipt not found"));
        
        if (receipt.getStatus() != ReceiptStatus.IN_PROGRESS) {
            throw new ResponseStatusException(BAD_REQUEST, "Receipt is not in receiving state");
        }
        
        List<Task> tasks = taskRepository.findByReceiptIdAndTaskType(receiptId, TaskType.RECEIVING);
        if (tasks.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "No receiving tasks exist for receipt");
        }
        
        boolean allCompleted = tasks.stream().allMatch(t -> t.getStatus() == TaskStatus.COMPLETED);
        if (!allCompleted) {
            throw new ResponseStatusException(BAD_REQUEST, "All receiving tasks must be completed first");
        }
        
        // Operator confirmed all discrepancies during task completion
        // Proceed to next status based on cross-dock flag
        if (Boolean.TRUE.equals(receipt.getCrossDock())) {
            // Cross-dock: skip ACCEPTED and go straight to READY_FOR_SHIPMENT
            receipt.setStatus(ReceiptStatus.READY_FOR_SHIPMENT);
        } else {
            // Normal flow: ACCEPTED
            receipt.setStatus(ReceiptStatus.ACCEPTED);
        }
        
        // Transition pallets from RECEIVING to RECEIVED
        // This allows placement tasks to be created
        List<Pallet> pallets = palletRepository.findByReceipt(receipt);
        pallets.stream()
            .filter(p -> p.getStatus() == PalletStatus.RECEIVING)
            .forEach(p -> {
                p.setStatus(PalletStatus.RECEIVED);
                palletRepository.save(p);
            });
        
        return receipt;
    }

    /**
     * Automatically checks if all receiving tasks are completed, and if so,
     * transitions the receipt to ACCEPTED (or READY_FOR_SHIPMENT).
     * 
     * Called by TaskService when a receiving task is completed.
     */
    @Transactional
    public void checkAndCompleteReceipt(Long receiptId) {
        Receipt receipt = receiptRepository.findById(receiptId).orElse(null);
        if (receipt == null || receipt.getStatus() != ReceiptStatus.IN_PROGRESS) {
            return;
        }

        List<Task> tasks = taskRepository.findByReceiptIdAndTaskType(receiptId, TaskType.RECEIVING);
        if (tasks.isEmpty()) {
            return;
        }

        boolean allCompleted = tasks.stream().allMatch(t -> t.getStatus() == TaskStatus.COMPLETED);
        if (allCompleted) {
            // Reuse logic from completeReceiving
            completeReceiving(receiptId);
        }
    }

    /**
     * Cancels a receipt (cannot cancel STOCKED receipts)
     */
    @Transactional
    public Receipt cancel(Long receiptId) {
        Receipt receipt = receiptRepository.findById(receiptId)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Receipt not found"));
        
        if (receipt.getStatus() == ReceiptStatus.STOCKED) {
            throw new ResponseStatusException(BAD_REQUEST, "Stocked receipt cannot be cancelled");
        }
        
        receipt.setStatus(ReceiptStatus.CANCELLED);
        return receipt;
    }

    // Helper methods

    private void validateScanRequest(Task task, RecordScanRequest request) {
        if (task.getTaskType() != TaskType.RECEIVING) {
            throw new ResponseStatusException(BAD_REQUEST, 
                "Only receiving tasks accept scans");
        }
        
        Receipt receipt = task.getReceipt();
        if (receipt == null) {
            throw new ResponseStatusException(BAD_REQUEST, 
                "Task is not linked to a receipt");
        }
        
        if (receipt.getStatus() != ReceiptStatus.IN_PROGRESS) {
            throw new ResponseStatusException(BAD_REQUEST, 
                "Receipt is not in receiving state");
        }
        
        // Validate palletCode is required
        if (request.palletCode() == null || request.palletCode().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, 
                "Pallet code is required");
        }
        
        if (request.qty() == null || request.qty() < 1) {
            throw new ResponseStatusException(BAD_REQUEST, 
                "Quantity must be at least 1");
        }

        if (Boolean.TRUE.equals(request.damageFlag()) && request.damageType() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "damageType is required when damageFlag=true");
        }

        if (request.expiryDate() != null && request.expiryDate().isBefore(java.time.LocalDate.of(2000, 1, 1))) {
            throw new ResponseStatusException(BAD_REQUEST, "expiryDate is out of allowed range");
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
     * Finds first available location in RECEIVING zone.
     * Used to assign transit location to pallets during receiving.
     */
    private Location findTransitLocation() {
        return locationRepository
            .findFirstByLocationTypeAndStatusAndActiveTrueOrderByIdAsc(
                LocationType.RECEIVING, LocationStatus.AVAILABLE)
            .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST,
                "No available transit location found. " +
                "Create locations with type RECEIVING and status AVAILABLE first."));
    }

    private void createDiscrepancyRecord(Receipt receipt, ReceiptLine line, DiscrepancyType type,
                                        BigDecimal expectedQty, BigDecimal actualQty, String comment) {
        Discrepancy d = new Discrepancy();
        d.setReceipt(receipt);
        d.setLine(line);
        d.setType(type.name());
        d.setQtyExpected(expectedQty);
        d.setQtyActual(actualQty);
        d.setDescription(comment);
        discrepancyRepository.save(d);
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
