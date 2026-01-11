package com.wmsdipl.core.service;

import com.wmsdipl.core.domain.*;
import com.wmsdipl.core.repository.LocationRepository;
import com.wmsdipl.core.repository.PalletMovementRepository;
import com.wmsdipl.core.repository.PalletRepository;
import com.wmsdipl.core.repository.SkuRepository;
import com.wmsdipl.core.service.StockService.StockResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.server.ResponseStatusException;

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
class StockServiceTest {

    @Mock
    private PalletRepository palletRepository;

    @Mock
    private PalletMovementRepository palletMovementRepository;

    @Mock
    private SkuRepository skuRepository;

    @Mock
    private LocationRepository locationRepository;

    @InjectMocks
    private StockService stockService;

    private Pallet testPallet;
    private Sku testSku;
    private Location testLocation;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        testSku = new Sku();
        testSku.setId(100L);
        testSku.setCode("SKU001");
        testSku.setName("Test Product");

        testLocation = new Location();
        testLocation.setId(200L);
        testLocation.setCode("LOC-A-01");

        testPallet = new Pallet();
        testPallet.setId(1L);
        testPallet.setCode("PLT001");
        testPallet.setSkuId(100L);
        testPallet.setQuantity(new BigDecimal("100.00"));
        testPallet.setLocation(testLocation);
        testPallet.setStatus(PalletStatus.PLACED);

        pageable = PageRequest.of(0, 50);
    }

    @Test
    void shouldGetStock_WhenNoFilters() {
        // Given
        Page<Pallet> palletPage = new PageImpl<>(List.of(testPallet), pageable, 1);
        when(palletRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(palletPage);
        when(skuRepository.findAllById(any()))
                .thenReturn(List.of(testSku));

        // When
        StockResult result = stockService.getStock(
                null, null, null, null, null, null, pageable);

        // Then
        assertNotNull(result);
        assertNotNull(result.pallets);
        assertEquals(1, result.pallets.getTotalElements());
        assertEquals(1, result.skuMap.size());
        assertTrue(result.skuMap.containsKey(100L));
        assertEquals("SKU001", result.skuMap.get(100L).getCode());
        verify(palletRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void shouldGetStock_WhenFilteredBySkuCode() {
        // Given
        Page<Pallet> palletPage = new PageImpl<>(List.of(testPallet), pageable, 1);
        when(palletRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(palletPage);
        when(skuRepository.findAllById(any()))
                .thenReturn(List.of(testSku));

        // When
        StockResult result = stockService.getStock(
                "SKU001", null, null, null, null, null, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.pallets.getTotalElements());
        // Note: findByCode is called inside Specification.toPredicate, not directly verifiable
    }

    @Test
    void shouldGetPalletById_WhenValidId() {
        // Given
        when(palletRepository.findById(1L)).thenReturn(Optional.of(testPallet));
        when(skuRepository.findAllById(any()))
                .thenReturn(List.of(testSku));

        // When
        StockResult result = stockService.getPalletById(1L);

        // Then
        assertNotNull(result);
        assertNotNull(result.singlePallet);
        assertEquals("PLT001", result.singlePallet.getCode());
        assertEquals(1, result.skuMap.size());
        verify(palletRepository).findById(1L);
    }

    @Test
    void shouldThrowException_WhenPalletNotFound() {
        // Given
        when(palletRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResponseStatusException.class, () -> 
            stockService.getPalletById(999L)
        );
        verify(palletRepository).findById(999L);
    }

    @Test
    void shouldGetPalletHistory_WhenValidPalletId() {
        // Given
        PalletMovement movement1 = new PalletMovement();
        movement1.setMovementType(MovementType.RECEIVE);
        movement1.setMovedAt(LocalDateTime.of(2026, 1, 10, 10, 0));

        PalletMovement movement2 = new PalletMovement();
        movement2.setMovementType(MovementType.PLACE);
        movement2.setMovedAt(LocalDateTime.of(2026, 1, 10, 11, 0));

        List<PalletMovement> movements = List.of(movement2, movement1); // Descending order

        when(palletRepository.findById(1L)).thenReturn(Optional.of(testPallet));
        when(palletMovementRepository.findByPalletOrderByMovedAtDesc(testPallet))
                .thenReturn(movements);

        // When
        List<PalletMovement> result = stockService.getPalletHistory(1L);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(MovementType.PLACE, result.get(0).getMovementType()); // Newest first
        assertEquals(MovementType.RECEIVE, result.get(1).getMovementType());
        verify(palletMovementRepository).findByPalletOrderByMovedAtDesc(testPallet);
    }

    @Test
    void shouldGetPalletsByLocation_WhenValidLocationId() {
        // Given
        Page<Pallet> palletPage = new PageImpl<>(List.of(testPallet), pageable, 1);
        when(locationRepository.findById(200L)).thenReturn(Optional.of(testLocation));
        when(palletRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(palletPage);
        when(skuRepository.findAllById(any()))
                .thenReturn(List.of(testSku));

        // When
        StockResult result = stockService.getPalletsByLocation(200L, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.pallets.getTotalElements());
        verify(locationRepository).findById(200L);
    }

    @Test
    void shouldThrowException_WhenLocationNotFound() {
        // Given
        when(locationRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResponseStatusException.class, () -> 
            stockService.getPalletsByLocation(999L, pageable)
        );
        verify(locationRepository).findById(999L);
    }

    @Test
    void shouldLoadSkusForPallets_WhenMultiplePallets() {
        // Given
        Pallet pallet2 = new Pallet();
        pallet2.setId(2L);
        pallet2.setSkuId(101L);
        pallet2.setQuantity(new BigDecimal("50.00"));

        Sku sku2 = new Sku();
        sku2.setId(101L);
        sku2.setCode("SKU002");

        Page<Pallet> palletPage = new PageImpl<>(List.of(testPallet, pallet2), pageable, 2);
        when(palletRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(palletPage);
        when(skuRepository.findAllById(any()))
                .thenReturn(List.of(testSku, sku2));

        // When
        StockResult result = stockService.getStock(
                null, null, null, null, null, null, pageable);

        // Then
        assertNotNull(result);
        assertEquals(2, result.pallets.getTotalElements());
        assertEquals(2, result.skuMap.size());
        assertTrue(result.skuMap.containsKey(100L));
        assertTrue(result.skuMap.containsKey(101L));
        verify(skuRepository).findAllById(any());
    }
}
