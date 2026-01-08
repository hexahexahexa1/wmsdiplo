package com.wmsdipl.core.service.workflow;

import com.wmsdipl.contracts.dto.RecordScanRequest;
import com.wmsdipl.core.domain.Discrepancy;
import com.wmsdipl.core.domain.Location;
import com.wmsdipl.core.domain.LocationStatus;
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
import com.wmsdipl.core.service.TaskLifecycleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

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

    public ReceivingWorkflowService(
            ReceiptRepository receiptRepository,
            TaskRepository taskRepository,
            ScanRepository scanRepository,
            DiscrepancyRepository discrepancyRepository,
            TaskLifecycleService taskLifecycleService,
            PalletRepository palletRepository,
            LocationRepository locationRepository,
            SkuRepository skuRepository
    ) {
        this.receiptRepository = receiptRepository;
        this.taskRepository = taskRepository;
        this.scanRepository = scanRepository;
        this.discrepancyRepository = discrepancyRepository;
        this.taskLifecycleService = taskLifecycleService;
        this.palletRepository = palletRepository;
        this.locationRepository = locationRepository;
        this.skuRepository = skuRepository;
    }

    /**
     * Starts the receiving process: CONFIRMED → IN_PROGRESS
     * Creates a RECEIVING task for each receipt line.
     */
    @Transactional
    public void startReceiving(Long receiptId) {
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
        
        receipt.getLines().forEach(line -> {
            Task task = new Task();
            task.setReceipt(receipt);
            task.setLine(line);
            task.setTaskType(TaskType.RECEIVING);
            task.setStatus(TaskStatus.NEW);
            task.setQtyAssigned(line.getQtyExpected());
            taskRepository.save(task);
        });
        
        receipt.setStatus(ReceiptStatus.IN_PROGRESS);
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
        if (pallet.getSkuId() == null) {
            pallet.setSkuId(expectedSkuId);
            pallet.setStatus(PalletStatus.RECEIVING);
            pallet.setReceipt(task.getReceipt());
            pallet.setReceiptLine(line);
            
            // Set transit location
            Location transitLocation = findTransitLocation();
            pallet.setLocation(transitLocation);
            
            // Initialize quantity
            if (pallet.getQuantity() == null) {
                pallet.setQuantity(BigDecimal.ZERO);
            }
        }
        
        // === 7. ACCUMULATE QUANTITY ON PALLET ===
        BigDecimal qtyDecimal = new BigDecimal(request.qty());
        BigDecimal currentPalletQty = pallet.getQuantity() != null 
            ? pallet.getQuantity() : BigDecimal.ZERO;
        pallet.setQuantity(currentPalletQty.add(qtyDecimal));
        palletRepository.save(pallet);
        
        // === 8. UPDATE TASK QUANTITY ===
        Receipt receipt = task.getReceipt();
        BigDecimal currentDone = zeroIfNull(task.getQtyDone());
        BigDecimal newTotal = currentDone.add(qtyDecimal);
        task.setQtyDone(newTotal);

        BigDecimal expectedQty = line.getQtyExpected();

        // === 9. DETECT DISCREPANCIES ===
        boolean hasDiscrepancy = false;
        String discrepancyType = null;
        
        // BARCODE_MISMATCH
        if (!barcodeMatches) {
            hasDiscrepancy = true;
            discrepancyType = "BARCODE_MISMATCH";
        }
        
        // OVER_QTY
        if (expectedQty != null && newTotal.compareTo(expectedQty) > 0) {
            hasDiscrepancy = true;
            discrepancyType = discrepancyType != null ? discrepancyType : "OVER_QTY";
        }
        
        // UNDER_QTY (detected only when task is being completed with less than expected)
        // Note: We don't flag UNDER_QTY during individual scans, only when completing the task
        // This allows receiving to continue and accumulate quantity
        
        // SSCC_MISMATCH
        if (line.getSsccExpected() != null && request.sscc() != null
            && !line.getSsccExpected().equals(request.sscc())) {
            hasDiscrepancy = true;
            discrepancyType = discrepancyType != null ? discrepancyType : "SSCC_MISMATCH";
        }

        // === 10. CREATE AND SAVE SCAN ===
        Scan scan = new Scan();
        scan.setTask(task);
        scan.setPalletCode(request.palletCode());
        scan.setSscc(request.sscc());
        scan.setBarcode(request.barcode());
        scan.setQty(qtyDecimal);
        scan.setDeviceId(request.deviceId());
        scan.setDiscrepancy(hasDiscrepancy);
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
     * Completes the receiving workflow: IN_PROGRESS → ACCEPTED or PENDING_RESOLUTION
     * 
     * Receipt moves to ACCEPTED if:
     * - All tasks are COMPLETED
     * - All discrepancies are resolved (resolved = true)
     * 
     * Receipt moves to PENDING_RESOLUTION if:
     * - There are unresolved discrepancies (resolved = false or null)
     * 
     * Note: Quantity gaps (qtyDone < qtyAssigned) are handled via UNDER_QTY discrepancies.
     * If operator confirmed completion with shortage, the discrepancy is auto-resolved,
     * so the receipt should proceed to ACCEPTED, not PENDING_RESOLUTION.
     */
    @Transactional
    public Receipt completeReceiving(Long receiptId) {
        Receipt receipt = receiptRepository.findById(receiptId)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Receipt not found"));
        
        if (receipt.getStatus() != ReceiptStatus.IN_PROGRESS && receipt.getStatus() != ReceiptStatus.PENDING_RESOLUTION) {
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
        
        // Check only for UNRESOLVED discrepancies
        // Resolved discrepancies (operator confirmed) should not block ACCEPTED status
        boolean hasOpenDiscrepancy = discrepancyRepository.findByReceipt(receipt).stream()
            .anyMatch(d -> d.getResolved() == null || !d.getResolved());

        if (hasOpenDiscrepancy) {
            receipt.setStatus(ReceiptStatus.PENDING_RESOLUTION);
        } else {
            receipt.setStatus(ReceiptStatus.ACCEPTED);
            
            // Transition pallets from RECEIVING to RECEIVED
            // This allows placement tasks to be created
            List<Pallet> pallets = palletRepository.findByReceipt(receipt);
            pallets.stream()
                .filter(p -> p.getStatus() == PalletStatus.RECEIVING)
                .forEach(p -> {
                    p.setStatus(PalletStatus.RECEIVED);
                    palletRepository.save(p);
                });
        }
        
        return receipt;
    }

    /**
     * Resolves discrepancies and continues workflow: PENDING_RESOLUTION → IN_PROGRESS
     */
    @Transactional
    public Receipt resolveAndContinue(Long receiptId) {
        Receipt receipt = receiptRepository.findById(receiptId)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Receipt not found"));
        
        if (receipt.getStatus() != ReceiptStatus.PENDING_RESOLUTION) {
            throw new ResponseStatusException(BAD_REQUEST, "Receipt is not pending resolution");
        }
        
        boolean hasOpenDiscrepancy = discrepancyRepository.findByReceipt(receipt).stream()
            .anyMatch(d -> d.getResolved() == null || !d.getResolved());
        if (hasOpenDiscrepancy) {
            throw new ResponseStatusException(BAD_REQUEST, "Resolve all discrepancies first");
        }
        
        receipt.setStatus(ReceiptStatus.IN_PROGRESS);
        return receipt;
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
        
        if (receipt.getStatus() != ReceiptStatus.IN_PROGRESS 
            && receipt.getStatus() != ReceiptStatus.PENDING_RESOLUTION) {
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
    }

    /**
     * Finds first available location in RECEIVING zone.
     * Used to assign transit location to pallets during receiving.
     */
    private Location findTransitLocation() {
        return locationRepository
            .findFirstByZone_ZoneTypeAndStatusAndActiveTrueOrderByIdAsc(
                "RECEIVING", LocationStatus.AVAILABLE)
            .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST,
                "No available transit location found. " +
                "Create a RECEIVING zone with AVAILABLE locations first."));
    }

    private void createDiscrepancyRecord(Receipt receipt, ReceiptLine line, String type,
                                        BigDecimal expectedQty, BigDecimal actualQty, String comment) {
        Discrepancy d = new Discrepancy();
        d.setReceipt(receipt);
        d.setLine(line);
        d.setType(type);
        d.setQtyExpected(expectedQty);
        d.setQtyActual(actualQty);
        d.setComment(comment);
        discrepancyRepository.save(d);
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
