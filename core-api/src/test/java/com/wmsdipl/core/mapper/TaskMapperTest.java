package com.wmsdipl.core.mapper;

import com.wmsdipl.contracts.dto.TaskDto;
import com.wmsdipl.core.domain.Location;
import com.wmsdipl.core.domain.Pallet;
import com.wmsdipl.core.domain.Receipt;
import com.wmsdipl.core.domain.ReceiptLine;
import com.wmsdipl.core.domain.Sku;
import com.wmsdipl.core.domain.SkuUnitConfig;
import com.wmsdipl.core.domain.Task;
import com.wmsdipl.core.domain.TaskStatus;
import com.wmsdipl.core.domain.TaskType;
import com.wmsdipl.core.repository.LocationRepository;
import com.wmsdipl.core.repository.PalletRepository;
import com.wmsdipl.core.repository.SkuRepository;
import com.wmsdipl.core.repository.SkuUnitConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TaskMapperTest {

    private TaskMapper taskMapper;
    private LocationRepository locationRepository;
    private SkuRepository skuRepository;
    private PalletRepository palletRepository;
    private SkuUnitConfigRepository skuUnitConfigRepository;

    @BeforeEach
    void setUp() {
        locationRepository = mock(LocationRepository.class);
        skuRepository = mock(SkuRepository.class);
        palletRepository = mock(PalletRepository.class);
        skuUnitConfigRepository = mock(SkuUnitConfigRepository.class);
        when(skuUnitConfigRepository.findBySkuIdInAndIsBaseTrueAndActiveTrue(anyCollection())).thenReturn(List.of());
        taskMapper = new TaskMapper(locationRepository, skuRepository, palletRepository, skuUnitConfigRepository);
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
        when(line.getUom()).thenReturn("BOX");
        when(line.getUnitFactorToBase()).thenReturn(BigDecimal.valueOf(12));

        Location targetLocation = mock(Location.class);
        when(targetLocation.getId()).thenReturn(300L);
        when(targetLocation.getCode()).thenReturn("LOC-A1");
        when(locationRepository.findById(300L)).thenReturn(Optional.of(targetLocation));

        Sku sku = mock(Sku.class);
        when(sku.getId()).thenReturn(100L);
        when(sku.getCode()).thenReturn("SKU-100");
        when(skuRepository.findAllById(any())).thenReturn(List.of(sku));
        SkuUnitConfig baseConfig = mock(SkuUnitConfig.class);
        when(baseConfig.getSkuId()).thenReturn(100L);
        when(baseConfig.getUnitCode()).thenReturn("PCS");
        when(skuUnitConfigRepository.findBySkuIdInAndIsBaseTrueAndActiveTrue(anyCollection())).thenReturn(List.of(baseConfig));

        Pallet pallet = mock(Pallet.class);
        when(pallet.getId()).thenReturn(50L);
        when(pallet.getCode()).thenReturn("PALLET-050");
        when(palletRepository.findById(50L)).thenReturn(Optional.of(pallet));

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
        assertEquals("SKU-100", dto.skuCode());
        assertEquals("PALLET-050", dto.palletCode());
        assertEquals(BigDecimal.TEN, dto.qtyAssigned());
        assertEquals(BigDecimal.valueOf(5), dto.qtyDone());
        assertEquals("BOX", dto.lineUom());
        assertEquals("PCS", dto.baseUom());
        assertEquals(BigDecimal.valueOf(12), dto.unitFactorToBase());
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
