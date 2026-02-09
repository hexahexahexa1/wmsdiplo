package com.wmsdipl.core.service;

import com.wmsdipl.core.domain.Discrepancy;
import com.wmsdipl.core.domain.Receipt;
import com.wmsdipl.core.domain.ReceiptLine;
import com.wmsdipl.core.domain.Scan;
import com.wmsdipl.core.domain.Task;
import com.wmsdipl.core.domain.TaskStatus;
import com.wmsdipl.core.domain.TaskType;
import org.springframework.web.server.ResponseStatusException;
import com.wmsdipl.core.repository.DiscrepancyRepository;
import com.wmsdipl.core.repository.ScanRepository;
import com.wmsdipl.core.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.withSettings;

@ExtendWith(MockitoExtension.class)
class TaskLifecycleServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ScanRepository scanRepository;

    @Mock
    private DiscrepancyRepository discrepancyRepository;

    @InjectMocks
    private TaskLifecycleService taskLifecycleService;

    private Task testTask;

    @BeforeEach
    void setUp() {
        testTask = mock(Task.class, withSettings().lenient());
        lenient().when(testTask.getId()).thenReturn(1L);
        lenient().when(testTask.getStatus()).thenReturn(TaskStatus.NEW);
        lenient().when(testTask.getQtyAssigned()).thenReturn(BigDecimal.TEN);
        lenient().when(testTask.getQtyDone()).thenReturn(BigDecimal.ZERO);
    }

    @Test
    void shouldGetTask_WhenValidId() {
        // Given
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));

        // When
        Task result = taskLifecycleService.getTask(1L);

        // Then
        assertNotNull(result);
        verify(taskRepository, times(1)).findById(1L);
    }

    @Test
    void shouldThrowException_WhenTaskNotFound() {
        // Given
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> taskLifecycleService.getTask(999L));
        assertEquals("Task not found: 999", exception.getMessage());
    }

    @Test
    void shouldAssignTask_WhenValidParameters() {
        // Given
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);

        // When
        Task result = taskLifecycleService.assign(1L, "user1", "admin");

        // Then
        assertNotNull(result);
        verify(testTask, times(1)).setAssignee("user1");
        verify(testTask, times(1)).setAssignedBy("admin");
        verify(testTask, times(1)).setStatus(TaskStatus.ASSIGNED);
        verify(taskRepository, times(1)).save(testTask);
    }

    @Test
    void shouldReassignTask_WhenCurrentStatusAssigned() {
        // Given
        when(testTask.getStatus()).thenReturn(TaskStatus.ASSIGNED);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);

        // When
        Task result = taskLifecycleService.assign(1L, "operator2", "supervisor1");

        // Then
        assertNotNull(result);
        verify(testTask, times(1)).setAssignee("operator2");
        verify(testTask, times(1)).setAssignedBy("supervisor1");
        verify(testTask, never()).setStatus(TaskStatus.ASSIGNED);
        verify(taskRepository, times(1)).save(testTask);
    }

    @Test
    void shouldStartTask_WhenValidId() {
        // Given
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);

        // When
        Task result = taskLifecycleService.start(1L);

        // Then
        assertNotNull(result);
        verify(testTask, times(1)).setStatus(TaskStatus.IN_PROGRESS);
        verify(testTask, times(1)).setStartedAt(any());
        verify(taskRepository, times(1)).save(testTask);
    }

    @Test
    void shouldCompleteTask_WhenQtyMatches() {
        // Given
        when(testTask.getQtyDone()).thenReturn(BigDecimal.TEN);
        when(testTask.getStatus()).thenReturn(TaskStatus.IN_PROGRESS);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);

        // When
        Task result = taskLifecycleService.complete(1L);

        // Then
        assertNotNull(result);
        verify(testTask, times(1)).setStatus(TaskStatus.COMPLETED);
        verify(testTask, times(1)).setClosedAt(any());
        verify(discrepancyRepository, never()).save(any(Discrepancy.class));
        verify(taskRepository, times(1)).save(testTask);
    }

    @Test
    void shouldCompleteTask_AndCreateDiscrepancy_WhenUnderQty() {
        // Given
        Receipt receipt = mock(Receipt.class);
        ReceiptLine line = mock(ReceiptLine.class);
        
        when(testTask.getQtyDone()).thenReturn(BigDecimal.valueOf(8));
        when(testTask.getTaskType()).thenReturn(TaskType.RECEIVING);
        when(testTask.getStatus()).thenReturn(TaskStatus.IN_PROGRESS);
        when(testTask.getReceipt()).thenReturn(receipt);
        when(testTask.getLine()).thenReturn(line);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);
        
        Scan scan = mock(Scan.class);
        when(scanRepository.findByTask(testTask)).thenReturn(List.of(scan));
        when(scanRepository.save(any(Scan.class))).thenReturn(scan);

        // When
        Task result = taskLifecycleService.complete(1L);

        // Then
        verify(testTask, times(1)).setStatus(TaskStatus.COMPLETED);
        
        ArgumentCaptor<Discrepancy> discrepancyCaptor = ArgumentCaptor.forClass(Discrepancy.class);
        verify(discrepancyRepository, times(1)).save(discrepancyCaptor.capture());
        
        Discrepancy savedDiscrepancy = discrepancyCaptor.getValue();
        assertEquals("UNDER_QTY", savedDiscrepancy.getType());
        assertEquals(1L, savedDiscrepancy.getTaskId());
        assertEquals(BigDecimal.TEN, savedDiscrepancy.getQtyExpected());
        assertEquals(BigDecimal.valueOf(8), savedDiscrepancy.getQtyActual());
        assertTrue(savedDiscrepancy.getResolved());
        assertEquals("system", savedDiscrepancy.getResolvedBy());
        
        verify(scanRepository, times(1)).save(scan);
        verify(scan, times(1)).setDiscrepancy(true);
    }

    @Test
    void shouldCancelTask_WhenValidId() {
        // Given
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);

        // When
        Task result = taskLifecycleService.cancel(1L);

        // Then
        assertNotNull(result);
        verify(testTask, times(1)).setStatus(TaskStatus.CANCELLED);
        verify(testTask, times(1)).setClosedAt(any());
        verify(taskRepository, times(1)).save(testTask);
    }

    @Test
    void shouldAutoStartTask_WhenStatusIsNew() {
        // Given
        when(testTask.getStatus()).thenReturn(TaskStatus.NEW);
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);

        // When
        taskLifecycleService.autoStartIfNeeded(testTask);

        // Then
        verify(testTask, times(1)).setStatus(TaskStatus.IN_PROGRESS);
        verify(testTask, times(1)).setStartedAt(any());
        verify(taskRepository, times(1)).save(testTask);
    }

    @Test
    void shouldAutoStartTask_WhenStatusIsAssigned() {
        // Given
        when(testTask.getStatus()).thenReturn(TaskStatus.ASSIGNED);
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);

        // When
        taskLifecycleService.autoStartIfNeeded(testTask);

        // Then
        verify(testTask, times(1)).setStatus(TaskStatus.IN_PROGRESS);
        verify(testTask, times(1)).setStartedAt(any());
        verify(taskRepository, times(1)).save(testTask);
    }

    @Test
    void shouldThrowException_WhenAssignFromNonNewStatus() {
        when(testTask.getStatus()).thenReturn(TaskStatus.IN_PROGRESS);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));

        assertThrows(IllegalStateException.class, () -> taskLifecycleService.assign(1L, "user1", "admin"));
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void shouldThrowException_WhenStartFromCompletedStatus() {
        when(testTask.getStatus()).thenReturn(TaskStatus.COMPLETED);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));

        assertThrows(IllegalStateException.class, () -> taskLifecycleService.start(1L));
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void shouldThrowException_WhenCancelCompletedTask() {
        when(testTask.getStatus()).thenReturn(TaskStatus.COMPLETED);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));

        assertThrows(IllegalStateException.class, () -> taskLifecycleService.cancel(1L));
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void shouldNotAutoStartTask_WhenStatusIsCompleted() {
        // Given
        when(testTask.getStatus()).thenReturn(TaskStatus.COMPLETED);

        // When
        taskLifecycleService.autoStartIfNeeded(testTask);

        // Then
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void shouldThrowException_WhenCompletingPlacementTaskManually() {
        // Given
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        when(testTask.getStatus()).thenReturn(TaskStatus.IN_PROGRESS);
        when(testTask.getTaskType()).thenReturn(TaskType.PLACEMENT);

        // When & Then
        assertThrows(ResponseStatusException.class, () -> taskLifecycleService.complete(1L));
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void shouldCompleteReceivingTaskManually() {
        // Given
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        when(testTask.getStatus()).thenReturn(TaskStatus.IN_PROGRESS);
        when(testTask.getTaskType()).thenReturn(TaskType.RECEIVING);
        when(testTask.getQtyDone()).thenReturn(BigDecimal.TEN);
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);

        // When
        Task result = taskLifecycleService.complete(1L);

        // Then
        assertNotNull(result);
        verify(testTask, times(1)).setStatus(TaskStatus.COMPLETED);
        verify(taskRepository, times(1)).save(testTask);
    }
}
