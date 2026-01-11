package com.wmsdipl.core.service;

import com.wmsdipl.core.domain.Location;
import com.wmsdipl.core.domain.LocationStatus;
import com.wmsdipl.core.domain.Zone;
import com.wmsdipl.core.repository.LocationRepository;
import com.wmsdipl.core.repository.PalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocationServiceTest {

    @Mock
    private LocationRepository locationRepository;
    
    @Mock
    private PalletRepository palletRepository;

    @InjectMocks
    private LocationService locationService;

    private Location testLocation;
    private Zone testZone;

    @BeforeEach
    void setUp() {
        testZone = new Zone();
        testZone.setCode("ZONE-A");
        testZone.setName("Zone A");

        testLocation = new Location();
        testLocation.setCode("LOC-A-01-01-01");
        testLocation.setZone(testZone);
        testLocation.setAisle("A");
        testLocation.setBay("01");
        testLocation.setLevel("01");
        testLocation.setXCoord(new BigDecimal("100"));
        testLocation.setYCoord(new BigDecimal("200"));
        testLocation.setZCoord(new BigDecimal("300"));
        testLocation.setMaxWeightKg(new BigDecimal("1000"));
        testLocation.setMaxHeightCm(new BigDecimal("200"));
        testLocation.setMaxWidthCm(new BigDecimal("120"));
        testLocation.setMaxDepthCm(new BigDecimal("80"));
        testLocation.setMaxPallets(2);
        testLocation.setActive(true);
    }

    @Test
    void shouldGetAllLocations_WhenCalled() {
        // Given
        List<Location> locations = List.of(testLocation);
        when(locationRepository.findAll()).thenReturn(locations);

        // When
        List<Location> result = locationService.getAll();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(locationRepository, times(1)).findAll();
    }

    @Test
    void shouldGetLocationById_WhenValidId() {
        // Given
        when(locationRepository.findById(1L)).thenReturn(Optional.of(testLocation));

        // When
        Optional<Location> result = locationService.getById(1L);

        // Then
        assertTrue(result.isPresent());
        assertEquals("LOC-A-01-01-01", result.get().getCode());
        verify(locationRepository, times(1)).findById(1L);
    }

    @Test
    void shouldReturnEmpty_WhenLocationNotFoundById() {
        // Given
        when(locationRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        Optional<Location> result = locationService.getById(999L);

        // Then
        assertFalse(result.isPresent());
        verify(locationRepository, times(1)).findById(999L);
    }

    @Test
    void shouldGetLocationByCode_WhenValidCode() {
        // Given
        when(locationRepository.findByCode("LOC-A-01-01-01")).thenReturn(Optional.of(testLocation));

        // When
        Optional<Location> result = locationService.getByCode("LOC-A-01-01-01");

        // Then
        assertTrue(result.isPresent());
        assertEquals("LOC-A-01-01-01", result.get().getCode());
        verify(locationRepository, times(1)).findByCode("LOC-A-01-01-01");
    }

    @Test
    void shouldReturnEmpty_WhenLocationNotFoundByCode() {
        // Given
        when(locationRepository.findByCode("INVALID")).thenReturn(Optional.empty());

        // When
        Optional<Location> result = locationService.getByCode("INVALID");

        // Then
        assertFalse(result.isPresent());
        verify(locationRepository, times(1)).findByCode("INVALID");
    }

    @Test
    void shouldGetLocationsByZone_WhenValidZone() {
        // Given
        List<Location> locations = List.of(testLocation);
        when(locationRepository.findByZone(testZone)).thenReturn(locations);

        // When
        List<Location> result = locationService.getByZone(testZone);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testZone, result.get(0).getZone());
        verify(locationRepository, times(1)).findByZone(testZone);
    }

    @Test
    void shouldCreateLocation_WhenValid() {
        // Given
        when(locationRepository.save(any(Location.class))).thenReturn(testLocation);

        // When
        Location result = locationService.create(testLocation);

        // Then
        assertNotNull(result);
        assertEquals("LOC-A-01-01-01", result.getCode());
        verify(locationRepository, times(1)).save(testLocation);
    }

    @Test
    void shouldUpdateLocation_WhenValidData() {
        // Given
        Location updateData = new Location();
        updateData.setCode("LOC-B-02-02-02");
        updateData.setZone(testZone);
        updateData.setAisle("B");
        updateData.setBay("02");
        updateData.setLevel("02");
        updateData.setXCoord(new BigDecimal("150"));
        updateData.setYCoord(new BigDecimal("250"));
        updateData.setZCoord(new BigDecimal("350"));
        updateData.setMaxWeightKg(new BigDecimal("1500"));
        updateData.setMaxHeightCm(new BigDecimal("250"));
        updateData.setMaxWidthCm(new BigDecimal("150"));
        updateData.setMaxDepthCm(new BigDecimal("100"));
        updateData.setMaxPallets(3);
        updateData.setActive(false);

        when(locationRepository.findById(1L)).thenReturn(Optional.of(testLocation));
        when(palletRepository.countByLocation(any(Location.class))).thenReturn(0L);
        when(locationRepository.save(any(Location.class))).thenReturn(testLocation);

        // When
        Location result = locationService.update(1L, updateData);

        // Then
        assertNotNull(result);
        assertEquals("LOC-B-02-02-02", testLocation.getCode());
        assertEquals("B", testLocation.getAisle());
        assertEquals("02", testLocation.getBay());
        assertEquals("02", testLocation.getLevel());
        assertEquals(new BigDecimal("150"), testLocation.getXCoord());
        assertEquals(new BigDecimal("250"), testLocation.getYCoord());
        assertEquals(new BigDecimal("350"), testLocation.getZCoord());
        assertEquals(new BigDecimal("1500"), testLocation.getMaxWeightKg());
        assertEquals(new BigDecimal("250"), testLocation.getMaxHeightCm());
        assertEquals(new BigDecimal("150"), testLocation.getMaxWidthCm());
        assertEquals(new BigDecimal("100"), testLocation.getMaxDepthCm());
        assertEquals(3, testLocation.getMaxPallets());
        assertFalse(testLocation.getActive());
        verify(locationRepository, times(1)).save(any(Location.class));
    }

    @Test
    void shouldThrowException_WhenUpdateLocationNotFound() {
        // Given
        Location updateData = new Location();
        when(locationRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalArgumentException.class, 
            () -> locationService.update(999L, updateData));
        verify(locationRepository, never()).save(any(Location.class));
    }

    @Test
    void shouldDeleteLocation_WhenValidId() {
        // Given
        Location location = new Location();
        location.setStatus(LocationStatus.AVAILABLE);
        location.setCode("TEST-LOC");
        
        when(locationRepository.findById(1L)).thenReturn(Optional.of(location));
        doNothing().when(locationRepository).deleteById(1L);

        // When
        locationService.delete(1L);

        // Then
        verify(locationRepository, times(1)).findById(1L);
        verify(locationRepository, times(1)).deleteById(1L);
    }

    @Test
    void shouldThrowException_WhenDeletingOccupiedLocation() {
        // Given
        Location location = new Location();
        location.setStatus(LocationStatus.OCCUPIED);
        location.setCode("OCCUPIED-LOC");
        
        when(locationRepository.findById(1L)).thenReturn(Optional.of(location));

        // When & Then
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> locationService.delete(1L)
        );
        assertTrue(exception.getMessage().contains("Cannot delete occupied location"));
        verify(locationRepository, times(1)).findById(1L);
        verify(locationRepository, never()).deleteById(anyLong());
    }

    @Test
    void shouldThrowException_WhenCreatingLocationWithDuplicateCode() {
        // Given
        when(locationRepository.existsByCode("LOC-A-01-01-01")).thenReturn(true);

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> locationService.create(testLocation)
        );
        assertEquals("Location with code 'LOC-A-01-01-01' already exists", exception.getMessage());
        verify(locationRepository, times(1)).existsByCode("LOC-A-01-01-01");
        verify(locationRepository, never()).save(any(Location.class));
    }

    @Test
    void shouldCreateLocation_WhenCodeIsUnique() {
        // Given
        when(locationRepository.existsByCode("LOC-A-01-01-01")).thenReturn(false);
        when(locationRepository.save(any(Location.class))).thenReturn(testLocation);

        // When
        Location result = locationService.create(testLocation);

        // Then
        assertNotNull(result);
        assertEquals("LOC-A-01-01-01", result.getCode());
        verify(locationRepository, times(1)).existsByCode("LOC-A-01-01-01");
        verify(locationRepository, times(1)).save(testLocation);
    }

    @Test
    void shouldThrowException_WhenUpdatingToExistingCode() {
        // Given
        Location updateData = new Location();
        updateData.setCode("LOC-B-02-02-02");
        updateData.setZone(testZone);
        updateData.setAisle("B");
        updateData.setBay("02");
        updateData.setLevel("02");
        updateData.setActive(true);

        when(locationRepository.findById(1L)).thenReturn(Optional.of(testLocation));
        when(palletRepository.countByLocation(any(Location.class))).thenReturn(0L);
        when(locationRepository.existsByCode("LOC-B-02-02-02")).thenReturn(true);

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> locationService.update(1L, updateData)
        );
        assertEquals("Location with code 'LOC-B-02-02-02' already exists", exception.getMessage());
        verify(locationRepository, times(1)).findById(1L);
        verify(palletRepository, times(1)).countByLocation(any(Location.class));
        verify(locationRepository, times(1)).existsByCode("LOC-B-02-02-02");
        verify(locationRepository, never()).save(any(Location.class));
    }
    
    @Test
    void shouldThrowException_WhenUpdatingOccupiedLocation() {
        // Given
        Location updateData = new Location();
        updateData.setCode("LOC-B-03-03-03");
        updateData.setZone(testZone);
        updateData.setAisle("B");
        updateData.setBay("03");
        updateData.setLevel("03");
        updateData.setMaxPallets(5);
        updateData.setActive(true);

        when(locationRepository.findById(1L)).thenReturn(Optional.of(testLocation));
        when(palletRepository.countByLocation(any(Location.class))).thenReturn(2L); // Location has 2 pallets

        // When & Then
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> locationService.update(1L, updateData)
        );
        assertEquals("Ячейка занята, изменение невозможно", exception.getReason());
        verify(locationRepository, times(1)).findById(1L);
        verify(palletRepository, times(1)).countByLocation(any(Location.class));
        verify(locationRepository, never()).save(any(Location.class));
    }

    @Test
    void shouldBlockLocation_WhenAvailable() {
        // Given
        testLocation.setStatus(LocationStatus.AVAILABLE);
        when(locationRepository.findById(1L)).thenReturn(Optional.of(testLocation));
        when(locationRepository.save(any(Location.class))).thenReturn(testLocation);

        // When
        Location result = locationService.blockLocation(1L);

        // Then
        assertNotNull(result);
        assertEquals(LocationStatus.BLOCKED, testLocation.getStatus());
        verify(locationRepository, times(1)).findById(1L);
        verify(locationRepository, times(1)).save(testLocation);
    }

    @Test
    void shouldThrowException_WhenBlockingAlreadyBlockedLocation() {
        // Given
        testLocation.setStatus(LocationStatus.BLOCKED);
        when(locationRepository.findById(1L)).thenReturn(Optional.of(testLocation));

        // When & Then
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> locationService.blockLocation(1L)
        );
        assertTrue(exception.getMessage().contains("Location is already blocked"));
        verify(locationRepository, times(1)).findById(1L);
        verify(locationRepository, never()).save(any(Location.class));
    }

    @Test
    void shouldThrowException_WhenBlockingOccupiedLocation() {
        // Given
        testLocation.setStatus(LocationStatus.OCCUPIED);
        when(locationRepository.findById(1L)).thenReturn(Optional.of(testLocation));

        // When & Then
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> locationService.blockLocation(1L)
        );
        assertTrue(exception.getMessage().contains("Cannot block occupied location"));
        verify(locationRepository, times(1)).findById(1L);
        verify(locationRepository, never()).save(any(Location.class));
    }

    @Test
    void shouldUnblockLocation_WhenBlocked() {
        // Given
        testLocation.setStatus(LocationStatus.BLOCKED);
        when(locationRepository.findById(1L)).thenReturn(Optional.of(testLocation));
        when(locationRepository.save(any(Location.class))).thenReturn(testLocation);

        // When
        Location result = locationService.unblockLocation(1L);

        // Then
        assertNotNull(result);
        assertEquals(LocationStatus.AVAILABLE, testLocation.getStatus());
        verify(locationRepository, times(1)).findById(1L);
        verify(locationRepository, times(1)).save(testLocation);
    }

    @Test
    void shouldThrowException_WhenUnblockingNonBlockedLocation() {
        // Given
        testLocation.setStatus(LocationStatus.AVAILABLE);
        when(locationRepository.findById(1L)).thenReturn(Optional.of(testLocation));

        // When & Then
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> locationService.unblockLocation(1L)
        );
        assertTrue(exception.getMessage().contains("Location is not blocked"));
        verify(locationRepository, times(1)).findById(1L);
        verify(locationRepository, never()).save(any(Location.class));
    }

    @Test
    void shouldUpdateStatus_WhenValidId() {
        // Given
        testLocation.setStatus(LocationStatus.AVAILABLE);
        when(locationRepository.findById(1L)).thenReturn(Optional.of(testLocation));
        when(locationRepository.save(any(Location.class))).thenReturn(testLocation);

        // When
        Location result = locationService.updateStatus(1L, LocationStatus.OCCUPIED);

        // Then
        assertNotNull(result);
        assertEquals(LocationStatus.OCCUPIED, testLocation.getStatus());
        verify(locationRepository, times(1)).findById(1L);
        verify(locationRepository, times(1)).save(testLocation);
    }

    @Test
    void shouldThrowException_WhenUpdatingStatusOfNonexistentLocation() {
        // Given
        when(locationRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> locationService.updateStatus(999L, LocationStatus.BLOCKED)
        );
        assertEquals("Location not found: 999", exception.getMessage());
        verify(locationRepository, times(1)).findById(999L);
        verify(locationRepository, never()).save(any(Location.class));
    }
}
