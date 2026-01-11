package com.wmsdipl.core.service;

import com.wmsdipl.core.domain.*;
import com.wmsdipl.core.repository.PalletMovementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockMovementServiceTest {

    @Mock
    private PalletMovementRepository palletMovementRepository;

    @InjectMocks
    private StockMovementService stockMovementService;

    private Pallet testPallet;
    private Location fromLocation;
    private Location toLocation;

    @BeforeEach
    void setUp() {
        testPallet = new Pallet();
        testPallet.setId(1L);
        testPallet.setCode("PLT001");
        testPallet.setQuantity(new BigDecimal("100.00"));

        fromLocation = new Location();
        fromLocation.setId(100L);
        fromLocation.setCode("FROM-LOC");

        toLocation = new Location();
        toLocation.setId(200L);
        toLocation.setCode("TO-LOC");
    }

    @Test
    void shouldRecordReceive_WhenCalled() {
        // Given
        when(palletMovementRepository.save(any(PalletMovement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        PalletMovement result = stockMovementService.recordReceive(
                testPallet, toLocation, "operator1", 5L);

        // Then
        assertNotNull(result);
        ArgumentCaptor<PalletMovement> captor = ArgumentCaptor.forClass(PalletMovement.class);
        verify(palletMovementRepository).save(captor.capture());

        PalletMovement saved = captor.getValue();
        assertEquals(testPallet, saved.getPallet());
        assertNull(saved.getFromLocation()); // No previous location for RECEIVE
        assertEquals(toLocation, saved.getToLocation());
        assertEquals(MovementType.RECEIVE, saved.getMovementType());
        assertEquals(new BigDecimal("100.00"), saved.getQuantity());
        assertEquals("operator1", saved.getMovedBy());
        assertEquals(5L, saved.getTaskId());
        assertNotNull(saved.getMovedAt());
    }

    @Test
    void shouldRecordPlacement_WhenCalled() {
        // Given
        when(palletMovementRepository.save(any(PalletMovement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        PalletMovement result = stockMovementService.recordPlacement(
                testPallet, fromLocation, toLocation, "operator1", 10L);

        // Then
        assertNotNull(result);
        ArgumentCaptor<PalletMovement> captor = ArgumentCaptor.forClass(PalletMovement.class);
        verify(palletMovementRepository).save(captor.capture());

        PalletMovement saved = captor.getValue();
        assertEquals(testPallet, saved.getPallet());
        assertEquals(fromLocation, saved.getFromLocation());
        assertEquals(toLocation, saved.getToLocation());
        assertEquals(MovementType.PLACE, saved.getMovementType());
        assertEquals(new BigDecimal("100.00"), saved.getQuantity());
        assertEquals("operator1", saved.getMovedBy());
        assertEquals(10L, saved.getTaskId());
    }

    @Test
    void shouldRecordMove_WhenCalled() {
        // Given
        when(palletMovementRepository.save(any(PalletMovement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        PalletMovement result = stockMovementService.recordMove(
                testPallet, fromLocation, toLocation, "operator1", null);

        // Then
        assertNotNull(result);
        ArgumentCaptor<PalletMovement> captor = ArgumentCaptor.forClass(PalletMovement.class);
        verify(palletMovementRepository).save(captor.capture());

        PalletMovement saved = captor.getValue();
        assertEquals(MovementType.MOVE, saved.getMovementType());
        assertNull(saved.getTaskId()); // Manual move, no task
    }

    @Test
    void shouldRecordPick_WhenFullPallet() {
        // Given
        when(palletMovementRepository.save(any(PalletMovement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        PalletMovement result = stockMovementService.recordPick(
                testPallet, fromLocation, null, "operator1", 20L);

        // Then
        assertNotNull(result);
        ArgumentCaptor<PalletMovement> captor = ArgumentCaptor.forClass(PalletMovement.class);
        verify(palletMovementRepository).save(captor.capture());

        PalletMovement saved = captor.getValue();
        assertEquals(MovementType.PICK, saved.getMovementType());
        assertEquals(fromLocation, saved.getFromLocation());
        assertNull(saved.getToLocation()); // Picked items leave warehouse
        assertEquals(new BigDecimal("100.00"), saved.getQuantity()); // Full pallet
    }

    @Test
    void shouldRecordPick_WhenPartialQuantity() {
        // Given
        BigDecimal partialQty = new BigDecimal("30.00");
        when(palletMovementRepository.save(any(PalletMovement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        PalletMovement result = stockMovementService.recordPick(
                testPallet, fromLocation, partialQty, "operator1", 20L);

        // Then
        assertNotNull(result);
        ArgumentCaptor<PalletMovement> captor = ArgumentCaptor.forClass(PalletMovement.class);
        verify(palletMovementRepository).save(captor.capture());

        PalletMovement saved = captor.getValue();
        assertEquals(new BigDecimal("30.00"), saved.getQuantity()); // Partial pick
    }

    @Test
    void shouldRecordAdjustment_WhenCalled() {
        // Given
        BigDecimal newQuantity = new BigDecimal("95.00");
        when(palletMovementRepository.save(any(PalletMovement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        PalletMovement result = stockMovementService.recordAdjustment(
                testPallet, fromLocation, newQuantity, "supervisor1");

        // Then
        assertNotNull(result);
        ArgumentCaptor<PalletMovement> captor = ArgumentCaptor.forClass(PalletMovement.class);
        verify(palletMovementRepository).save(captor.capture());

        PalletMovement saved = captor.getValue();
        assertEquals(MovementType.ADJUST, saved.getMovementType());
        assertEquals(fromLocation, saved.getFromLocation());
        assertEquals(fromLocation, saved.getToLocation()); // Same location
        assertEquals(new BigDecimal("95.00"), saved.getQuantity());
        assertEquals("supervisor1", saved.getMovedBy());
        assertNull(saved.getTaskId()); // Manual adjustment
    }
}
