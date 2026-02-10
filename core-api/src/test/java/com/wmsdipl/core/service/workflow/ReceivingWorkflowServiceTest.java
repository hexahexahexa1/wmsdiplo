package com.wmsdipl.core.service.workflow;

import com.wmsdipl.contracts.dto.RecordScanRequest;
import com.wmsdipl.contracts.dto.DamageType;
import com.wmsdipl.core.domain.*;
import com.wmsdipl.core.repository.*;
import com.wmsdipl.core.service.DuplicateScanDetectionService;
import com.wmsdipl.core.service.ReceiptService;
import com.wmsdipl.core.service.ReceiptWorkflowBlockerService;
import com.wmsdipl.core.service.SkuService;
import com.wmsdipl.core.service.StockMovementService;
import com.wmsdipl.core.service.TaskLifecycleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
class ReceivingWorkflowServiceTest {

    @Mock
    private ReceiptRepository receiptRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private ScanRepository scanRepository;
    @Mock
    private DiscrepancyRepository discrepancyRepository;
    @Mock
    private TaskLifecycleService taskLifecycleService;
    @Mock
    private PalletRepository palletRepository;
    @Mock
    private LocationRepository locationRepository;
    @Mock
    private SkuRepository skuRepository;
    @Mock
    private SkuService skuService;
    @Mock
    private StockMovementService stockMovementService;
    @Mock
    private DuplicateScanDetectionService duplicateScanDetectionService;
    @Mock
    private ReceiptService receiptService;
    @Mock
    private ReceiptWorkflowBlockerService receiptWorkflowBlockerService;

    @InjectMocks
    private ReceivingWorkflowService receivingWorkflowService;

    private Receipt testReceipt;
    private ReceiptLine testLine;

    @BeforeEach
    void setUp() throws Exception {
        testReceipt = new Receipt();
        testReceipt.setStatus(ReceiptStatus.CONFIRMED);
        Field receiptIdField = Receipt.class.getDeclaredField("id");
        receiptIdField.setAccessible(true);
        receiptIdField.set(testReceipt, 1L);

        testLine = new ReceiptLine();
        testLine.setLineNo(1);
        testLine.setQtyExpected(BigDecimal.TEN);
        testReceipt.setLines(List.of(testLine));
    }

    @Test
    void shouldStartReceiving_AndReturnCount() {
        when(receiptRepository.findById(1L)).thenReturn(Optional.of(testReceipt));
        when(taskRepository.findByReceiptIdAndTaskType(1L, TaskType.RECEIVING)).thenReturn(Collections.emptyList());
        
        int count = receivingWorkflowService.startReceiving(1L);
        
        assertEquals(1, count);
        assertEquals(ReceiptStatus.IN_PROGRESS, testReceipt.getStatus());
        verify(taskRepository, times(1)).save(any(Task.class));
        verify(receiptService).ensureReceiptLinesReadyForWorkflow(testReceipt);
    }

    @Test
    void shouldAutoCompleteReceipt_WhenLastTaskCompleted() {
        testReceipt.setStatus(ReceiptStatus.IN_PROGRESS);
        
        Task task1 = new Task();
        task1.setStatus(TaskStatus.COMPLETED);
        Task task2 = new Task();
        task2.setStatus(TaskStatus.COMPLETED);
        
        when(receiptRepository.findById(1L)).thenReturn(Optional.of(testReceipt));
        when(taskRepository.findByReceiptIdAndTaskType(1L, TaskType.RECEIVING)).thenReturn(List.of(task1, task2));
        
        receivingWorkflowService.checkAndCompleteReceipt(1L);
        
        assertEquals(ReceiptStatus.ACCEPTED, testReceipt.getStatus());
    }

    @Test
    void shouldTransitionToReadyForPlacement_WhenCrossDock() {
        testReceipt.setStatus(ReceiptStatus.IN_PROGRESS);
        testReceipt.setCrossDock(true);
        
        Task task1 = new Task();
        task1.setStatus(TaskStatus.COMPLETED);
        
        when(receiptRepository.findById(1L)).thenReturn(Optional.of(testReceipt));
        when(taskRepository.findByReceiptIdAndTaskType(1L, TaskType.RECEIVING)).thenReturn(List.of(task1));
        
        receivingWorkflowService.checkAndCompleteReceipt(1L);
        
        assertEquals(ReceiptStatus.READY_FOR_PLACEMENT, testReceipt.getStatus());
    }

    @Test
    void shouldReturnReplay_WhenSameRequestIdIsSentTwice() {
        Task task = new Task();
        task.setTaskType(TaskType.RECEIVING);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setReceipt(testReceipt);

        ReceiptLine line = new ReceiptLine();
        line.setSkuId(1L);
        line.setQtyExpected(BigDecimal.TEN);
        task.setLine(line);
        testReceipt.setStatus(ReceiptStatus.IN_PROGRESS);

        Scan existing = new Scan();
        existing.setRequestId("req-123");

        when(taskLifecycleService.getTask(1L)).thenReturn(task);
        when(scanRepository.findByTaskIdAndRequestId(1L, "req-123")).thenReturn(Optional.of(existing));

        RecordScanRequest request = new RecordScanRequest(
            "req-123",
            "PLT-001",
            10,
            null,
            null,
            null,
            null,
            null,
            false,
            null,
            null,
            null,
            null
        );

        Scan result = receivingWorkflowService.recordScan(1L, request);

        assertTrue(Boolean.TRUE.equals(result.getDuplicate()));
        assertTrue(Boolean.TRUE.equals(result.getIdempotentReplay()));
        verifyNoInteractions(palletRepository);
    }

    @Test
    void shouldRejectScan_WhenDamageFlagTrueAndDamageTypeMissing() {
        Task task = new Task();
        task.setTaskType(TaskType.RECEIVING);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setReceipt(testReceipt);

        ReceiptLine line = new ReceiptLine();
        line.setSkuId(1L);
        line.setQtyExpected(BigDecimal.TEN);
        task.setLine(line);
        testReceipt.setStatus(ReceiptStatus.IN_PROGRESS);

        when(taskLifecycleService.getTask(1L)).thenReturn(task);

        RecordScanRequest request = new RecordScanRequest(
            null,
            "PLT-001",
            10,
            null,
            null,
            null,
            null,
            null,
            true,
            null,
            "damage",
            null,
            null
        );

        assertThrows(org.springframework.web.server.ResponseStatusException.class,
            () -> receivingWorkflowService.recordScan(1L, request));
    }

    @Test
    void shouldLinkRejectedScannedSkuAsDraftSkuId_WhenBarcodeMismatch() {
        testReceipt.setStatus(ReceiptStatus.IN_PROGRESS);

        ReceiptLine line = new ReceiptLine();
        line.setSkuId(1L);
        line.setQtyExpected(BigDecimal.TEN);
        line.setUom("PCS");

        Task task = new Task();
        task.setTaskType(TaskType.RECEIVING);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setReceipt(testReceipt);
        task.setLine(line);
        task.setQtyAssigned(BigDecimal.TEN);
        task.setQtyDone(BigDecimal.ZERO);
        task.setAssignee("operator");

        Pallet pallet = new Pallet();
        pallet.setCode("PLT-REJ-001");
        pallet.setQuantity(BigDecimal.ZERO);

        Location receivingLocation = new Location();
        receivingLocation.setLocationType(LocationType.RECEIVING);
        receivingLocation.setStatus(LocationStatus.AVAILABLE);

        Sku expectedSku = new Sku();
        expectedSku.setId(1L);
        expectedSku.setCode("SKU-EXP-001");
        expectedSku.setStatus(SkuStatus.ACTIVE);

        Sku rejectedScannedSku = new Sku();
        rejectedScannedSku.setId(77L);
        rejectedScannedSku.setCode("SKU-REJ-001");
        rejectedScannedSku.setStatus(SkuStatus.REJECTED);

        when(taskLifecycleService.getTask(1L)).thenReturn(task);
        when(duplicateScanDetectionService.checkScan("PLT-REJ-001"))
            .thenReturn(DuplicateScanDetectionService.ScanResult.valid("PLT-REJ-001"));
        when(palletRepository.findByCode("PLT-REJ-001")).thenReturn(Optional.of(pallet));
        when(locationRepository.findFirstByLocationTypeAndStatusAndActiveTrueOrderByIdAsc(
            LocationType.RECEIVING, LocationStatus.AVAILABLE
        )).thenReturn(Optional.of(receivingLocation));
        when(skuRepository.findById(1L)).thenReturn(Optional.of(expectedSku));
        when(skuRepository.findByCode("SKU-REJ-001")).thenReturn(Optional.of(rejectedScannedSku));
        when(palletRepository.save(any(Pallet.class))).thenAnswer(inv -> inv.getArgument(0));
        when(scanRepository.save(any(Scan.class))).thenAnswer(inv -> inv.getArgument(0));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        RecordScanRequest request = new RecordScanRequest(
            null,
            "PLT-REJ-001",
            1,
            null,
            "SKU-REJ-001",
            null,
            null,
            null,
            false,
            null,
            null,
            null,
            null
        );

        receivingWorkflowService.recordScan(1L, request);

        ArgumentCaptor<Discrepancy> discrepancyCaptor = ArgumentCaptor.forClass(Discrepancy.class);
        verify(discrepancyRepository).save(discrepancyCaptor.capture());
        Discrepancy discrepancy = discrepancyCaptor.getValue();
        assertEquals("BARCODE_MISMATCH", discrepancy.getType());
        assertEquals(77L, discrepancy.getDraftSkuId());
    }

    @Test
    void shouldUseTaskAssignedQtyAsExpected_WhenCreatingDamageDiscrepancyForSplitTask() {
        // Given
        testReceipt.setStatus(ReceiptStatus.IN_PROGRESS);

        ReceiptLine line = new ReceiptLine();
        line.setSkuId(1L);
        line.setQtyExpected(BigDecimal.valueOf(4)); // Full line quantity (split across tasks)
        line.setUom("PCS");

        Task task = new Task();
        task.setTaskType(TaskType.RECEIVING);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setReceipt(testReceipt);
        task.setLine(line);
        task.setQtyAssigned(BigDecimal.ONE); // This конкретная task expects 1
        task.setQtyDone(BigDecimal.ZERO);
        task.setAssignee("operator");
        try {
            Field taskIdField = Task.class.getDeclaredField("id");
            taskIdField.setAccessible(true);
            taskIdField.set(task, 415L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Pallet pallet = new Pallet();
        pallet.setCode("PLT-00002");
        pallet.setQuantity(BigDecimal.ZERO);

        Location receivingLocation = new Location();
        receivingLocation.setLocationType(LocationType.RECEIVING);
        receivingLocation.setStatus(LocationStatus.AVAILABLE);

        when(taskLifecycleService.getTask(415L)).thenReturn(task);
        when(duplicateScanDetectionService.checkScan("PLT-00002"))
            .thenReturn(DuplicateScanDetectionService.ScanResult.valid("PLT-00002"));
        when(palletRepository.findByCode("PLT-00002")).thenReturn(Optional.of(pallet));
        when(locationRepository.findFirstByLocationTypeAndStatusAndActiveTrueOrderByIdAsc(
            LocationType.RECEIVING, LocationStatus.AVAILABLE
        )).thenReturn(Optional.of(receivingLocation));
        when(palletRepository.save(any(Pallet.class))).thenAnswer(inv -> inv.getArgument(0));
        when(scanRepository.save(any(Scan.class))).thenAnswer(inv -> inv.getArgument(0));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        RecordScanRequest request = new RecordScanRequest(
            null,
            "PLT-00002",
            1,
            null,
            null,
            null,
            null,
            null,
            true,
            DamageType.PHYSICAL_DAMAGE,
            null,
            null,
            null
        );

        // When
        receivingWorkflowService.recordScan(415L, request);

        // Then
        ArgumentCaptor<Discrepancy> discrepancyCaptor = ArgumentCaptor.forClass(Discrepancy.class);
        verify(discrepancyRepository).save(discrepancyCaptor.capture());
        Discrepancy discrepancy = discrepancyCaptor.getValue();
        assertEquals("DAMAGE", discrepancy.getType());
        assertEquals(0, discrepancy.getQtyExpected().compareTo(BigDecimal.ONE));
        assertEquals(0, discrepancy.getQtyActual().compareTo(BigDecimal.ONE));
    }

    @Test
    void shouldUseTaskAssignedQtyAsExpected_WhenCreatingLotMismatchDiscrepancyForSplitTask() {
        // Given
        testReceipt.setStatus(ReceiptStatus.IN_PROGRESS);

        ReceiptLine line = new ReceiptLine();
        line.setSkuId(1L);
        line.setQtyExpected(BigDecimal.valueOf(3)); // Full line quantity
        line.setUom("BOX");
        line.setLotNumberExpected("LOT-EXPECTED-001");

        Task task = new Task();
        task.setTaskType(TaskType.RECEIVING);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setReceipt(testReceipt);
        task.setLine(line);
        task.setQtyAssigned(BigDecimal.ONE); // This concrete task expects 1
        task.setQtyDone(BigDecimal.ZERO);
        task.setAssignee("admin");
        try {
            Field taskIdField = Task.class.getDeclaredField("id");
            taskIdField.setAccessible(true);
            taskIdField.set(task, 437L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Pallet pallet = new Pallet();
        pallet.setCode("PLT-LOT-001");
        pallet.setQuantity(BigDecimal.ZERO);

        Location receivingLocation = new Location();
        receivingLocation.setLocationType(LocationType.RECEIVING);
        receivingLocation.setStatus(LocationStatus.AVAILABLE);

        when(taskLifecycleService.getTask(437L)).thenReturn(task);
        when(duplicateScanDetectionService.checkScan("PLT-LOT-001"))
            .thenReturn(DuplicateScanDetectionService.ScanResult.valid("PLT-LOT-001"));
        when(palletRepository.findByCode("PLT-LOT-001")).thenReturn(Optional.of(pallet));
        when(locationRepository.findFirstByLocationTypeAndStatusAndActiveTrueOrderByIdAsc(
            LocationType.RECEIVING, LocationStatus.AVAILABLE
        )).thenReturn(Optional.of(receivingLocation));
        when(palletRepository.save(any(Pallet.class))).thenAnswer(inv -> inv.getArgument(0));
        when(scanRepository.save(any(Scan.class))).thenAnswer(inv -> inv.getArgument(0));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        RecordScanRequest request = new RecordScanRequest(
            null,
            "PLT-LOT-001",
            1,
            null,
            null,
            null,
            null,
            null,
            false,
            null,
            null,
            "LOT-ACTUAL-002",
            LocalDate.now().plusDays(30)
        );

        // When
        receivingWorkflowService.recordScan(437L, request);

        // Then
        ArgumentCaptor<Discrepancy> discrepancyCaptor = ArgumentCaptor.forClass(Discrepancy.class);
        verify(discrepancyRepository).save(discrepancyCaptor.capture());
        Discrepancy discrepancy = discrepancyCaptor.getValue();
        assertEquals("LOT_MISMATCH", discrepancy.getType());
        assertEquals(0, discrepancy.getQtyExpected().compareTo(BigDecimal.ONE));
        assertEquals(0, discrepancy.getQtyActual().compareTo(BigDecimal.ONE));
    }
}
