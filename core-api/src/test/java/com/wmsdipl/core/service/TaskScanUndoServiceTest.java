package com.wmsdipl.core.service;

import com.wmsdipl.contracts.dto.UndoLastScanResultDto;
import com.wmsdipl.core.domain.Location;
import com.wmsdipl.core.domain.Pallet;
import com.wmsdipl.core.domain.PalletMovement;
import com.wmsdipl.core.domain.PalletStatus;
import com.wmsdipl.core.domain.Scan;
import com.wmsdipl.core.domain.Task;
import com.wmsdipl.core.domain.TaskStatus;
import com.wmsdipl.core.domain.TaskType;
import com.wmsdipl.core.repository.DiscrepancyRepository;
import com.wmsdipl.core.repository.LocationRepository;
import com.wmsdipl.core.repository.PalletMovementRepository;
import com.wmsdipl.core.repository.PalletRepository;
import com.wmsdipl.core.repository.ScanRepository;
import com.wmsdipl.core.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskScanUndoServiceTest {

    @Mock
    private TaskLifecycleService taskLifecycleService;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private ScanRepository scanRepository;
    @Mock
    private PalletRepository palletRepository;
    @Mock
    private PalletMovementRepository palletMovementRepository;
    @Mock
    private DiscrepancyRepository discrepancyRepository;
    @Mock
    private LocationRepository locationRepository;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private TaskScanUndoService taskScanUndoService;

    @Test
    void shouldUndoLastScanForReceiving() {
        Task task = task(1L, TaskType.RECEIVING, TaskStatus.IN_PROGRESS, new BigDecimal("10.000"));
        Scan scan = scan(10L, task, "PALLET-1", new BigDecimal("3.000"));

        Pallet pallet = new Pallet();
        pallet.setId(101L);
        pallet.setCode("PALLET-1");
        pallet.setQuantity(new BigDecimal("3.000"));
        pallet.setStatus(PalletStatus.RECEIVING);
        pallet.setSkuId(500L);

        PalletMovement movement = new PalletMovement();
        setMovementId(movement, 201L);
        movement.setScanId(10L);

        when(taskLifecycleService.getTask(1L)).thenReturn(task);
        when(scanRepository.findFirstByTaskOrderByScannedAtDescIdDesc(task)).thenReturn(Optional.of(scan));
        when(palletRepository.findByCode("PALLET-1")).thenReturn(Optional.of(pallet));
        when(palletMovementRepository.findByScanId(10L)).thenReturn(Optional.of(movement));
        when(discrepancyRepository.deleteByScanId(10L)).thenReturn(1L);

        UndoLastScanResultDto result = taskScanUndoService.undoLastScan(1L, "operator1");

        assertEquals(0, task.getQtyDone().compareTo(new BigDecimal("7.000")));
        assertEquals(PalletStatus.EMPTY, pallet.getStatus());
        assertEquals(0, pallet.getQuantity().compareTo(BigDecimal.ZERO));
        assertNull(pallet.getSkuId());
        assertNull(pallet.getLocation());
        assertEquals(1, result.discrepanciesRolledBack());
        verify(palletMovementRepository).delete(movement);
        verify(scanRepository).delete(scan);
        verify(taskRepository).save(task);
    }

    @Test
    void shouldUndoLastScanForPlacement() {
        Task task = task(2L, TaskType.PLACEMENT, TaskStatus.IN_PROGRESS, new BigDecimal("1.000"));
        task.setSourceLocationId(901L);

        Scan scan = scan(20L, task, "PALLET-2", new BigDecimal("1.000"));

        Location source = new Location();
        source.setId(901L);
        source.setCode("RCV-01");
        Location target = new Location();
        target.setId(902L);
        target.setCode("A-01-01");

        Pallet pallet = new Pallet();
        pallet.setId(102L);
        pallet.setCode("PALLET-2");
        pallet.setQuantity(new BigDecimal("15.000"));
        pallet.setStatus(PalletStatus.PLACED);
        pallet.setLocation(target);

        PalletMovement movement = new PalletMovement();
        setMovementId(movement, 202L);
        movement.setScanId(20L);
        movement.setFromLocation(source);
        movement.setToLocation(target);

        when(taskLifecycleService.getTask(2L)).thenReturn(task);
        when(scanRepository.findFirstByTaskOrderByScannedAtDescIdDesc(task)).thenReturn(Optional.of(scan));
        when(palletRepository.findByCode("PALLET-2")).thenReturn(Optional.of(pallet));
        when(palletMovementRepository.findByScanId(20L)).thenReturn(Optional.of(movement));
        when(discrepancyRepository.deleteByScanId(20L)).thenReturn(0L);

        taskScanUndoService.undoLastScan(2L, "operator1");

        assertEquals(0, task.getQtyDone().compareTo(BigDecimal.ZERO));
        assertEquals(PalletStatus.RECEIVED, pallet.getStatus());
        assertEquals("RCV-01", pallet.getLocation().getCode());
        verify(palletMovementRepository).delete(movement);
        verify(scanRepository).delete(scan);
    }

    @Test
    void shouldUndoLastScanForShipping() {
        Task task = task(3L, TaskType.SHIPPING, TaskStatus.IN_PROGRESS, new BigDecimal("5.000"));
        task.setSourceLocationId(903L);

        Scan scan = scan(30L, task, "PALLET-3", new BigDecimal("2.000"));

        Location source = new Location();
        source.setId(903L);
        source.setCode("CROSS-01");

        Pallet pallet = new Pallet();
        pallet.setId(103L);
        pallet.setCode("PALLET-3");
        pallet.setQuantity(new BigDecimal("3.000"));
        pallet.setStatus(PalletStatus.SHIPPED);
        pallet.setLocation(null);

        PalletMovement movement = new PalletMovement();
        setMovementId(movement, 203L);
        movement.setScanId(30L);
        movement.setFromLocation(source);

        when(taskLifecycleService.getTask(3L)).thenReturn(task);
        when(scanRepository.findFirstByTaskOrderByScannedAtDescIdDesc(task)).thenReturn(Optional.of(scan));
        when(palletRepository.findByCode("PALLET-3")).thenReturn(Optional.of(pallet));
        when(palletMovementRepository.findByScanId(30L)).thenReturn(Optional.of(movement));
        when(discrepancyRepository.deleteByScanId(30L)).thenReturn(0L);

        taskScanUndoService.undoLastScan(3L, "operator1");

        assertEquals(0, task.getQtyDone().compareTo(new BigDecimal("3.000")));
        assertEquals(0, pallet.getQuantity().compareTo(new BigDecimal("5.000")));
        assertEquals(PalletStatus.PLACED, pallet.getStatus());
        assertEquals("CROSS-01", pallet.getLocation().getCode());
        verify(palletMovementRepository).delete(movement);
    }

    @Test
    void shouldRejectUndoForCompletedTask() {
        Task task = task(4L, TaskType.RECEIVING, TaskStatus.COMPLETED, new BigDecimal("1.000"));
        when(taskLifecycleService.getTask(4L)).thenReturn(task);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> taskScanUndoService.undoLastScan(4L, "operator1"));

        assertEquals(409, ex.getStatusCode().value());
        verify(scanRepository, never()).findFirstByTaskOrderByScannedAtDescIdDesc(any());
    }

    @Test
    void shouldReturnConflictWhenNoScansToUndo() {
        Task task = task(5L, TaskType.RECEIVING, TaskStatus.IN_PROGRESS, new BigDecimal("1.000"));
        when(taskLifecycleService.getTask(5L)).thenReturn(task);
        when(scanRepository.findFirstByTaskOrderByScannedAtDescIdDesc(task)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> taskScanUndoService.undoLastScan(5L, "operator1"));

        assertEquals(409, ex.getStatusCode().value());
    }

    private Task task(Long id, TaskType type, TaskStatus status, BigDecimal qtyDone) {
        Task task = new Task();
        task.setId(id);
        task.setTaskType(type);
        task.setStatus(status);
        task.setQtyDone(qtyDone);
        return task;
    }

    private Scan scan(Long id, Task task, String palletCode, BigDecimal qty) {
        Scan scan = new Scan();
        scan.setTask(task);
        scan.setPalletCode(palletCode);
        scan.setQty(qty);
        try {
            java.lang.reflect.Field idField = Scan.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(scan, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return scan;
    }

    private void setMovementId(PalletMovement movement, Long id) {
        try {
            java.lang.reflect.Field idField = PalletMovement.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(movement, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
