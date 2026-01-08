package com.wmsdipl.core.service;

import com.wmsdipl.core.domain.Location;
import com.wmsdipl.core.domain.Pallet;
import com.wmsdipl.core.domain.PalletMovement;
import com.wmsdipl.core.domain.PalletStatus;
import com.wmsdipl.core.repository.LocationRepository;
import com.wmsdipl.core.repository.PalletCodePoolRepository;
import com.wmsdipl.core.repository.PalletMovementRepository;
import com.wmsdipl.core.repository.PalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PalletServiceTest {

    @Mock
    private PalletRepository palletRepository;

    @Mock
    private PalletMovementRepository movementRepository;

    @Mock
    private PalletCodePoolRepository codePoolRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private PalletCodeGenerator codeGenerator;

    @InjectMocks
    private PalletService palletService;

    private Pallet testPallet;
    private Location fromLocation;
    private Location toLocation;
    private PalletMovement testMovement;

    @BeforeEach
    void setUp() {
        fromLocation = new Location();
        fromLocation.setCode("LOC-A-01");

        toLocation = new Location();
        toLocation.setCode("LOC-B-01");

        testPallet = new Pallet();
        testPallet.setCode("PALLET001");
        testPallet.setLocation(fromLocation);
        testPallet.setStatus(PalletStatus.RECEIVED);

        testMovement = new PalletMovement();
        testMovement.setPallet(testPallet);
        testMovement.setFromLocation(fromLocation);
        testMovement.setToLocation(toLocation);
    }

    @Test
    void shouldGetAllPallets_WhenCalled() {
        // Given
        List<Pallet> pallets = List.of(testPallet);
        when(palletRepository.findAll()).thenReturn(pallets);

        // When
        List<Pallet> result = palletService.getAll();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(palletRepository, times(1)).findAll();
    }

    @Test
    void shouldGetPalletById_WhenValidId() {
        // Given
        when(palletRepository.findById(1L)).thenReturn(Optional.of(testPallet));

        // When
        Optional<Pallet> result = palletService.getById(1L);

        // Then
        assertTrue(result.isPresent());
        assertEquals("PALLET001", result.get().getCode());
        verify(palletRepository, times(1)).findById(1L);
    }

    @Test
    void shouldReturnEmpty_WhenPalletNotFound() {
        // Given
        when(palletRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        Optional<Pallet> result = palletService.getById(999L);

        // Then
        assertFalse(result.isPresent());
        verify(palletRepository, times(1)).findById(999L);
    }

    @Test
    void shouldCreatePallet_WhenValid() {
        // Given
        when(palletRepository.save(any(Pallet.class))).thenReturn(testPallet);

        // When
        Pallet result = palletService.create(testPallet);

        // Then
        assertNotNull(result);
        assertEquals("PALLET001", result.getCode());
        verify(palletRepository, times(1)).save(testPallet);
    }

    @Test
    void shouldMovePallet_WhenValidLocations() {
        // Given
        when(palletRepository.findById(1L)).thenReturn(Optional.of(testPallet));
        when(locationRepository.findById(2L)).thenReturn(Optional.of(toLocation));
        when(palletRepository.save(any(Pallet.class))).thenReturn(testPallet);
        when(movementRepository.save(any(PalletMovement.class))).thenReturn(testMovement);

        // When
        Pallet result = palletService.move(1L, 2L, "MANUAL", "user1");

        // Then
        assertNotNull(result);
        assertEquals(toLocation, result.getLocation());
        assertEquals(PalletStatus.PLACED, result.getStatus());
        verify(palletRepository, times(1)).save(any(Pallet.class));
        verify(movementRepository, times(1)).save(any(PalletMovement.class));
    }

    @Test
    void shouldThrowException_WhenPalletNotFoundForMove() {
        // Given
        when(palletRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalArgumentException.class, 
            () -> palletService.move(999L, 2L, "MANUAL", "user1"));
        verify(palletRepository, never()).save(any(Pallet.class));
        verify(movementRepository, never()).save(any(PalletMovement.class));
    }

    @Test
    void shouldThrowException_WhenLocationNotFoundForMove() {
        // Given
        when(palletRepository.findById(1L)).thenReturn(Optional.of(testPallet));
        when(locationRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalArgumentException.class, 
            () -> palletService.move(1L, 999L, "MANUAL", "user1"));
        verify(palletRepository, never()).save(any(Pallet.class));
        verify(movementRepository, never()).save(any(PalletMovement.class));
    }

    @Test
    void shouldGetMovements_WhenValidPalletId() {
        // Given
        List<PalletMovement> movements = List.of(testMovement);
        when(palletRepository.findById(1L)).thenReturn(Optional.of(testPallet));
        when(movementRepository.findByPallet(testPallet)).thenReturn(movements);

        // When
        List<PalletMovement> result = palletService.getMovements(1L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(movementRepository, times(1)).findByPallet(testPallet);
    }

    @Test
    void shouldThrowException_WhenPalletNotFoundForMovements() {
        // Given
        when(palletRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalArgumentException.class, 
            () -> palletService.getMovements(999L));
        verify(movementRepository, never()).findByPallet(any(Pallet.class));
    }

    @Test
    void shouldGenerateInternalCodes_WhenValidParameters() {
        // Given
        List<String> expectedCodes = List.of("INT001", "INT002", "INT003");
        when(codeGenerator.generateInternalCodes("INT", 3)).thenReturn(expectedCodes);
        when(codePoolRepository.saveAll(anyList())).thenReturn(List.of());

        // When
        List<String> result = palletService.generateInternal("INT", 3, "INTERNAL");

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.contains("INT001"));
        verify(codeGenerator, times(1)).generateInternalCodes("INT", 3);
        verify(codePoolRepository, times(1)).saveAll(anyList());
    }

    @Test
    void shouldGenerateSSCCCodes_WhenValidParameters() {
        // Given
        List<String> expectedCodes = List.of(
            "000012345678901234567", 
            "000012345678901234568", 
            "000012345678901234569"
        );
        when(codeGenerator.generateSSCC("0000123456789", 3)).thenReturn(expectedCodes);
        when(codePoolRepository.saveAll(anyList())).thenReturn(List.of());

        // When
        List<String> result = palletService.generateSSCC("0000123456789", 3);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
        verify(codeGenerator, times(1)).generateSSCC("0000123456789", 3);
        verify(codePoolRepository, times(1)).saveAll(anyList());
    }

    @Test
    void shouldUseDefaultCodeType_WhenNullProvided() {
        // Given
        List<String> expectedCodes = List.of("INT001");
        when(codeGenerator.generateInternalCodes("INT", 1)).thenReturn(expectedCodes);
        when(codePoolRepository.saveAll(anyList())).thenReturn(List.of());

        // When
        List<String> result = palletService.generateInternal("INT", 1, null);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(codePoolRepository, times(1)).saveAll(anyList());
    }
}
