package com.wmsdipl.core.service.workflow;

import com.wmsdipl.contracts.dto.ShippingWaveActionResultDto;
import com.wmsdipl.contracts.dto.ShippingWaveDto;
import com.wmsdipl.core.domain.Receipt;
import com.wmsdipl.core.domain.ReceiptStatus;
import com.wmsdipl.core.domain.Task;
import com.wmsdipl.core.domain.TaskStatus;
import com.wmsdipl.core.domain.TaskType;
import com.wmsdipl.core.repository.ReceiptRepository;
import com.wmsdipl.core.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShippingWaveServiceTest {

    @Mock
    private ReceiptRepository receiptRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ShippingWorkflowService shippingWorkflowService;

    @InjectMocks
    private ShippingWaveService shippingWaveService;

    @Test
    void shouldBuildWaveSummary() {
        Receipt receipt = new Receipt();
        receipt.setId(1L);
        receipt.setDocNo("RCP-1");
        receipt.setCrossDock(true);
        receipt.setOutboundRef("OUT-1");
        receipt.setStatus(ReceiptStatus.READY_FOR_SHIPMENT);

        Task shippingTask = new Task();
        shippingTask.setStatus(TaskStatus.ASSIGNED);

        when(receiptRepository.findByCrossDockTrueAndOutboundRefIsNotNull()).thenReturn(List.of(receipt));
        when(taskRepository.findByTaskTypeAndReceiptIdIn(TaskType.SHIPPING, List.of(1L))).thenReturn(List.of(shippingTask));

        List<ShippingWaveDto> waves = shippingWaveService.listWaves();

        assertEquals(1, waves.size());
        assertEquals("OUT-1", waves.get(0).outboundRef());
        assertEquals("READY", waves.get(0).status());
        assertEquals(1, waves.get(0).openShippingTasks());
    }

    @Test
    void shouldStartWave_ForReadyReceipts() {
        Receipt receipt = new Receipt();
        receipt.setId(10L);
        receipt.setDocNo("RCP-10");
        receipt.setCrossDock(true);
        receipt.setOutboundRef("OUT-2");
        receipt.setStatus(ReceiptStatus.READY_FOR_SHIPMENT);

        when(receiptRepository.findByCrossDockTrueAndOutboundRef("OUT-2")).thenReturn(List.of(receipt));
        when(shippingWorkflowService.startShipping(10L)).thenReturn(3);

        ShippingWaveActionResultDto result = shippingWaveService.startWave("OUT-2");

        assertEquals(1, result.targetedReceipts());
        assertEquals(1, result.affectedReceipts());
        assertEquals(3, result.tasksCreated());
        assertEquals(0, result.blockedReceiptIds().size());
        verify(shippingWorkflowService).startShipping(10L);
    }

    @Test
    void shouldBlockCompleteWave_ForNonShippingStatuses() {
        Receipt receipt = new Receipt();
        receipt.setId(11L);
        receipt.setDocNo("RCP-11");
        receipt.setCrossDock(true);
        receipt.setOutboundRef("OUT-3");
        receipt.setStatus(ReceiptStatus.READY_FOR_SHIPMENT);

        when(receiptRepository.findByCrossDockTrueAndOutboundRef("OUT-3")).thenReturn(List.of(receipt));
        when(taskRepository.findByTaskTypeAndReceiptIdIn(TaskType.SHIPPING, List.of(11L))).thenReturn(List.of());

        ShippingWaveActionResultDto result = shippingWaveService.completeWave("OUT-3");

        assertEquals(1, result.targetedReceipts());
        assertEquals(0, result.affectedReceipts());
        assertEquals(1, result.blockedReceiptIds().size());
        verify(shippingWorkflowService, never()).completeShipping(11L);
    }

    @Test
    void shouldNotCompleteWave_WhenShippingTasksAreIncomplete() {
        Receipt receipt = new Receipt();
        receipt.setId(12L);
        receipt.setDocNo("RCP-12");
        receipt.setCrossDock(true);
        receipt.setOutboundRef("OUT-4");
        receipt.setStatus(ReceiptStatus.SHIPPING_IN_PROGRESS);

        Task shippingTask = new Task();
        shippingTask.setStatus(TaskStatus.IN_PROGRESS);
        shippingTask.setReceipt(receipt);

        when(receiptRepository.findByCrossDockTrueAndOutboundRef("OUT-4")).thenReturn(List.of(receipt));
        when(taskRepository.findByTaskTypeAndReceiptIdIn(TaskType.SHIPPING, List.of(12L))).thenReturn(List.of(shippingTask));

        ShippingWaveActionResultDto result = shippingWaveService.completeWave("OUT-4");

        assertEquals(1, result.targetedReceipts());
        assertEquals(0, result.affectedReceipts());
        assertEquals(List.of(12L), result.blockedReceiptIds());
        verify(shippingWorkflowService, never()).completeShipping(12L);
    }

    @Test
    void shouldShowConcreteReceiptStatus_WhenWaveHasSingleNonShippingState() {
        Receipt receipt = new Receipt();
        receipt.setId(20L);
        receipt.setDocNo("RCP-20");
        receipt.setCrossDock(true);
        receipt.setOutboundRef("OUT-5");
        receipt.setStatus(ReceiptStatus.DRAFT);

        when(receiptRepository.findByCrossDockTrueAndOutboundRefIsNotNull()).thenReturn(List.of(receipt));
        when(taskRepository.findByTaskTypeAndReceiptIdIn(TaskType.SHIPPING, List.of(20L))).thenReturn(List.of());

        List<ShippingWaveDto> waves = shippingWaveService.listWaves();

        assertEquals(1, waves.size());
        assertEquals("DRAFT", waves.get(0).status());
    }

    @Test
    void shouldKeepMixed_WhenWaveContainsDifferentNonShippingStates() {
        Receipt r1 = new Receipt();
        r1.setId(30L);
        r1.setDocNo("RCP-30");
        r1.setCrossDock(true);
        r1.setOutboundRef("OUT-6");
        r1.setStatus(ReceiptStatus.DRAFT);

        Receipt r2 = new Receipt();
        r2.setId(31L);
        r2.setDocNo("RCP-31");
        r2.setCrossDock(true);
        r2.setOutboundRef("OUT-6");
        r2.setStatus(ReceiptStatus.READY_FOR_PLACEMENT);

        when(receiptRepository.findByCrossDockTrueAndOutboundRefIsNotNull()).thenReturn(List.of(r1, r2));
        when(taskRepository.findByTaskTypeAndReceiptIdIn(TaskType.SHIPPING, List.of(30L, 31L))).thenReturn(List.of());

        List<ShippingWaveDto> waves = shippingWaveService.listWaves();

        assertEquals(1, waves.size());
        assertEquals("MIXED", waves.get(0).status());
    }
}
