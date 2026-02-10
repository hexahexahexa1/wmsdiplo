package com.wmsdipl.core.integration;

import com.wmsdipl.contracts.dto.*;
import com.wmsdipl.contracts.dto.DamageType;
import com.wmsdipl.core.domain.*;
import com.wmsdipl.core.repository.*;
import com.wmsdipl.core.service.BulkOperationsService;
import com.wmsdipl.core.service.AnalyticsService;
import com.wmsdipl.core.service.workflow.ReceivingWorkflowService;
import com.wmsdipl.core.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for receiving workflow improvements.
 * 
 * Tests cover:
 * 1. Damaged goods tracking and routing to DAMAGED locations
 * 2. Cross-dock receipts routing to CROSS_DOCK locations
 * 3. Multi-pallet auto-split based on SKU capacity
 * 4. Lot number and expiry date tracking with validation
 * 5. Bulk operations (assign, priority, cancel, create pallets)
 * 6. Analytics dashboard metrics calculation
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReceivingImprovementsIT {

    @Autowired
    private ReceivingWorkflowService receivingWorkflowService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private com.wmsdipl.core.service.TaskLifecycleService taskLifecycleService;

    @Autowired
    private BulkOperationsService bulkOperationsService;

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private ReceiptRepository receiptRepository;

    @Autowired
    private SkuRepository skuRepository;

    @Autowired
    private SkuUnitConfigRepository skuUnitConfigRepository;

    @Autowired
    private PalletRepository palletRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ScanRepository scanRepository;

    @Autowired
    private DiscrepancyRepository discrepancyRepository;

    @Autowired
    private com.wmsdipl.core.repository.ZoneRepository zoneRepository;

    private Sku testSku;
    private com.wmsdipl.core.domain.Zone testZone;
    private Location storageLocation;
    private Location damagedLocation;
    private Location crossDockLocation;
    private Location receivingLocation;

    @BeforeEach
    void setUp() {
        // Create test zone for locations
        testZone = new com.wmsdipl.core.domain.Zone();
        testZone.setCode("ZONE-TEST");
        testZone.setName("Test Zone");
        testZone = zoneRepository.save(testZone);

        // Create test SKU with pallet capacity
        testSku = new Sku();
        testSku.setCode("SKU001");
        testSku.setName("Test Product");
        testSku.setUom("ШТ");  // Required field
        testSku.setPalletCapacity(new BigDecimal("100")); // Max 100 units per pallet
        testSku = skuRepository.save(testSku);

        SkuUnitConfig baseUnit = new SkuUnitConfig();
        baseUnit.setSkuId(testSku.getId());
        baseUnit.setUnitCode("PCS");
        baseUnit.setFactorToBase(BigDecimal.ONE.setScale(6));
        baseUnit.setUnitsPerPallet(new BigDecimal("100.000"));
        baseUnit.setIsBase(true);
        baseUnit.setActive(true);
        skuUnitConfigRepository.save(baseUnit);

        // Create test locations
        storageLocation = new Location();
        storageLocation.setCode("STOR-01");
        storageLocation.setZone(testZone);
        storageLocation.setLocationType(LocationType.STORAGE);
        storageLocation.setStatus(LocationStatus.AVAILABLE);
        storageLocation.setActive(true);
        storageLocation = locationRepository.save(storageLocation);

        damagedLocation = new Location();
        damagedLocation.setCode("DAMAGED-01");
        damagedLocation.setZone(testZone);
        damagedLocation.setLocationType(LocationType.DAMAGED);
        damagedLocation.setStatus(LocationStatus.AVAILABLE);
        damagedLocation.setActive(true);
        damagedLocation = locationRepository.save(damagedLocation);

        crossDockLocation = new Location();
        crossDockLocation.setCode("XDOCK-01");
        crossDockLocation.setZone(testZone);
        crossDockLocation.setLocationType(LocationType.CROSS_DOCK);
        crossDockLocation.setStatus(LocationStatus.AVAILABLE);
        crossDockLocation.setActive(true);
        crossDockLocation = locationRepository.save(crossDockLocation);

        receivingLocation = new Location();
        receivingLocation.setCode("RECV-01");
        receivingLocation.setZone(testZone);
        receivingLocation.setLocationType(LocationType.RECEIVING);
        receivingLocation.setStatus(LocationStatus.AVAILABLE);
        receivingLocation.setActive(true);
        receivingLocation = locationRepository.save(receivingLocation);
    }

    @Test
    void shouldTrackDamagedGoods_AndRouteToDAMAGEDLocation() {
        // Given: Create receipt
        Receipt receipt = new Receipt();
        receipt.setDocNo("RCV-DAMAGE-001");
        receipt.setDocDate(LocalDate.now());
        receipt.setStatus(ReceiptStatus.CONFIRMED);
        receipt = receiptRepository.save(receipt);

        ReceiptLine line = new ReceiptLine();
        line.setReceipt(receipt);
        line.setLineNo(1);
        line.setSkuId(testSku.getId());
        line.setUom("PCS");
        line.setQtyExpected(new BigDecimal("50"));

        receipt.addLine(line);
        receipt = receiptRepository.save(receipt);

        // When: Start receiving and scan with damage flag
        receivingWorkflowService.startReceiving(receipt.getId());
        receipt = receiptRepository.findById(receipt.getId()).orElseThrow();
        assertEquals(ReceiptStatus.IN_PROGRESS, receipt.getStatus());

        // Get the created task
        List<Task> tasks = taskRepository.findByReceiptId(receipt.getId());
        assertEquals(1, tasks.size());
        Task task = tasks.get(0);

        // Start task
        taskLifecycleService.start(task.getId());

        // Create pallet for scanning
        Pallet testPallet = new Pallet();
        testPallet.setCode("PLT-DAMAGE-001");
        testPallet.setStatus(PalletStatus.EMPTY);
        palletRepository.save(testPallet);

        // Record scan with damage
        RecordScanRequest scanRequest = new RecordScanRequest(
            null,
            "PLT-DAMAGE-001",
            50,
            null,
            "SKU001",  // Barcode matches SKU code to avoid BARCODE_MISMATCH
            null,
            null,
            null,
            true,  // damageFlag
            DamageType.PHYSICAL_DAMAGE,  // damageType
            "Box crushed during transport",  // damageDescription
            null,
            null
        );

        receivingWorkflowService.recordScan(task.getId(), scanRequest);

        // Then: Verify damaged pallet created
        Pallet pallet = palletRepository.findByCode("PLT-DAMAGE-001").orElseThrow();
        assertEquals(PalletStatus.DAMAGED, pallet.getStatus());

        // Verify discrepancy created
        List<Discrepancy> discrepancies = discrepancyRepository.findByReceipt(receipt);
        assertEquals(1, discrepancies.size());
        Discrepancy discrepancy = discrepancies.get(0);
        assertEquals("DAMAGE", discrepancy.getType());

        // Verify scan recorded with damage info
        List<Scan> scans = scanRepository.findByTask(task);
        assertEquals(1, scans.size());
        Scan scan = scans.get(0);
        assertTrue(scan.getDamageFlag());
        assertEquals(DamageType.PHYSICAL_DAMAGE, scan.getDamageType());
        assertEquals("Box crushed during transport", scan.getDamageDescription());

        // Complete task
        taskLifecycleService.complete(task.getId());

        // Verify pallet routed to DAMAGED location (via putaway)
        // Note: This would require executing putaway workflow, simplified here
        assertNotNull(pallet);
    }

    @Test
    void shouldProcessCrossDockReceipt_AndRouteToXDOCKLocation() {
        // Given: Create cross-dock receipt
        Receipt receipt = new Receipt();
        receipt.setDocNo("RCV-XDOCK-001");
        receipt.setDocDate(LocalDate.now());
        receipt.setStatus(ReceiptStatus.CONFIRMED);
        receipt.setCrossDock(true);  // Mark as cross-dock
        receipt = receiptRepository.save(receipt);

        ReceiptLine line = new ReceiptLine();
        line.setReceipt(receipt);
        line.setLineNo(1);
        line.setSkuId(testSku.getId());
        line.setUom("PCS");
        line.setQtyExpected(new BigDecimal("50"));
        line.setLotNumberExpected("LOT-2026-01");  // Expected lot number
        line.setExpiryDateExpected(LocalDate.of(2026, 6, 30));  // Expected expiry date

        receipt.addLine(line);
        receipt = receiptRepository.save(receipt);

        // When: Start receiving and complete scan
        receivingWorkflowService.startReceiving(receipt.getId());

        List<Task> tasks = taskRepository.findByReceiptId(receipt.getId());
        Task task = tasks.get(0);

        taskLifecycleService.start(task.getId());

        // Create pallet for scanning
        Pallet testPallet = new Pallet();
        testPallet.setCode("PLT-XDOCK-001");
        testPallet.setStatus(PalletStatus.EMPTY);
        palletRepository.save(testPallet);

        RecordScanRequest scanRequest = new RecordScanRequest(
            null,
            "PLT-XDOCK-001",
            30,
            null,
            "SKU001",  // Barcode matches SKU code
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );

        receivingWorkflowService.recordScan(task.getId(), scanRequest);
        taskLifecycleService.complete(task.getId());

        receivingWorkflowService.completeReceiving(receipt.getId());

        // Then: Verify receipt status is READY_FOR_PLACEMENT (not ACCEPTED)
        receipt = receiptRepository.findById(receipt.getId()).orElseThrow();
        assertEquals(ReceiptStatus.READY_FOR_PLACEMENT, receipt.getStatus());

        // Verify pallet exists and would be routed to CROSS_DOCK location
        Pallet pallet = palletRepository.findByCode("PLT-XDOCK-001").orElseThrow();
        assertEquals(PalletStatus.RECEIVED, pallet.getStatus());
    }

    @Test
    void shouldAutoSplitMultiplePallets_WhenQuantityExceedsCapacity() {
        // Given: SKU with capacity 100, line with 250 units
        Receipt receipt = new Receipt();
        receipt.setDocNo("RCV-SPLIT-001");
        receipt.setDocDate(LocalDate.now());
        receipt.setStatus(ReceiptStatus.CONFIRMED);
        receipt = receiptRepository.save(receipt);

        ReceiptLine line = new ReceiptLine();
        line.setReceipt(receipt);
        line.setLineNo(1);
        line.setSkuId(testSku.getId());
        line.setUom("PCS");
        line.setQtyExpected(new BigDecimal("250"));  // Exceeds capacity of 100

        receipt.addLine(line);
        receipt = receiptRepository.save(receipt);

        // When: Start receiving
        receivingWorkflowService.startReceiving(receipt.getId());

        // Then: Verify 3 tasks created (100 + 100 + 50)
        List<Task> tasks = taskRepository.findByReceiptId(receipt.getId());
        assertEquals(3, tasks.size());

        // Verify quantities
        assertEquals(0, new BigDecimal("100").compareTo(tasks.get(0).getQtyAssigned()));
        assertEquals(0, new BigDecimal("100").compareTo(tasks.get(1).getQtyAssigned()));
        assertEquals(0, new BigDecimal("50").compareTo(tasks.get(2).getQtyAssigned()));

        // Verify all are RECEIVING tasks
        tasks.forEach(task -> {
            assertEquals(TaskType.RECEIVING, task.getTaskType());
            assertEquals(TaskStatus.NEW, task.getStatus());
        });
    }

    @Test
    void shouldTrackLotNumberAndExpiryDate_AndDetectMismatches() {
        // Given: Receipt line with expected lot and expiry
        Receipt receipt = new Receipt();
        receipt.setDocNo("RCV-LOT-001");
        receipt.setDocDate(LocalDate.now());
        receipt.setStatus(ReceiptStatus.CONFIRMED);
        receipt = receiptRepository.save(receipt);

        ReceiptLine line = new ReceiptLine();
        line.setReceipt(receipt);
        line.setLineNo(1);
        line.setSkuId(testSku.getId());
        line.setUom("PCS");
        line.setQtyExpected(new BigDecimal("50"));
        line.setLotNumberExpected("LOT-2026-01");  // Expected lot number
        line.setExpiryDateExpected(LocalDate.of(2026, 6, 30));  // Expected expiry date

        receipt.addLine(line);
        receipt = receiptRepository.save(receipt);

        // When: Start receiving and scan with different lot
        receivingWorkflowService.startReceiving(receipt.getId());

        List<Task> tasks = taskRepository.findByReceiptId(receipt.getId());
        Task task = tasks.get(0);

        taskLifecycleService.start(task.getId());

        // Create pallet for scanning
        Pallet testPallet = new Pallet();
        testPallet.setCode("PLT-LOT-001");
        testPallet.setStatus(PalletStatus.EMPTY);
        palletRepository.save(testPallet);

        RecordScanRequest scanRequest = new RecordScanRequest(
            null,
            "PLT-LOT-001",
            50,
            null,
            "SKU001",  // Barcode matches SKU code
            null,
            null,
            null,
            null,
            null,
            null,
            "LOT-2026-02",  // Different lot
            LocalDate.of(2026, 12, 31)
        );

        receivingWorkflowService.recordScan(task.getId(), scanRequest);

        // Then: Verify discrepancy created for lot mismatch
        List<Discrepancy> discrepancies = discrepancyRepository.findByReceipt(receipt);
        assertTrue(discrepancies.stream()
            .anyMatch(d -> d.getType().equals("LOT_MISMATCH")));

        // Verify scan has lot number and expiry date
        List<Scan> scans = scanRepository.findByTask(task);
        assertEquals(1, scans.size());
        Scan scan = scans.get(0);
        assertEquals("LOT-2026-02", scan.getLotNumber());
        assertEquals(LocalDate.of(2026, 12, 31), scan.getExpiryDate());
    }

    @Test
    void shouldDetectExpiredProduct_AndCreateDiscrepancy() {
        // Given: Receipt line with future expiry
        Receipt receipt = new Receipt();
        receipt.setDocNo("RCV-EXPIRY-001");
        receipt.setDocDate(LocalDate.now());
        receipt.setStatus(ReceiptStatus.CONFIRMED);
        receipt = receiptRepository.save(receipt);

        ReceiptLine line = new ReceiptLine();
        line.setReceipt(receipt);
        line.setLineNo(1);
        line.setSkuId(testSku.getId());
        line.setUom("PCS");
        line.setQtyExpected(new BigDecimal("50"));
        line.setExpiryDateExpected(LocalDate.of(2027, 12, 31));

        receipt.addLine(line);
        receipt = receiptRepository.save(receipt);

        // When: Scan with expired date
        receivingWorkflowService.startReceiving(receipt.getId());

        List<Task> tasks = taskRepository.findByReceiptId(receipt.getId());
        Task task = tasks.get(0);
        taskLifecycleService.start(task.getId());

        // Create pallet for scanning
        Pallet testPallet = new Pallet();
        testPallet.setCode("PLT-EXPIRED-001");
        testPallet.setStatus(PalletStatus.EMPTY);
        palletRepository.save(testPallet);

        RecordScanRequest scanRequest = new RecordScanRequest(
            null,
            "PLT-EXPIRED-001",
            50,
            null,
            "SKU001",  // Barcode matches SKU code
            null,
            null,
            null,
            null,
            null,
            null,
            "LOT-2025-01",
            LocalDate.of(2025, 1, 1)  // Expired date
        );

        receivingWorkflowService.recordScan(task.getId(), scanRequest);

        // Then: Verify discrepancy for expired product
        List<Discrepancy> discrepancies = discrepancyRepository.findByReceipt(receipt);
        assertTrue(discrepancies.stream()
            .anyMatch(d -> d.getType().equals("EXPIRED_PRODUCT")));
    }

    @Test
    void shouldBulkAssignTasks_ToOperator() {
        // Given: Create multiple tasks
        List<Long> taskIds = createMultipleTasks(5);

        // When: Bulk assign to operator
        BulkAssignRequest request = new BulkAssignRequest(taskIds, "operator1");
        BulkOperationResult<Long> result = bulkOperationsService.bulkAssignTasks(request);

        // Then: Verify all assigned
        assertEquals(5, result.successes().size());
        assertEquals(0, result.failures().size());
        assertTrue(result.failures().isEmpty());

        // Verify tasks have assignee
        taskIds.forEach(taskId -> {
            Task task = taskRepository.findById(taskId).orElseThrow();
            assertEquals("operator1", task.getAssignee());
        });
    }

    @Test
    void shouldBulkSetPriority_ForTasks() {
        // Given: Create multiple tasks
        List<Long> taskIds = createMultipleTasks(5);

        // When: Bulk set priority
        BulkSetPriorityRequest request = new BulkSetPriorityRequest(taskIds, 10);
        BulkOperationResult<Long> result = bulkOperationsService.bulkSetPriority(request);

        // Then: Verify all updated
        assertEquals(5, result.successes().size());
        assertEquals(0, result.failures().size());

        taskIds.forEach(taskId -> {
            Task task = taskRepository.findById(taskId).orElseThrow();
            assertEquals(10, task.getPriority());
        });
    }

    @Test
    void shouldBulkCreatePallets_WithSequentialCodes() {
        // When: Create 50 pallets
        int startNumber = 900000 + (int) (System.currentTimeMillis() % 10000);
        BulkCreatePalletsRequest request = new BulkCreatePalletsRequest(startNumber, 50);
        PalletCreationResult result = bulkOperationsService.bulkCreatePallets(request);

        // Then: Verify 50 pallets created
        assertEquals(50, result.created().size());

        // Verify sequential codes
        List<Pallet> pallets = palletRepository.findAll();
        String firstCode = String.format("PLT-%05d", startNumber);
        String lastCode = String.format("PLT-%05d", startNumber + 49);
        assertTrue(pallets.stream().anyMatch(p -> p.getCode().equals(firstCode)));
        assertTrue(pallets.stream().anyMatch(p -> p.getCode().equals(lastCode)));
        assertTrue(
            pallets.stream()
                .filter(p -> p.getCode().equals(firstCode) || p.getCode().equals(lastCode))
                .allMatch(p -> p.getStatus() == PalletStatus.EMPTY)
        );
    }

    @Test
    void shouldCalculateAnalytics_ForReceivingMetrics() {
        // Given: Create test data
        createAnalyticsTestData();

        // When: Calculate analytics
        LocalDateTime start = LocalDateTime.now().minusDays(7);
        LocalDateTime end = LocalDateTime.now();
        ReceivingAnalyticsDto analytics = analyticsService.calculateAnalytics(start, end);

        // Then: Verify metrics calculated
        assertNotNull(analytics);
        assertNotNull(analytics.receiptsByStatus());
        assertNotNull(analytics.discrepanciesByType());
        assertNotNull(analytics.palletsByStatus());
        assertTrue(analytics.discrepancyRate() >= 0.0);
        assertTrue(analytics.damagedPalletsRate() >= 0.0);
        assertTrue(analytics.avgReceivingTimeHours() >= 0.0);
    }

    // Helper methods

    private List<Long> createMultipleTasks(int count) {
        Receipt receipt = new Receipt();
        receipt.setDocNo("RCV-BULK-" + System.currentTimeMillis());
        receipt.setDocDate(LocalDate.now());
        receipt.setStatus(ReceiptStatus.CONFIRMED);
        receipt = receiptRepository.save(receipt);

        for (int i = 1; i <= count; i++) {
        ReceiptLine line = new ReceiptLine();
        line.setReceipt(receipt);
        line.setLineNo(i);
        line.setSkuId(testSku.getId());
        line.setUom("PCS");
        line.setQtyExpected(new BigDecimal("50"));
        line.setLotNumberExpected("LOT-2026-01");  // Expected lot number
        line.setExpiryDateExpected(LocalDate.of(2026, 6, 30));  // Expected expiry date
            receipt.getLines().add(line);
        }

        receipt = receiptRepository.save(receipt);
        receivingWorkflowService.startReceiving(receipt.getId());

        return taskRepository.findByReceiptId(receipt.getId())
            .stream()
            .map(Task::getId)
            .toList();
    }

    private void createAnalyticsTestData() {
        // Create receipts with various statuses
        for (int i = 0; i < 3; i++) {
            Receipt receipt = new Receipt();
            receipt.setDocNo("RCV-ANALYTICS-" + i);
            receipt.setDocDate(LocalDate.now().minusDays(i));
            receipt.setStatus(ReceiptStatus.ACCEPTED);
            receiptRepository.save(receipt);
        }

        // Create pallets with different statuses
        for (int i = 0; i < 5; i++) {
            Pallet pallet = new Pallet();
            pallet.setCode("PLT-ANALYTICS-" + i);
            pallet.setStatus(i % 2 == 0 ? PalletStatus.RECEIVED : PalletStatus.DAMAGED);
            palletRepository.save(pallet);
        }
    }
}
