package com.wmsdipl.core.service.workflow;

import com.wmsdipl.contracts.dto.RecordScanRequest;
import com.wmsdipl.core.domain.*;
import com.wmsdipl.core.repository.*;
import com.wmsdipl.core.service.DuplicateScanDetectionService;
import com.wmsdipl.core.service.ReceiptService;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
    private StockMovementService stockMovementService;
    @Mock
    private DuplicateScanDetectionService duplicateScanDetectionService;
    @Mock
    private ReceiptService receiptService;

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
}
