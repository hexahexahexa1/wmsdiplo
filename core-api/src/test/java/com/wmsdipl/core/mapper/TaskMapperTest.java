package com.wmsdipl.core.mapper;

import com.wmsdipl.contracts.dto.TaskDto;
import com.wmsdipl.core.domain.Location;
import com.wmsdipl.core.domain.Receipt;
import com.wmsdipl.core.domain.ReceiptLine;
import com.wmsdipl.core.domain.Task;
import com.wmsdipl.core.domain.TaskStatus;
import com.wmsdipl.core.domain.TaskType;
import com.wmsdipl.core.repository.LocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TaskMapperTest {

    private TaskMapper taskMapper;
    private LocationRepository locationRepository;

    @BeforeEach
    void setUp() {
        locationRepository = mock(LocationRepository.class);
        taskMapper = new TaskMapper(locationRepository);
    }

    @Test
    void shouldMapTaskToDto_WithAllFields() {
        // Given
        Receipt receipt = mock(Receipt.class);
        when(receipt.getId()).thenReturn(1L);
        when(receipt.getDocNo()).thenReturn("DOC001");

        ReceiptLine line = mock(ReceiptLine.class);
        when(line.getId()).thenReturn(10L);
        when(line.getSkuId()).thenReturn(100L);

        Location targetLocation = mock(Location.class);
        when(targetLocation.getCode()).thenReturn("LOC-A1");
        when(locationRepository.findById(300L)).thenReturn(Optional.of(targetLocation));

        Task task = mock(Task.class);
        when(task.getId()).thenReturn(5L);
        when(task.getTaskType()).thenReturn(TaskType.RECEIVING);
        when(task.getStatus()).thenReturn(TaskStatus.IN_PROGRESS);
        when(task.getAssignee()).thenReturn("user1");
        when(task.getAssignedBy()).thenReturn("admin");
        when(task.getPalletId()).thenReturn(50L);
        when(task.getSourceLocationId()).thenReturn(200L);
        when(task.getTargetLocationId()).thenReturn(300L);
        when(task.getReceipt()).thenReturn(receipt);
        when(task.getLine()).thenReturn(line);
        when(task.getQtyAssigned()).thenReturn(BigDecimal.TEN);
        when(task.getQtyDone()).thenReturn(BigDecimal.valueOf(5));
        when(task.getPriority()).thenReturn(1);
        when(task.getCreatedAt()).thenReturn(LocalDateTime.of(2026, 1, 1, 10, 0));
        when(task.getStartedAt()).thenReturn(LocalDateTime.of(2026, 1, 1, 11, 0));
        when(task.getClosedAt()).thenReturn(null);

        // When
        TaskDto dto = taskMapper.toDto(task);

        // Then
        assertNotNull(dto);
        assertEquals(5L, dto.id());
        assertEquals("RECEIVING", dto.taskType());
        assertEquals("IN_PROGRESS", dto.status());
        assertEquals("user1", dto.assignee());
        assertEquals("admin", dto.assignedBy());
        assertEquals(50L, dto.palletId());
        assertEquals(200L, dto.sourceLocationId());
        assertEquals(300L, dto.targetLocationId());
        assertEquals("LOC-A1", dto.targetLocationCode());
        assertEquals(1L, dto.receiptId());
        assertEquals("DOC001", dto.receiptDocNo());
        assertEquals(10L, dto.lineId());
        assertEquals(100L, dto.skuId());
        assertEquals(BigDecimal.TEN, dto.qtyAssigned());
        assertEquals(BigDecimal.valueOf(5), dto.qtyDone());
        assertEquals(1, dto.priority());
        assertEquals(LocalDateTime.of(2026, 1, 1, 10, 0), dto.createdAt());
        assertEquals(LocalDateTime.of(2026, 1, 1, 11, 0), dto.startedAt());
        assertNull(dto.closedAt());
    }

    @Test
    void shouldMapTaskToDto_WithNullReceipt() {
        // Given
        Task task = mock(Task.class);
        when(task.getId()).thenReturn(5L);
        when(task.getReceipt()).thenReturn(null);

        // When
        TaskDto dto = taskMapper.toDto(task);

        // Then
        assertNotNull(dto);
        assertNull(dto.receiptId());
        assertNull(dto.receiptDocNo());
    }

    @Test
    void shouldMapTaskToDto_WithNullLine() {
        // Given
        Task task = mock(Task.class);
        when(task.getId()).thenReturn(5L);
        when(task.getLine()).thenReturn(null);

        // When
        TaskDto dto = taskMapper.toDto(task);

        // Then
        assertNotNull(dto);
        assertNull(dto.lineId());
        assertNull(dto.skuId());
    }

    @Test
    void shouldMapTaskToDto_WithNullTaskType() {
        // Given
        Task task = mock(Task.class);
        when(task.getId()).thenReturn(5L);
        when(task.getTaskType()).thenReturn(null);

        // When
        TaskDto dto = taskMapper.toDto(task);

        // Then
        assertNotNull(dto);
        assertNull(dto.taskType());
    }

    @Test
    void shouldMapTaskToDto_WithNullStatus() {
        // Given
        Task task = mock(Task.class);
        when(task.getId()).thenReturn(5L);
        when(task.getStatus()).thenReturn(null);

        // When
        TaskDto dto = taskMapper.toDto(task);

        // Then
        assertNotNull(dto);
        assertNull(dto.status());
    }
}
