package com.wmsdipl.core.service.workflow;

import com.wmsdipl.contracts.dto.RecordScanRequest;
import com.wmsdipl.core.domain.*;
import com.wmsdipl.core.repository.*;
import com.wmsdipl.core.service.DuplicateScanDetectionService;
import com.wmsdipl.core.service.PutawayService;
import com.wmsdipl.core.service.TaskLifecycleService;
import com.wmsdipl.core.service.StockMovementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlacementWorkflowServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private PalletRepository palletRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private ScanRepository scanRepository;

    @Mock
    private TaskLifecycleService taskLifecycleService;

    @Mock
    private ReceiptRepository receiptRepository;

    @Mock
    private PutawayService putawayService;

    @Mock
    private StockMovementService stockMovementService;

    @Mock
    private DuplicateScanDetectionService duplicateScanDetectionService;

    @InjectMocks
    private PlacementWorkflowService placementWorkflowService;

    private Receipt testReceipt;
    private Task testTask;
    private Pallet testPallet;
    private Location sourceLocation;
    private Location targetLocation;
    private Scan testScan;

    @BeforeEach
    void setUp() throws Exception {
        testReceipt = new Receipt();
        testReceipt.setDocNo("DOC001");
        testReceipt.setStatus(ReceiptStatus.ACCEPTED);
        // Set ID using reflection
        Field receiptIdField = Receipt.class.getDeclaredField("id");
        receiptIdField.setAccessible(true);
        receiptIdField.set(testReceipt, 1L);

        sourceLocation = new Location();
        sourceLocation.setCode("RECEIVING-01");
        Field sourceLocationIdField = Location.class.getDeclaredField("id");
        sourceLocationIdField.setAccessible(true);
        sourceLocationIdField.set(sourceLocation, 1L);

        targetLocation = new Location();
        targetLocation.setCode("STORAGE-A-01");
        Field targetLocationIdField = Location.class.getDeclaredField("id");
        targetLocationIdField.setAccessible(true);
        targetLocationIdField.set(targetLocation, 2L);

        testPallet = new Pallet();
        testPallet.setCode("PALLET001");
        testPallet.setLocation(sourceLocation);
        testPallet.setStatus(PalletStatus.RECEIVED);
        testPallet.setQuantity(BigDecimal.TEN);

        testTask = new Task();
        testTask.setReceipt(testReceipt);
        testTask.setTaskType(TaskType.PLACEMENT);
        testTask.setStatus(TaskStatus.IN_PROGRESS);
        testTask.setPalletId(1L);
        testTask.setSourceLocationId(1L);
        testTask.setTargetLocationId(2L);
        testTask.setAssignee("operator1");

        testScan = new Scan();
        testScan.setTask(testTask);
        testScan.setPalletCode("PALLET001");

        lenient().when(duplicateScanDetectionService.checkScan(anyString()))
            .thenReturn(DuplicateScanDetectionService.ScanResult.valid("PALLET001"));
    }

    @Test
    void shouldStartPlacement_WhenReceiptAccepted() {
        // Given
        Task createdTask = new Task();
        createdTask.setTaskType(TaskType.PLACEMENT);
        List<Task> tasks = List.of(createdTask);

        when(receiptRepository.findById(1L)).thenReturn(Optional.of(testReceipt));
        when(putawayService.generatePlacementTasks(1L)).thenReturn(tasks);
        when(receiptRepository.save(any(Receipt.class))).thenReturn(testReceipt);

        // When
        placementWorkflowService.startPlacement(1L);

        // Then
        assertEquals(ReceiptStatus.PLACING, testReceipt.getStatus());
        verify(putawayService, times(1)).generatePlacementTasks(1L);
        verify(receiptRepository, times(1)).save(testReceipt);
    }

    @Test
    void shouldThrowException_WhenStartPlacementWithNonAcceptedReceipt() {
        // Given
        testReceipt.setStatus(ReceiptStatus.DRAFT);
        when(receiptRepository.findById(1L)).thenReturn(Optional.of(testReceipt));

        // When & Then
        assertThrows(ResponseStatusException.class, 
            () -> placementWorkflowService.startPlacement(1L));
        verify(putawayService, never()).generatePlacementTasks(anyLong());
    }

    @Test
    void shouldThrowException_WhenStartPlacementWithNoTasks() {
        // Given
        when(receiptRepository.findById(1L)).thenReturn(Optional.of(testReceipt));
        when(putawayService.generatePlacementTasks(1L)).thenReturn(new ArrayList<>());

        // When & Then
        assertThrows(ResponseStatusException.class, 
            () -> placementWorkflowService.startPlacement(1L));
        verify(receiptRepository, never()).save(any(Receipt.class));
    }

    @Test
    void shouldStartPlacement_WhenCrossDockReceiptReadyForPlacement() {
        testReceipt.setCrossDock(true);
        testReceipt.setStatus(ReceiptStatus.READY_FOR_PLACEMENT);
        Task createdTask = new Task();
        createdTask.setTaskType(TaskType.PLACEMENT);

        when(receiptRepository.findById(1L)).thenReturn(Optional.of(testReceipt));
        when(putawayService.generatePlacementTasks(1L)).thenReturn(List.of(createdTask));
        when(receiptRepository.save(any(Receipt.class))).thenReturn(testReceipt);

        int created = placementWorkflowService.startPlacement(1L);

        assertEquals(1, created);
        assertEquals(ReceiptStatus.PLACING, testReceipt.getStatus());
        verify(receiptRepository, times(1)).save(testReceipt);
    }

    @Test
    void shouldRecordPlacement_WhenValidRequest() throws Exception {
        // Given
        testReceipt.setStatus(ReceiptStatus.PLACING);
        // Use reflection to set the ID on testPallet since there's no public setter
        Field idField = Pallet.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(testPallet, 1L);
        
        RecordScanRequest request = new RecordScanRequest(
            null, "PALLET001", 10, "SSCC001", "BARCODE001", "STORAGE-A-01", "DEVICE001", null,
            null, null, null, null, null
        );

        when(taskLifecycleService.getTask(1L)).thenReturn(testTask);
        when(palletRepository.findByCode("PALLET001")).thenReturn(Optional.of(testPallet));
        when(locationRepository.findById(1L)).thenReturn(Optional.of(sourceLocation));
        when(locationRepository.findById(2L)).thenReturn(Optional.of(targetLocation));
        when(palletRepository.save(any(Pallet.class))).thenReturn(testPallet);
        when(stockMovementService.recordPlacement(any(), any(), any(), any(), anyLong()))
                .thenReturn(new PalletMovement());
        when(scanRepository.save(any(Scan.class))).thenReturn(testScan);
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);

        // When
        Scan result = placementWorkflowService.recordPlacement(1L, request);

        // Then
        assertNotNull(result);
        assertEquals(PalletStatus.PLACED, testPallet.getStatus());
        assertEquals(targetLocation, testPallet.getLocation());
        assertEquals(TaskStatus.IN_PROGRESS, testTask.getStatus());
        assertNull(testTask.getClosedAt());
    }

    @Test
    void shouldThrowException_WhenRecordPlacementWithWrongTaskType() {
        // Given
        testTask.setTaskType(TaskType.RECEIVING);
        RecordScanRequest request = new RecordScanRequest(
            null, "PALLET001", 10, "SSCC001", "BARCODE001", null, "DEVICE001", null,
            null, null, null, null, null
        );

        when(taskLifecycleService.getTask(1L)).thenReturn(testTask);

        // When & Then
        assertThrows(ResponseStatusException.class, 
            () -> placementWorkflowService.recordPlacement(1L, request));
    }

    @Test
    void shouldThrowException_WhenRecordPlacementWithWrongStatus() {
        // Given
        testTask.setStatus(TaskStatus.NEW);
        RecordScanRequest request = new RecordScanRequest(
            null, "PALLET001", 10, "SSCC001", "BARCODE001", null, "DEVICE001", null,
            null, null, null, null, null
        );

        when(taskLifecycleService.getTask(1L)).thenReturn(testTask);

        // When & Then
        assertThrows(ResponseStatusException.class, 
            () -> placementWorkflowService.recordPlacement(1L, request));
    }

    @Test
    void shouldThrowException_WhenPalletNotFound() {
        // Given
        RecordScanRequest request = new RecordScanRequest(
            null, "INVALID", 10, "SSCC001", "BARCODE001", "STORAGE-A-01", "DEVICE001", null,
            null, null, null, null, null
        );

        when(taskLifecycleService.getTask(1L)).thenReturn(testTask);
        when(locationRepository.findById(2L)).thenReturn(Optional.of(targetLocation)); // Needed for validation
        when(palletRepository.findByCode("INVALID")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResponseStatusException.class, 
            () -> placementWorkflowService.recordPlacement(1L, request));
    }

    @Test
    void shouldThrowException_WhenPalletDoesNotMatchTask() {
        // Given
        testTask.setPalletId(999L);
        RecordScanRequest request = new RecordScanRequest(
            null, "PALLET001", 10, "SSCC001", "BARCODE001", "STORAGE-A-01", "DEVICE001", null,
            null, null, null, null, null
        );

        when(taskLifecycleService.getTask(1L)).thenReturn(testTask);
        when(locationRepository.findById(2L)).thenReturn(Optional.of(targetLocation)); // Needed for validation
        
        // Use reflection to set pallet ID
        try {
            Field idField = Pallet.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(testPallet, 1L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        when(palletRepository.findByCode("PALLET001")).thenReturn(Optional.of(testPallet));

        // When & Then
        assertThrows(ResponseStatusException.class, 
            () -> placementWorkflowService.recordPlacement(1L, request));
    }

    @Test
    void shouldReturnReplay_WhenSameRequestIdIsSentTwice() {
        RecordScanRequest request = new RecordScanRequest(
            "req-123", "PALLET001", 10, "SSCC001", "BARCODE001", "STORAGE-A-01", "DEVICE001", null,
            null, null, null, null, null
        );

        Scan existing = new Scan();
        existing.setRequestId("req-123");

        when(taskLifecycleService.getTask(1L)).thenReturn(testTask);
        when(locationRepository.findById(2L)).thenReturn(Optional.of(targetLocation));
        when(scanRepository.findByTaskIdAndRequestId(1L, "req-123")).thenReturn(Optional.of(existing));

        Scan result = placementWorkflowService.recordPlacement(1L, request);

        assertTrue(Boolean.TRUE.equals(result.getDuplicate()));
        assertTrue(Boolean.TRUE.equals(result.getIdempotentReplay()));
        verifyNoInteractions(palletRepository);
    }

    @Test
    void shouldCompleteManually_WhenAllTasksCompleted() {
        // Given
        testReceipt.setStatus(ReceiptStatus.PLACING);
        Task task1 = new Task();
        task1.setStatus(TaskStatus.COMPLETED);
        Task task2 = new Task();
        task2.setStatus(TaskStatus.COMPLETED);

        when(receiptRepository.findById(1L)).thenReturn(Optional.of(testReceipt));
        when(taskRepository.findByReceiptIdAndTaskType(1L, TaskType.PLACEMENT))
            .thenReturn(List.of(task1, task2));
        when(receiptRepository.save(any(Receipt.class))).thenReturn(testReceipt);

        // When
        placementWorkflowService.completePlacement(1L);

        // Then
        assertEquals(ReceiptStatus.STOCKED, testReceipt.getStatus());
        verify(receiptRepository, times(1)).save(testReceipt);
    }

    @Test
    void shouldReturnReadyForShipment_WhenCompletePlacementForCrossDock() {
        testReceipt.setCrossDock(true);
        testReceipt.setStatus(ReceiptStatus.PLACING);
        Task task = new Task();
        task.setStatus(TaskStatus.COMPLETED);

        when(receiptRepository.findById(1L)).thenReturn(Optional.of(testReceipt));
        when(taskRepository.findByReceiptIdAndTaskType(1L, TaskType.PLACEMENT)).thenReturn(List.of(task));
        when(receiptRepository.save(any(Receipt.class))).thenReturn(testReceipt);

        placementWorkflowService.completePlacement(1L);

        assertEquals(ReceiptStatus.READY_FOR_SHIPMENT, testReceipt.getStatus());
        verify(receiptRepository, times(1)).save(testReceipt);
    }

    @Test
    void shouldThrowException_WhenCompleteWithIncompleteTask() {
        // Given
        testReceipt.setStatus(ReceiptStatus.PLACING);
        Task task1 = new Task();
        task1.setStatus(TaskStatus.COMPLETED);
        Task task2 = new Task();
        task2.setStatus(TaskStatus.IN_PROGRESS);

        when(receiptRepository.findById(1L)).thenReturn(Optional.of(testReceipt));
        when(taskRepository.findByReceiptIdAndTaskType(1L, TaskType.PLACEMENT))
            .thenReturn(List.of(task1, task2));

        // When & Then
        assertThrows(ResponseStatusException.class, 
            () -> placementWorkflowService.completePlacement(1L));
    }
}
