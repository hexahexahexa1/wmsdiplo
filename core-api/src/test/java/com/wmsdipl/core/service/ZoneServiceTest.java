package com.wmsdipl.core.service;

import com.wmsdipl.core.domain.Zone;
import com.wmsdipl.core.repository.ZoneRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ZoneServiceTest {

    @Mock
    private ZoneRepository zoneRepository;

    @InjectMocks
    private ZoneService zoneService;

    private Zone testZone;

    @BeforeEach
    void setUp() throws Exception {
        testZone = new Zone();
        // Use reflection to set id since there's no setter
        Field idField = Zone.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(testZone, 1L);
        
        testZone.setCode("ZONE-A");
        testZone.setName("Zone A");
        testZone.setPriorityRank(1);
        testZone.setDescription("Test zone");
        testZone.setActive(true);
    }

    @Test
    void shouldGetAllZones_WhenCalled() {
        // Given
        List<Zone> zones = List.of(testZone);
        when(zoneRepository.findAll()).thenReturn(zones);

        // When
        List<Zone> result = zoneService.getAll();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("ZONE-A", result.get(0).getCode());
        verify(zoneRepository, times(1)).findAll();
    }

    @Test
    void shouldGetZoneById_WhenValidId() {
        // Given
        when(zoneRepository.findById(1L)).thenReturn(Optional.of(testZone));

        // When
        Optional<Zone> result = zoneService.getById(1L);

        // Then
        assertTrue(result.isPresent());
        assertEquals("ZONE-A", result.get().getCode());
        verify(zoneRepository, times(1)).findById(1L);
    }

    @Test
    void shouldReturnEmpty_WhenZoneNotFoundById() {
        // Given
        when(zoneRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        Optional<Zone> result = zoneService.getById(999L);

        // Then
        assertFalse(result.isPresent());
        verify(zoneRepository, times(1)).findById(999L);
    }

    @Test
    void shouldGetZoneByCode_WhenValidCode() {
        // Given
        when(zoneRepository.findByCode("ZONE-A")).thenReturn(Optional.of(testZone));

        // When
        Optional<Zone> result = zoneService.getByCode("ZONE-A");

        // Then
        assertTrue(result.isPresent());
        assertEquals("ZONE-A", result.get().getCode());
        verify(zoneRepository, times(1)).findByCode("ZONE-A");
    }

    @Test
    void shouldCreateZone_WhenValidZone() {
        // Given
        when(zoneRepository.existsByCode("ZONE-A")).thenReturn(false);
        when(zoneRepository.save(any(Zone.class))).thenReturn(testZone);

        // When
        Zone result = zoneService.create(testZone);

        // Then
        assertNotNull(result);
        assertEquals("ZONE-A", result.getCode());
        verify(zoneRepository, times(1)).existsByCode("ZONE-A");
        verify(zoneRepository, times(1)).save(any(Zone.class));
    }

    @Test
    void shouldThrowException_WhenCreatingZoneWithDuplicateCode() {
        // Given
        when(zoneRepository.existsByCode("ZONE-A")).thenReturn(true);

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> zoneService.create(testZone)
        );
        assertEquals("Zone with code 'ZONE-A' already exists", exception.getMessage());
        verify(zoneRepository, times(1)).existsByCode("ZONE-A");
        verify(zoneRepository, never()).save(any(Zone.class));
    }

    @Test
    void shouldUpdateZone_WhenValidData() {
        // Given
        Zone updatePayload = new Zone();
        updatePayload.setCode("ZONE-A");
        updatePayload.setName("Updated Zone A");
        updatePayload.setPriorityRank(2);
        updatePayload.setDescription("Updated description");
        updatePayload.setActive(false);

        when(zoneRepository.findById(1L)).thenReturn(Optional.of(testZone));
        when(zoneRepository.save(any(Zone.class))).thenReturn(testZone);

        // When
        Zone result = zoneService.update(1L, updatePayload);

        // Then
        assertNotNull(result);
        assertEquals("Updated Zone A", testZone.getName());
        assertEquals(2, testZone.getPriorityRank());
        assertEquals("Updated description", testZone.getDescription());
        assertFalse(testZone.getActive());
        verify(zoneRepository, times(1)).findById(1L);
        verify(zoneRepository, times(1)).save(any(Zone.class));
    }

    @Test
    void shouldThrowException_WhenUpdatingNonexistentZone() {
        // Given
        Zone updatePayload = new Zone();
        updatePayload.setCode("ZONE-A");
        updatePayload.setName("Updated");
        
        when(zoneRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> zoneService.update(999L, updatePayload)
        );
        assertEquals("Zone not found: 999", exception.getMessage());
        verify(zoneRepository, times(1)).findById(999L);
        verify(zoneRepository, never()).save(any(Zone.class));
    }

    @Test
    void shouldThrowException_WhenUpdatingToExistingCode() {
        // Given
        Zone updatePayload = new Zone();
        updatePayload.setCode("ZONE-B");
        updatePayload.setName("Updated Zone");

        when(zoneRepository.findById(1L)).thenReturn(Optional.of(testZone));
        when(zoneRepository.existsByCode("ZONE-B")).thenReturn(true);

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> zoneService.update(1L, updatePayload)
        );
        assertEquals("Zone with code 'ZONE-B' already exists", exception.getMessage());
        verify(zoneRepository, times(1)).findById(1L);
        verify(zoneRepository, times(1)).existsByCode("ZONE-B");
        verify(zoneRepository, never()).save(any(Zone.class));
    }

    @Test
    void shouldUpdateZone_WhenCodeUnchanged() {
        // Given
        Zone updatePayload = new Zone();
        updatePayload.setCode("ZONE-A"); // Same code
        updatePayload.setName("Updated Name");
        updatePayload.setPriorityRank(1);
        updatePayload.setDescription("Updated");
        updatePayload.setActive(true);

        when(zoneRepository.findById(1L)).thenReturn(Optional.of(testZone));
        when(zoneRepository.save(any(Zone.class))).thenReturn(testZone);

        // When
        Zone result = zoneService.update(1L, updatePayload);

        // Then
        assertNotNull(result);
        assertEquals("Updated Name", testZone.getName());
        verify(zoneRepository, times(1)).findById(1L);
        verify(zoneRepository, never()).existsByCode(anyString()); // Should not check for duplicate
        verify(zoneRepository, times(1)).save(any(Zone.class));
    }

    @Test
    void shouldDeleteZone_WhenNoLocations() {
        // Given
        when(zoneRepository.findById(1L)).thenReturn(Optional.of(testZone));
        doNothing().when(zoneRepository).deleteById(1L);

        // When
        zoneService.delete(1L);

        // Then
        verify(zoneRepository, times(1)).findById(1L);
        verify(zoneRepository, times(1)).deleteById(1L);
    }

    @Test
    void shouldThrowException_WhenDeletingZoneWithLocations() throws Exception {
        // Given
        // Use reflection to add location to the zone's locations list
        Field locationsField = Zone.class.getDeclaredField("locations");
        locationsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<com.wmsdipl.core.domain.Location> locations = 
            (List<com.wmsdipl.core.domain.Location>) locationsField.get(testZone);
        
        com.wmsdipl.core.domain.Location location = new com.wmsdipl.core.domain.Location();
        Field locationIdField = com.wmsdipl.core.domain.Location.class.getDeclaredField("id");
        locationIdField.setAccessible(true);
        locationIdField.set(location, 1L);
        location.setCode("LOC-01");
        locations.add(location);

        when(zoneRepository.findById(1L)).thenReturn(Optional.of(testZone));

        // When & Then
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> zoneService.delete(1L)
        );
        assertTrue(exception.getMessage().contains("Cannot delete zone with existing locations"));
        assertTrue(exception.getMessage().contains("1 location(s)"));
        verify(zoneRepository, times(1)).findById(1L);
        verify(zoneRepository, never()).deleteById(anyLong());
    }

    @Test
    void shouldThrowException_WhenDeletingNonexistentZone() {
        // Given
        when(zoneRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> zoneService.delete(999L)
        );
        assertEquals("Zone not found: 999", exception.getMessage());
        verify(zoneRepository, times(1)).findById(999L);
        verify(zoneRepository, never()).deleteById(anyLong());
    }
}
