package com.wmsdipl.core.service;

import com.wmsdipl.contracts.dto.BulkAssignRequest;
import com.wmsdipl.contracts.dto.BulkCreatePalletsRequest;
import com.wmsdipl.contracts.dto.BulkOperationResult;
import com.wmsdipl.contracts.dto.PalletCreationResult;
import com.wmsdipl.core.domain.Pallet;
import com.wmsdipl.core.domain.PalletStatus;
import com.wmsdipl.core.domain.Task;
import com.wmsdipl.core.domain.TaskStatus;
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
import java.util.Optional;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.any;
import org.mockito.ArgumentCaptor;

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

        Task task101 = new Task();
        task101.setStatus(TaskStatus.NEW);
        Task task102 = new Task();
        task102.setStatus(TaskStatus.ASSIGNED);
        Task task103 = new Task();
        task103.setStatus(TaskStatus.IN_PROGRESS);

        when(taskRepository.findById(101L)).thenReturn(Optional.of(task101));
        when(taskRepository.findById(102L)).thenReturn(Optional.of(task102));
        when(taskRepository.findById(103L)).thenReturn(Optional.of(task103));
        when(taskRepository.save(task101)).thenReturn(task101);
        when(taskRepository.save(task102)).thenReturn(task102);

        // When
        BulkOperationResult<Long> result = bulkOperationsService.bulkAssignTasks(request);

        // Then
        assertEquals(List.of(101L, 102L), result.successes());
        assertEquals(1, result.failures().size());
        assertEquals(103L, result.failures().get(0).id());
        assertEquals("Task in IN_PROGRESS status cannot be reassigned", result.failures().get(0).error());
        verify(taskRepository).save(task101);
        verify(taskRepository).save(task102);
    }

    @Test
    void shouldCreateEmptyPalletsWithoutLocation_WhenBulkCreateRequested() {
        BulkCreatePalletsRequest request = new BulkCreatePalletsRequest(100, 2);
        when(palletRepository.save(any(Pallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PalletCreationResult result = bulkOperationsService.bulkCreatePallets(request);

        assertEquals(List.of("PLT-00100", "PLT-00101"), result.created());
        assertEquals(0, result.failures().size());

        ArgumentCaptor<Pallet> palletCaptor = ArgumentCaptor.forClass(Pallet.class);
        verify(palletRepository, times(2)).save(palletCaptor.capture());
        for (Pallet pallet : palletCaptor.getAllValues()) {
            assertEquals(PalletStatus.EMPTY, pallet.getStatus());
            assertEquals(BigDecimal.ZERO, pallet.getQuantity());
            assertNull(pallet.getLocation());
            assertNull(pallet.getReceipt());
            assertNull(pallet.getReceiptLine());
        }
    }
}
