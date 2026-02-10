package com.wmsdipl.core.service;

import com.wmsdipl.core.domain.Location;
import com.wmsdipl.core.domain.Pallet;
import com.wmsdipl.core.domain.PalletStatus;
import com.wmsdipl.core.domain.Receipt;
import com.wmsdipl.core.domain.ReceiptLine;
import com.wmsdipl.core.domain.Task;
import com.wmsdipl.core.domain.TaskStatus;
import com.wmsdipl.core.domain.TaskType;
import com.wmsdipl.core.repository.LocationRepository;
import com.wmsdipl.core.repository.PalletRepository;
import com.wmsdipl.core.repository.ReceiptRepository;
import com.wmsdipl.core.repository.TaskRepository;
import com.wmsdipl.core.service.putaway.LocationSelectionService;
import com.wmsdipl.core.service.putaway.PutawayContext;
import com.wmsdipl.core.service.putaway.PutawayContextBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PutawayServiceTest {

    @Mock
    private PalletRepository palletRepository;
    @Mock
    private LocationRepository locationRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private ReceiptRepository receiptRepository;
    @Mock
    private LocationSelectionService locationSelectionService;
    @Mock
    private PutawayContextBuilder contextBuilder;

    @InjectMocks
    private PutawayService putawayService;

    @Test
    void shouldSynchronizePalletSkuFromReceiptLineBeforeCreatingPlacementTasks() throws Exception {
        Receipt receipt = new Receipt();
        setId(receipt, 282L);

        ReceiptLine line = new ReceiptLine();
        setId(line, 387L);
        line.setSkuId(375L);
        line.setReceipt(receipt);

        Location source = new Location();
        setId(source, 1L);
        source.setCode("TEST");

        Location target = new Location();
        setId(target, 985L);
        target.setCode("B-01-03");

        Pallet pallet = new Pallet();
        setId(pallet, 1926L);
        pallet.setCode("PLT-00254");
        pallet.setReceipt(receipt);
        pallet.setReceiptLine(line);
        pallet.setSkuId(309L);
        pallet.setStatus(PalletStatus.RECEIVED);
        pallet.setLocation(source);
        pallet.setQuantity(new BigDecimal("1.000"));

        when(receiptRepository.findById(282L)).thenReturn(Optional.of(receipt));
        when(palletRepository.findByReceiptAndStatus(receipt, PalletStatus.RECEIVED)).thenReturn(List.of(pallet));
        when(palletRepository.findByReceiptAndStatus(receipt, PalletStatus.DAMAGED)).thenReturn(List.of());
        when(palletRepository.findByReceiptAndStatus(receipt, PalletStatus.QUARANTINE)).thenReturn(List.of());
        when(contextBuilder.buildContext(pallet)).thenReturn(
            new PutawayContext(receipt, null, null, null, source, null)
        );
        when(locationSelectionService.determineLocation(any(Pallet.class), any(PutawayContext.class)))
            .thenReturn(Optional.of(target));
        when(palletRepository.save(any(Pallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<Task> tasks = putawayService.generatePlacementTasks(282L);

        assertFalse(tasks.isEmpty());
        Task task = tasks.get(0);
        assertEquals(TaskType.PLACEMENT, task.getTaskType());
        assertEquals(TaskStatus.NEW, task.getStatus());
        assertEquals(new BigDecimal("1.000"), task.getQtyAssigned());
        assertEquals(1926L, task.getPalletId());
        assertEquals(985L, task.getTargetLocationId());

        assertEquals(375L, pallet.getSkuId());
        assertEquals(PalletStatus.IN_TRANSIT, pallet.getStatus());

        ArgumentCaptor<Pallet> palletCaptor = ArgumentCaptor.forClass(Pallet.class);
        verify(palletRepository, times(2)).save(palletCaptor.capture());
        List<Pallet> saved = palletCaptor.getAllValues();
        assertNotNull(saved);
        assertEquals(375L, saved.get(saved.size() - 1).getSkuId());
    }

    private void setId(Object target, Long id) throws Exception {
        Field idField = target.getClass().getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(target, id);
    }
}
