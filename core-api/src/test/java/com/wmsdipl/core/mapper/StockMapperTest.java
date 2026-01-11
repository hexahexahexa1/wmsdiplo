package com.wmsdipl.core.mapper;

import com.wmsdipl.contracts.dto.StockItemDto;
import com.wmsdipl.contracts.dto.StockMovementDto;
import com.wmsdipl.core.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class StockMapperTest {

    private StockMapper stockMapper;

    @BeforeEach
    void setUp() {
        stockMapper = new StockMapper();
    }

    @Test
    void shouldMapPalletToStockItemDto_WhenAllFieldsPresent() {
        // Given
        Sku sku = new Sku();
        sku.setId(100L);
        sku.setCode("SKU001");
        sku.setName("Test Product");

        Location location = new Location();
        location.setId(200L);
        location.setCode("LOC-A-01");

        Receipt receipt = new Receipt();
        receipt.setId(300L);
        receipt.setDocNo("RCP001");
        receipt.setDocDate(LocalDate.of(2026, 1, 10));

        Pallet pallet = new Pallet();
        pallet.setId(1L);
        pallet.setCode("PLT001");
        pallet.setStatus(PalletStatus.PLACED);
        pallet.setSkuId(100L);
        pallet.setQuantity(new BigDecimal("100.00"));
        pallet.setUom("PCS");
        pallet.setLocation(location);
        pallet.setReceipt(receipt);
        pallet.setLotNumber("LOT123");
        pallet.setExpiryDate(LocalDate.of(2027, 1, 10));
        pallet.setCreatedAt(LocalDateTime.of(2026, 1, 10, 10, 0));
        pallet.setUpdatedAt(LocalDateTime.of(2026, 1, 10, 11, 0));

        // When
        StockItemDto dto = stockMapper.toStockItemDto(pallet, sku);

        // Then
        assertNotNull(dto);
        assertEquals(1L, dto.palletId());
        assertEquals("PLT001", dto.palletCode());
        assertEquals("PLACED", dto.palletStatus());
        assertEquals(100L, dto.skuId());
        assertEquals("SKU001", dto.skuCode());
        assertEquals("Test Product", dto.skuName());
        assertEquals(new BigDecimal("100.00"), dto.quantity());
        assertEquals("PCS", dto.uom());
        assertEquals(200L, dto.locationId());
        assertEquals("LOC-A-01", dto.locationCode());
        assertEquals(300L, dto.receiptId());
        assertEquals("RCP001", dto.receiptDocNumber());
        assertEquals(LocalDateTime.of(2026, 1, 10, 0, 0), dto.receiptDate());
        assertEquals("LOT123", dto.lotNumber());
        assertEquals(LocalDate.of(2027, 1, 10), dto.expiryDate());
        assertEquals(LocalDateTime.of(2026, 1, 10, 10, 0), dto.createdAt());
        assertEquals(LocalDateTime.of(2026, 1, 10, 11, 0), dto.updatedAt());
    }

    @Test
    void shouldMapPalletToStockItemDto_WhenSkuIsNull() {
        // Given
        Pallet pallet = new Pallet();
        pallet.setId(1L);
        pallet.setCode("PLT001");
        pallet.setSkuId(100L);
        pallet.setQuantity(new BigDecimal("50.00"));
        pallet.setUom("PCS");

        // When
        StockItemDto dto = stockMapper.toStockItemDto(pallet, null);

        // Then
        assertNotNull(dto);
        assertEquals(1L, dto.palletId());
        assertEquals(100L, dto.skuId()); // skuId from pallet
        assertNull(dto.skuCode()); // null because SKU entity is null
        assertNull(dto.skuName());
    }

    @Test
    void shouldMapPalletToStockItemDto_WhenLocationIsNull() {
        // Given
        Pallet pallet = new Pallet();
        pallet.setId(1L);
        pallet.setCode("PLT001");
        pallet.setSkuId(100L);
        pallet.setQuantity(new BigDecimal("50.00"));
        pallet.setLocation(null); // Not yet placed

        // When
        StockItemDto dto = stockMapper.toStockItemDto(pallet, null);

        // Then
        assertNotNull(dto);
        assertNull(dto.locationId());
        assertNull(dto.locationCode());
    }

    @Test
    void shouldMapPalletMovementToStockMovementDto_WhenAllFieldsPresent() {
        // Given
        Pallet pallet = new Pallet();
        pallet.setId(1L);
        pallet.setCode("PLT001");

        Location fromLocation = new Location();
        fromLocation.setId(100L);
        fromLocation.setCode("RECV-01");

        Location toLocation = new Location();
        toLocation.setId(200L);
        toLocation.setCode("STOR-A-01");

        PalletMovement movement = new PalletMovement();
        movement.setPallet(pallet);
        movement.setMovementType(MovementType.PLACE);
        movement.setFromLocation(fromLocation);
        movement.setToLocation(toLocation);
        movement.setQuantity(new BigDecimal("100.00"));
        movement.setTaskId(5L);
        movement.setMovedBy("operator1");
        movement.setMovedAt(LocalDateTime.of(2026, 1, 10, 14, 30));

        // When
        StockMovementDto dto = stockMapper.toStockMovementDto(movement);

        // Then
        assertNotNull(dto);
        // Note: dto.id() will be null since we didn't persist the movement
        assertEquals(1L, dto.palletId());
        assertEquals("PLT001", dto.palletCode());
        assertEquals("PLACE", dto.movementType());
        assertEquals(100L, dto.fromLocationId());
        assertEquals("RECV-01", dto.fromLocationCode());
        assertEquals(200L, dto.toLocationId());
        assertEquals("STOR-A-01", dto.toLocationCode());
        assertEquals(new BigDecimal("100.00"), dto.quantity());
        assertEquals(5L, dto.taskId());
        assertEquals("operator1", dto.movedBy());
        assertEquals(LocalDateTime.of(2026, 1, 10, 14, 30), dto.movedAt());
    }

    @Test
    void shouldMapPalletMovementToStockMovementDto_WhenReceiveType() {
        // Given
        Pallet pallet = new Pallet();
        pallet.setId(1L);
        pallet.setCode("PLT001");

        Location toLocation = new Location();
        toLocation.setId(200L);
        toLocation.setCode("RECV-01");

        PalletMovement movement = new PalletMovement();
        movement.setPallet(pallet);
        movement.setMovementType(MovementType.RECEIVE);
        movement.setFromLocation(null); // No previous location for RECEIVE
        movement.setToLocation(toLocation);
        movement.setMovedBy("operator1");
        movement.setMovedAt(LocalDateTime.of(2026, 1, 10, 14, 30));

        // When
        StockMovementDto dto = stockMapper.toStockMovementDto(movement);

        // Then
        assertNotNull(dto);
        assertEquals("RECEIVE", dto.movementType());
        assertNull(dto.fromLocationId());
        assertNull(dto.fromLocationCode());
        assertEquals(200L, dto.toLocationId());
        assertEquals("RECV-01", dto.toLocationCode());
    }
}
