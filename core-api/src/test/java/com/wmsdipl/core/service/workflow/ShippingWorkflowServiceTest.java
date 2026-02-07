package com.wmsdipl.core.service.workflow;

import com.wmsdipl.contracts.dto.RecordScanRequest;
import com.wmsdipl.core.domain.Location;
import com.wmsdipl.core.domain.LocationType;
import com.wmsdipl.core.domain.Pallet;
import com.wmsdipl.core.domain.PalletMovement;
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
import com.wmsdipl.core.service.StockMovementService;
import com.wmsdipl.core.service.TaskLifecycleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShippingWorkflowServiceTest {

    @Mock
    private ReceiptRepository receiptRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskLifecycleService taskLifecycleService;

    @Mock
    private PalletRepository palletRepository;

    @Mock
    private ScanRepository scanRepository;

    @Mock
    private StockMovementService stockMovementService;

    @Mock
    private DuplicateScanDetectionService duplicateScanDetectionService;

    @InjectMocks
    private ShippingWorkflowService shippingWorkflowService;

    private Receipt crossDockReceipt;
    private Pallet pallet;
    private Location crossDockLocation;

    @BeforeEach
    void setUp() {
        crossDockReceipt = new Receipt();
        crossDockReceipt.setId(1L);
        crossDockReceipt.setCrossDock(true);
        crossDockReceipt.setStatus(ReceiptStatus.READY_FOR_SHIPMENT);

        crossDockLocation = new Location();
        crossDockLocation.setId(11L);
        crossDockLocation.setCode("XDOCK-01");
        crossDockLocation.setLocationType(LocationType.CROSS_DOCK);

        pallet = new Pallet();
        pallet.setId(100L);
        pallet.setCode("PLT-100");
        pallet.setReceipt(crossDockReceipt);
        pallet.setLocation(crossDockLocation);
        pallet.setStatus(PalletStatus.PLACED);
        pallet.setQuantity(BigDecimal.TEN);
    }

    @Test
    void shouldStartShipping_WhenCrossDockReceiptReady() {
        when(receiptRepository.findById(1L)).thenReturn(Optional.of(crossDockReceipt));
        when(palletRepository.findByReceipt(crossDockReceipt)).thenReturn(List.of(pallet));
        when(taskRepository.findByReceiptIdAndTaskType(1L, TaskType.SHIPPING)).thenReturn(List.of());
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(palletRepository.save(any(Pallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(receiptRepository.save(any(Receipt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        int created = shippingWorkflowService.startShipping(1L);

        assertEquals(1, created);
        assertEquals(ReceiptStatus.SHIPPING_IN_PROGRESS, crossDockReceipt.getStatus());
        assertEquals(PalletStatus.PICKING, pallet.getStatus());
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    void shouldRejectStartShipping_WhenReceiptIsNotCrossDock() {
        Receipt regular = new Receipt();
        regular.setId(2L);
        regular.setCrossDock(false);
        regular.setStatus(ReceiptStatus.READY_FOR_SHIPMENT);
        when(receiptRepository.findById(2L)).thenReturn(Optional.of(regular));

        assertThrows(ResponseStatusException.class, () -> shippingWorkflowService.startShipping(2L));
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void shouldRecordShippingScanAndReducePalletQuantity() {
        Task task = new Task();
        task.setId(5L);
        task.setTaskType(TaskType.SHIPPING);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setPalletId(100L);
        task.setQtyDone(BigDecimal.ZERO);
        task.setQtyAssigned(BigDecimal.TEN);
        task.setReceipt(crossDockReceipt);
        task.setAssignee("operator1");

        RecordScanRequest request = new RecordScanRequest(
            "req-1",
            "PLT-100",
            4,
            null,
            null,
            null,
            "device-1",
            null,
            null,
            null,
            null,
            null,
            null
        );

        when(taskLifecycleService.getTask(5L)).thenReturn(task);
        when(scanRepository.findByTaskIdAndRequestId(5L, "req-1")).thenReturn(Optional.empty());
        when(duplicateScanDetectionService.checkScan("PLT-100"))
            .thenReturn(DuplicateScanDetectionService.ScanResult.valid("PLT-100"));
        when(palletRepository.findByCode("PLT-100")).thenReturn(Optional.of(pallet));
        when(stockMovementService.recordPick(any(Pallet.class), any(Location.class), eq(BigDecimal.valueOf(4)), eq("operator1"), eq(5L)))
            .thenReturn(new PalletMovement());
        when(palletRepository.save(any(Pallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(scanRepository.save(any(Scan.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Scan savedScan = shippingWorkflowService.recordShipping(5L, request);

        assertNotNull(savedScan);
        assertEquals(BigDecimal.valueOf(6), pallet.getQuantity());
        assertEquals(PalletStatus.PICKING, pallet.getStatus());
        assertEquals(BigDecimal.valueOf(4), task.getQtyDone());

        ArgumentCaptor<Scan> scanCaptor = ArgumentCaptor.forClass(Scan.class);
        verify(scanRepository).save(scanCaptor.capture());
        assertEquals("req-1", scanCaptor.getValue().getRequestId());
        verify(taskLifecycleService, times(1)).autoStartIfNeeded(task);
        verify(taskRepository, times(1)).save(task);
    }

    @Test
    void shouldCompleteShipping_WhenAllShippingTasksCompleted() {
        crossDockReceipt.setStatus(ReceiptStatus.SHIPPING_IN_PROGRESS);

        Task done1 = new Task();
        done1.setStatus(TaskStatus.COMPLETED);
        Task done2 = new Task();
        done2.setStatus(TaskStatus.COMPLETED);

        when(receiptRepository.findById(1L)).thenReturn(Optional.of(crossDockReceipt));
        when(taskRepository.findByReceiptIdAndTaskType(1L, TaskType.SHIPPING)).thenReturn(List.of(done1, done2));
        when(receiptRepository.save(any(Receipt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        shippingWorkflowService.completeShipping(1L);

        assertEquals(ReceiptStatus.SHIPPED, crossDockReceipt.getStatus());
        verify(receiptRepository, times(1)).save(crossDockReceipt);
    }
}
