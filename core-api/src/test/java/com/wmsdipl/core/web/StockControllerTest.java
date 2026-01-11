package com.wmsdipl.core.web;

import com.wmsdipl.contracts.dto.StockItemDto;
import com.wmsdipl.contracts.dto.StockMovementDto;
import com.wmsdipl.core.domain.*;
import com.wmsdipl.core.mapper.StockMapper;
import com.wmsdipl.core.service.StockService;
import com.wmsdipl.core.service.StockService.StockResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * REST API tests for StockController using MockMvc.
 * Tests stock inventory queries, pallet details, movement history, and location-based queries.
 */
@WebMvcTest(StockController.class)
@AutoConfigureMockMvc(addFilters = false)
class StockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StockService stockService;

    @MockBean
    private StockMapper stockMapper;

    @Test
    void shouldReturnStockInventory_WhenNoFilters() throws Exception {
        // Given
        Pallet pallet1 = createPallet(1L, "PLT-001", 100L);
        Pallet pallet2 = createPallet(2L, "PLT-002", 101L);
        Sku sku1 = createSku(100L, "SKU-001", "Product One");
        Sku sku2 = createSku(101L, "SKU-002", "Product Two");
        
        Page<Pallet> palletPage = new PageImpl<>(List.of(pallet1, pallet2));
        Map<Long, Sku> skuMap = Map.of(100L, sku1, 101L, sku2);
        StockResult stockResult = new StockResult(palletPage, skuMap);
        
        StockItemDto dto1 = createStockItemDto(1L, "PLT-001", "SKU-001");
        StockItemDto dto2 = createStockItemDto(2L, "PLT-002", "SKU-002");

        when(stockService.getStock(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(stockResult);
        when(stockMapper.toStockItemDto(pallet1, sku1)).thenReturn(dto1);
        when(stockMapper.toStockItemDto(pallet2, sku2)).thenReturn(dto2);

        // When & Then
        mockMvc.perform(get("/api/stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].palletId").value(1))
                .andExpect(jsonPath("$.content[0].palletCode").value("PLT-001"))
                .andExpect(jsonPath("$.content[1].palletId").value(2))
                .andExpect(jsonPath("$.content[1].palletCode").value("PLT-002"))
                .andExpect(jsonPath("$.totalElements").value(2));

        verify(stockService).getStock(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class));
        verify(stockMapper, times(2)).toStockItemDto(any(Pallet.class), any(Sku.class));
    }

    @Test
    void shouldFilterBySkuCode_WhenSkuCodeProvided() throws Exception {
        // Given
        Pallet pallet = createPallet(1L, "PLT-001", 100L);
        Sku sku = createSku(100L, "SKU-001", "Product One");
        
        Page<Pallet> palletPage = new PageImpl<>(List.of(pallet));
        Map<Long, Sku> skuMap = Map.of(100L, sku);
        StockResult stockResult = new StockResult(palletPage, skuMap);
        
        StockItemDto dto = createStockItemDto(1L, "PLT-001", "SKU-001");

        when(stockService.getStock(eq("SKU-001"), isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(stockResult);
        when(stockMapper.toStockItemDto(pallet, sku)).thenReturn(dto);

        // When & Then
        mockMvc.perform(get("/api/stock")
                        .param("skuCode", "SKU-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].skuCode").value("SKU-001"));

        verify(stockService).getStock(eq("SKU-001"), isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void shouldFilterByLocationCode_WhenLocationCodeProvided() throws Exception {
        // Given
        Pallet pallet = createPallet(1L, "PLT-001", 100L);
        Sku sku = createSku(100L, "SKU-001", "Product One");
        
        Page<Pallet> palletPage = new PageImpl<>(List.of(pallet));
        Map<Long, Sku> skuMap = Map.of(100L, sku);
        StockResult stockResult = new StockResult(palletPage, skuMap);
        
        StockItemDto dto = createStockItemDto(1L, "PLT-001", "SKU-001");

        when(stockService.getStock(isNull(), eq("A-01"), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(stockResult);
        when(stockMapper.toStockItemDto(pallet, sku)).thenReturn(dto);

        // When & Then
        mockMvc.perform(get("/api/stock")
                        .param("locationCode", "A-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));

        verify(stockService).getStock(isNull(), eq("A-01"), isNull(), isNull(), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void shouldFilterByMultipleParameters_WhenAllProvided() throws Exception {
        // Given
        LocalDateTime asOfDate = LocalDateTime.of(2026, 1, 10, 10, 0);
        
        Page<Pallet> emptyPage = new PageImpl<>(List.of());
        StockResult stockResult = new StockResult(emptyPage, Map.of());

        when(stockService.getStock(
                eq("SKU-001"), 
                eq("A-01"), 
                eq("PLT"), 
                eq(123L), 
                eq("PLACED"), 
                eq(asOfDate), 
                any(Pageable.class)))
                .thenReturn(stockResult);

        // When & Then
        mockMvc.perform(get("/api/stock")
                        .param("skuCode", "SKU-001")
                        .param("locationCode", "A-01")
                        .param("palletBarcode", "PLT")
                        .param("receiptId", "123")
                        .param("status", "PLACED")
                        .param("asOfDate", "2026-01-10T10:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));

        verify(stockService).getStock(
                eq("SKU-001"), 
                eq("A-01"), 
                eq("PLT"), 
                eq(123L), 
                eq("PLACED"), 
                eq(asOfDate), 
                any(Pageable.class));
    }

    @Test
    void shouldReturnPalletDetails_WhenValidId() throws Exception {
        // Given
        Pallet pallet = createPallet(1L, "PLT-001", 100L);
        Sku sku = createSku(100L, "SKU-001", "Product One");
        
        Map<Long, Sku> skuMap = Map.of(100L, sku);
        StockResult stockResult = new StockResult(pallet, skuMap);
        
        StockItemDto dto = createStockItemDto(1L, "PLT-001", "SKU-001");

        when(stockService.getPalletById(1L)).thenReturn(stockResult);
        when(stockMapper.toStockItemDto(pallet, sku)).thenReturn(dto);

        // When & Then
        mockMvc.perform(get("/api/stock/pallet/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.palletId").value(1))
                .andExpect(jsonPath("$.palletCode").value("PLT-001"))
                .andExpect(jsonPath("$.skuCode").value("SKU-001"));

        verify(stockService).getPalletById(1L);
        verify(stockMapper).toStockItemDto(pallet, sku);
    }

    @Test
    void shouldReturnMovementHistory_WhenValidPalletId() throws Exception {
        // Given
        PalletMovement movement1 = createMovement(1L, MovementType.RECEIVE);
        PalletMovement movement2 = createMovement(2L, MovementType.PLACE);
        
        StockMovementDto dto1 = createMovementDto(1L, "RECEIVE");
        StockMovementDto dto2 = createMovementDto(2L, "PLACE");

        when(stockService.getPalletHistory(1L)).thenReturn(List.of(movement1, movement2));
        when(stockMapper.toStockMovementDto(movement1)).thenReturn(dto1);
        when(stockMapper.toStockMovementDto(movement2)).thenReturn(dto2);

        // When & Then
        mockMvc.perform(get("/api/stock/pallet/1/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].movementType").value("RECEIVE"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].movementType").value("PLACE"));

        verify(stockService).getPalletHistory(1L);
        verify(stockMapper, times(2)).toStockMovementDto(any(PalletMovement.class));
    }

    @Test
    void shouldReturnEmptyHistory_WhenNoPalletMovements() throws Exception {
        // Given
        when(stockService.getPalletHistory(1L)).thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/stock/pallet/1/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(stockService).getPalletHistory(1L);
        verify(stockMapper, never()).toStockMovementDto(any());
    }

    @Test
    void shouldReturnPalletsByLocation_WhenValidLocationId() throws Exception {
        // Given
        Pallet pallet1 = createPallet(1L, "PLT-001", 100L);
        Pallet pallet2 = createPallet(2L, "PLT-002", 100L);
        Sku sku = createSku(100L, "SKU-001", "Product One");
        
        Page<Pallet> palletPage = new PageImpl<>(List.of(pallet1, pallet2));
        Map<Long, Sku> skuMap = Map.of(100L, sku);
        StockResult stockResult = new StockResult(palletPage, skuMap);
        
        StockItemDto dto1 = createStockItemDto(1L, "PLT-001", "SKU-001");
        StockItemDto dto2 = createStockItemDto(2L, "PLT-002", "SKU-001");

        when(stockService.getPalletsByLocation(eq(10L), any(Pageable.class))).thenReturn(stockResult);
        when(stockMapper.toStockItemDto(pallet1, sku)).thenReturn(dto1);
        when(stockMapper.toStockItemDto(pallet2, sku)).thenReturn(dto2);

        // When & Then
        mockMvc.perform(get("/api/stock/location/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].palletId").value(1))
                .andExpect(jsonPath("$.content[1].palletId").value(2));

        verify(stockService).getPalletsByLocation(eq(10L), any(Pageable.class));
        verify(stockMapper, times(2)).toStockItemDto(any(Pallet.class), any(Sku.class));
    }

    @Test
    void shouldReturnEmptyPage_WhenNoPalletsInLocation() throws Exception {
        // Given
        Page<Pallet> emptyPage = new PageImpl<>(List.of());
        StockResult stockResult = new StockResult(emptyPage, Map.of());

        when(stockService.getPalletsByLocation(eq(10L), any(Pageable.class))).thenReturn(stockResult);

        // When & Then
        mockMvc.perform(get("/api/stock/location/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));

        verify(stockService).getPalletsByLocation(eq(10L), any(Pageable.class));
        verify(stockMapper, never()).toStockItemDto(any(), any());
    }

    @Test
    void shouldRespectPagination_WhenPageParametersProvided() throws Exception {
        // Given
        Page<Pallet> emptyPage = new PageImpl<>(List.of());
        StockResult stockResult = new StockResult(emptyPage, Map.of());

        when(stockService.getStock(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(stockResult);

        // When & Then
        mockMvc.perform(get("/api/stock")
                        .param("page", "2")
                        .param("size", "100")
                        .param("sort", "code,desc"))
                .andExpect(status().isOk());

        verify(stockService).getStock(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class));
    }

    // Helper methods

    private Pallet createPallet(Long id, String code, Long skuId) {
        Pallet pallet = new Pallet();
        try {
            java.lang.reflect.Field idField = Pallet.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(pallet, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        pallet.setCode(code);
        pallet.setCodeType("INTERNAL");
        pallet.setStatus(PalletStatus.PLACED);
        pallet.setSkuId(skuId);
        pallet.setQuantity(new BigDecimal("100.00"));
        pallet.setUom("ШТ");

        return pallet;
    }

    private Sku createSku(Long id, String code, String name) {
        Sku sku = new Sku();
        try {
            java.lang.reflect.Field idField = Sku.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(sku, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        sku.setCode(code);
        sku.setName(name);

        return sku;
    }

    private PalletMovement createMovement(Long id, MovementType type) {
        PalletMovement movement = new PalletMovement();
        try {
            java.lang.reflect.Field idField = PalletMovement.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(movement, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        movement.setMovementType(type);
        movement.setMovedAt(LocalDateTime.now());
        movement.setMovedBy("testuser");

        return movement;
    }

    private StockItemDto createStockItemDto(Long palletId, String palletCode, String skuCode) {
        return new StockItemDto(
                palletId,
                palletCode,
                "PLACED",
                100L, // skuId
                skuCode,
                "Product Name",
                new BigDecimal("100.00"),
                "ШТ",
                10L, // locationId
                "A-01", // locationCode
                1L, // receiptId
                "RCP-001", // receiptDocNumber
                LocalDateTime.now(), // receiptDate
                "LOT-001", // lotNumber
                null, // expiryDate
                LocalDateTime.now(), // createdAt
                LocalDateTime.now() // updatedAt
        );
    }

    private StockMovementDto createMovementDto(Long id, String movementType) {
        return new StockMovementDto(
                id,
                1L, // palletId
                "PLT-001", // palletCode
                movementType,
                null, // fromLocationId
                null, // fromLocationCode
                10L, // toLocationId
                "A-01", // toLocationCode
                new BigDecimal("100.00"),
                1L, // taskId
                "testuser",
                LocalDateTime.now()
        );
    }
}
