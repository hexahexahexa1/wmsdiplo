package com.wmsdipl.core.service;

import com.wmsdipl.core.domain.Discrepancy;
import com.wmsdipl.core.domain.Receipt;
import com.wmsdipl.core.domain.ReceiptLine;
import com.wmsdipl.core.domain.Scan;
import com.wmsdipl.core.domain.Task;
import com.wmsdipl.core.domain.TaskStatus;
import com.wmsdipl.core.domain.TaskType;
import com.wmsdipl.core.repository.DiscrepancyRepository;
import com.wmsdipl.core.repository.ReceiptRepository;
import com.wmsdipl.core.repository.TaskRepository;
import com.wmsdipl.core.service.workflow.PlacementWorkflowService;
import com.wmsdipl.core.service.workflow.ReceivingWorkflowService;
import com.wmsdipl.core.service.workflow.ShippingWorkflowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ReceiptRepository receiptRepository;

    @Mock
    private DiscrepancyRepository discrepancyRepository;

    @Mock
    private com.wmsdipl.core.repository.ScanRepository scanRepository;

    @Mock
    private TaskLifecycleService taskLifecycleService;

    @Mock
    private TaskScanUndoService taskScanUndoService;

    @Mock
    private ReceivingWorkflowService receivingWorkflowService;

    @Mock
    private PlacementWorkflowService placementWorkflowService;

    @Mock
    private ShippingWorkflowService shippingWorkflowService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private TaskService taskService;

    private Task testTask;
    private Receipt testReceipt;
    private Discrepancy testDiscrepancy;

    @BeforeEach
    void setUp() throws Exception {
        testReceipt = new Receipt();
        testReceipt.setDocNo("DOC001");
        
        java.lang.reflect.Field idField = Receipt.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(testReceipt, 1L);

        testTask = new Task();
        testTask.setReceipt(testReceipt);
        testTask.setTaskType(TaskType.RECEIVING);
        testTask.setStatus(TaskStatus.NEW);

        testDiscrepancy = new Discrepancy();
        java.lang.reflect.Field discrepancyIdField = Discrepancy.class.getDeclaredField("id");
        discrepancyIdField.setAccessible(true);
        discrepancyIdField.set(testDiscrepancy, 1L);
        testDiscrepancy.setResolved(false);
    }

    @Test
    void shouldFindAllTasks_WhenCalled() {
        // Given
        List<Task> tasks = List.of(testTask);
        when(taskRepository.findAll()).thenReturn(tasks);

        // When
        List<Task> result = taskService.findAll();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(taskRepository, times(1)).findAll();
    }

    @Test
    void shouldFindTasksByReceipt_WhenValidReceiptId() {
        // Given
        List<Task> tasks = List.of(testTask);
        when(taskRepository.findByReceiptId(1L)).thenReturn(tasks);

        // When
        List<Task> result = taskService.findByReceipt(1L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(taskRepository, times(1)).findByReceiptId(1L);
    }

    @Test
    void shouldGetTask_WhenValidId() {
        // Given
        when(taskLifecycleService.getTask(1L)).thenReturn(testTask);

        // When
        Task result = taskService.get(1L);

        // Then
        assertNotNull(result);
        assertEquals(TaskType.RECEIVING, result.getTaskType());
        verify(taskLifecycleService, times(1)).getTask(1L);
    }

    @Test
    void shouldThrowException_WhenTaskNotFound() {
        // Given
        when(taskLifecycleService.getTask(999L)).thenThrow(new IllegalArgumentException("Task not found: 999"));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> taskService.get(999L));
        verify(taskLifecycleService, times(1)).getTask(999L);
    }

    @Test
    void shouldCreateTask_WhenValidTask() {
        // Given
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);

        // When
        Task result = taskService.create(testTask);

        // Then
        assertNotNull(result);
        verify(taskRepository, times(1)).save(testTask);
    }

    @Test
    void shouldAssignTask_WhenValidId() {
        // Given
        when(taskLifecycleService.assign(1L, "user1", "admin")).thenReturn(testTask);
        testTask.setAssignee("user1");
        testTask.setAssignedBy("admin");
        testTask.setStatus(TaskStatus.ASSIGNED);

        // When
        Task result = taskService.assign(1L, "user1", "admin");

        // Then
        assertNotNull(result);
        assertEquals("user1", result.getAssignee());
        assertEquals("admin", result.getAssignedBy());
        assertEquals(TaskStatus.ASSIGNED, result.getStatus());
        verify(taskLifecycleService, times(1)).assign(1L, "user1", "admin");
    }

    @Test
    void shouldStartTask_WhenValidId() {
        // Given
        when(taskLifecycleService.start(1L)).thenReturn(testTask);
        testTask.setStatus(TaskStatus.IN_PROGRESS);
        testTask.setStartedAt(LocalDateTime.now());

        // When
        Task result = taskService.start(1L);

        // Then
        assertNotNull(result);
        assertEquals(TaskStatus.IN_PROGRESS, result.getStatus());
        assertNotNull(result.getStartedAt());
        verify(taskLifecycleService, times(1)).start(1L);
    }

    @Test
    void shouldCompleteTask_WhenValidId() {
        // Given
        when(taskLifecycleService.complete(1L)).thenReturn(testTask);
        testTask.setStatus(TaskStatus.COMPLETED);
        testTask.setClosedAt(LocalDateTime.now());

        // When
        Task result = taskService.complete(1L);

        // Then
        assertNotNull(result);
        assertEquals(TaskStatus.COMPLETED, result.getStatus());
        assertNotNull(result.getClosedAt());
        verify(taskLifecycleService, times(1)).complete(1L);
        verify(receivingWorkflowService, times(1)).checkAndCompleteReceipt(1L);
    }

    @Test
    void shouldCompleteTask_WhenReceivingReceiptAutoTransitionBlocked() {
        when(taskLifecycleService.complete(1L)).thenReturn(testTask);
        testTask.setStatus(TaskStatus.COMPLETED);
        testTask.setClosedAt(LocalDateTime.now());

        doThrow(new ReceiptWorkflowBlockedException(
            1L,
            "completeReceiving",
            List.of()
        )).when(receivingWorkflowService).checkAndCompleteReceipt(1L);

        Task result = taskService.complete(1L);

        assertNotNull(result);
        assertEquals(TaskStatus.COMPLETED, result.getStatus());
        verify(taskLifecycleService).complete(1L);
        verify(receivingWorkflowService).checkAndCompleteReceipt(1L);
    }

    @Test
    void shouldCancelTask_WhenValidId() {
        // Given
        when(taskLifecycleService.cancel(1L)).thenReturn(testTask);
        testTask.setStatus(TaskStatus.CANCELLED);
        testTask.setClosedAt(LocalDateTime.now());

        // When
        Task result = taskService.cancel(1L);

        // Then
        assertNotNull(result);
        assertEquals(TaskStatus.CANCELLED, result.getStatus());
        assertNotNull(result.getClosedAt());
        verify(taskLifecycleService, times(1)).cancel(1L);
    }

    @Test
    void shouldReleaseTaskWithScans_WhenRollbackRequired() {
        Task task = new Task();
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setQtyDone(new BigDecimal("5.000"));
        task.setAssignee("operator1");
        java.lang.reflect.Field taskIdField;
        try {
            taskIdField = Task.class.getDeclaredField("id");
            taskIdField.setAccessible(true);
            taskIdField.set(task, 1L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        when(taskLifecycleService.getTask(1L)).thenReturn(task);
        when(scanRepository.findFirstByTaskOrderByScannedAtDescIdDesc(task))
            .thenReturn(Optional.of(new Scan()), Optional.empty());
        when(taskRepository.save(task)).thenReturn(task);

        Task released = taskService.release(1L);

        assertEquals(TaskStatus.NEW, released.getStatus());
        assertNull(released.getAssignee());
        assertEquals(BigDecimal.ZERO, released.getQtyDone());
        verify(taskScanUndoService).undoLastScan(1L, "system");
        verify(scanRepository, never()).deleteByTask(any());
    }

    @Test
    void shouldReleaseTaskWithoutScans_WhenRollbackNotRequired() {
        Task task = new Task();
        task.setStatus(TaskStatus.ASSIGNED);
        task.setQtyDone(new BigDecimal("2.000"));
        java.lang.reflect.Field taskIdField;
        try {
            taskIdField = Task.class.getDeclaredField("id");
            taskIdField.setAccessible(true);
            taskIdField.set(task, 2L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        when(taskLifecycleService.getTask(2L)).thenReturn(task);
        when(scanRepository.findFirstByTaskOrderByScannedAtDescIdDesc(task))
            .thenReturn(Optional.empty());
        when(taskRepository.save(task)).thenReturn(task);

        Task released = taskService.release(2L);

        assertEquals(TaskStatus.NEW, released.getStatus());
        assertEquals(BigDecimal.ZERO, released.getQtyDone());
        verify(taskScanUndoService, never()).undoLastScan(anyLong(), anyString());
    }

    @Test
    void shouldFindOpenDiscrepancies_WhenCalled() {
        // Given
        List<Discrepancy> discrepancies = List.of(testDiscrepancy);
        when(discrepancyRepository.findByResolvedFalse()).thenReturn(discrepancies);

        // When
        List<Discrepancy> result = taskService.findOpenDiscrepancies();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertFalse(result.get(0).getResolved());
        verify(discrepancyRepository, times(1)).findByResolvedFalse();
    }

    @Test
    void shouldResolveDiscrepancy_WhenValidId() {
        // Given
        when(discrepancyRepository.findById(1L)).thenReturn(Optional.of(testDiscrepancy));
        when(discrepancyRepository.save(any(Discrepancy.class))).thenReturn(testDiscrepancy);

        // When
        Discrepancy result = taskService.resolveDiscrepancy(1L, "Resolved by admin");

        // Then
        assertNotNull(result);
        assertTrue(result.getResolved());
        assertEquals("Resolved by admin", result.getDescription());
        verify(discrepancyRepository, times(1)).save(any(Discrepancy.class));
    }

    @Test
    void shouldThrowException_WhenDiscrepancyNotFound() {
        // Given
        when(discrepancyRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalArgumentException.class, 
            () -> taskService.resolveDiscrepancy(999L, "Comment"));
        verify(discrepancyRepository, never()).save(any(Discrepancy.class));
    }

    @Test
    void shouldFilterDiscrepancyJournal_ByOperator() throws Exception {
        Discrepancy discrepancy = new Discrepancy();
        discrepancy.setTaskId(55L);
        discrepancy.setCreatedAt(LocalDateTime.now());

        Task task = new Task();
        java.lang.reflect.Field idField = Task.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(task, 55L);
        task.setAssignee("operator1");

        when(discrepancyRepository.findAll()).thenReturn(List.of(discrepancy));
        when(taskRepository.findAllById(any())).thenReturn(List.of(task));

        List<Discrepancy> result = taskService.findDiscrepancyJournal(
            null,
            null,
            null,
            null,
            null,
            null,
            "operator1",
            null
        );

        assertEquals(1, result.size());
    }

    @Test
    void shouldFilterDiscrepancyJournal_ByResolvedBy_WhenTaskAssigneeUnavailable() {
        Discrepancy discrepancy = new Discrepancy();
        discrepancy.setTaskId(null);
        discrepancy.setResolvedBy("operator2");
        discrepancy.setCreatedAt(LocalDateTime.now());

        when(discrepancyRepository.findAll()).thenReturn(List.of(discrepancy));

        List<Discrepancy> result = taskService.findDiscrepancyJournal(
            null,
            null,
            null,
            null,
            null,
            null,
            "operator2",
            null
        );

        assertEquals(1, result.size());
        verify(taskRepository, never()).findAllById(any());
    }

    @Test
    void shouldReturnSafeEmptyAssigneeMap_WhenDiscrepanciesHaveNoTaskIds() {
        Discrepancy discrepancy = new Discrepancy();
        discrepancy.setTaskId(null);

        Map<Long, String> result = taskService.findTaskAssigneesByDiscrepancies(List.of(discrepancy));

        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertDoesNotThrow(() -> result.get(null));
        verify(taskRepository, never()).findAllById(any());
    }

    @Test
    void shouldUpdateDiscrepancyComment() {
        when(discrepancyRepository.findById(1L)).thenReturn(Optional.of(testDiscrepancy));
        when(discrepancyRepository.save(any(Discrepancy.class))).thenReturn(testDiscrepancy);

        Discrepancy updated = taskService.updateDiscrepancyComment(1L, "new comment");

        assertEquals("new comment", updated.getDescription());
        verify(discrepancyRepository).save(testDiscrepancy);
        verify(auditLogService).logUpdate(
            eq("DISCREPANCY"),
            eq(1L),
            eq("system"),
            eq("comment"),
            eq(null),
            eq("new comment")
        );
    }

    @Test
    void shouldCreateReceivingTasks_WhenValidReceiptAndCount() {
        // Given
        ReceiptLine line1 = new ReceiptLine();
        line1.setLineNo(1);
        line1.setSkuId(100L); // Important: SKU must be set
        testReceipt.addLine(line1);
        
        when(receiptRepository.findById(1L)).thenReturn(Optional.of(testReceipt));
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);

        // When
        taskService.createReceivingTasks(1L, TaskType.RECEIVING, 3);

        // Then
        verify(receiptRepository, times(1)).findById(1L);
        verify(taskRepository, times(3)).save(any(Task.class));
    }

    @Test
    void shouldThrowException_WhenReceiptNotFoundForTaskCreation() {
        // Given
        when(receiptRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalArgumentException.class, 
            () -> taskService.createReceivingTasks(999L, TaskType.RECEIVING, 1));
        verify(taskRepository, never()).save(any(Task.class));
    }
}
