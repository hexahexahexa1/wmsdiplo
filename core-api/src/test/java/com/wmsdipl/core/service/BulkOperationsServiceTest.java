package com.wmsdipl.core.service;

import com.wmsdipl.contracts.dto.BulkAssignRequest;
import com.wmsdipl.contracts.dto.BulkOperationResult;
import com.wmsdipl.core.domain.Task;
import com.wmsdipl.core.repository.LocationRepository;
import com.wmsdipl.core.repository.PalletRepository;
import com.wmsdipl.core.repository.ReceiptRepository;
import com.wmsdipl.core.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BulkOperationsServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private PalletRepository palletRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private ReceiptRepository receiptRepository;

    @Mock
    private TaskLifecycleService taskLifecycleService;

    @InjectMocks
    private BulkOperationsService bulkOperationsService;

    @Test
    void shouldReturnPartialSuccess_WhenReassignmentContainsInProgressTask() {
        // Given
        BulkAssignRequest request = new BulkAssignRequest(List.of(101L, 102L, 103L), "operator2");
        when(taskLifecycleService.assign(101L, "operator2", "system")).thenReturn(new Task());
        when(taskLifecycleService.assign(102L, "operator2", "system")).thenReturn(new Task());
        doThrow(new IllegalStateException("Task in IN_PROGRESS status cannot be reassigned"))
            .when(taskLifecycleService).assign(103L, "operator2", "system");

        // When
        BulkOperationResult<Long> result = bulkOperationsService.bulkAssignTasks(request);

        // Then
        assertEquals(List.of(101L, 102L), result.successes());
        assertEquals(1, result.failures().size());
        assertEquals(103L, result.failures().get(0).id());
        assertEquals("Task in IN_PROGRESS status cannot be reassigned", result.failures().get(0).error());
        verify(taskLifecycleService).assign(101L, "operator2", "system");
        verify(taskLifecycleService).assign(102L, "operator2", "system");
        verify(taskLifecycleService).assign(103L, "operator2", "system");
    }
}
