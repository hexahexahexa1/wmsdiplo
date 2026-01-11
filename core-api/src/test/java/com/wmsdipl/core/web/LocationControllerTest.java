package com.wmsdipl.core.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wmsdipl.contracts.dto.CreateLocationRequest;
import com.wmsdipl.contracts.dto.LocationDto;
import com.wmsdipl.contracts.dto.UpdateLocationRequest;
import com.wmsdipl.core.domain.Location;
import com.wmsdipl.core.domain.LocationStatus;
import com.wmsdipl.core.domain.Zone;
import com.wmsdipl.core.mapper.LocationMapper;
import com.wmsdipl.core.repository.ZoneRepository;
import com.wmsdipl.core.service.LocationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for LocationController.
 * Tests location management REST API endpoints with security disabled.
 */
@WebMvcTest(LocationController.class)
@AutoConfigureMockMvc(addFilters = false)
class LocationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LocationService locationService;

    @MockBean
    private LocationMapper locationMapper;

    @MockBean
    private ZoneRepository zoneRepository;

    private Location testLocation;
    private LocationDto testLocationDto;
    private Zone testZone;
    private CreateLocationRequest createRequest;
    private UpdateLocationRequest updateRequest;

    @BeforeEach
    void setUp() throws Exception {
        testZone = new Zone();
        testZone.setCode("ZONE-A");
        testZone.setName("Zone A");
        java.lang.reflect.Field zoneIdField = Zone.class.getDeclaredField("id");
        zoneIdField.setAccessible(true);
        zoneIdField.set(testZone, 1L);

        testLocation = new Location();
        testLocation.setCode("LOC-A-01-01");
        testLocation.setZone(testZone);
        testLocation.setAisle("A");
        testLocation.setBay("01");
        testLocation.setLevel("01");
        testLocation.setMaxPallets(2);
        testLocation.setStatus(LocationStatus.AVAILABLE);
        testLocation.setActive(true);

        java.lang.reflect.Field locIdField = Location.class.getDeclaredField("id");
        locIdField.setAccessible(true);
        locIdField.set(testLocation, 1L);

        testLocationDto = new LocationDto(
            1L,
            1L,
            "ZONE-A",
            "LOC-A-01-01",
            "A", "01", "01",
            null, null, null,
            null, null, null, null,
            2,
            "STORAGE",
            "AVAILABLE",
            true
        );

        createRequest = new CreateLocationRequest(
            1L,
            "LOC-B-02-02",
            "B", "02", "02",
            null, null, null,
            null, null, null, null,
            3,
            "STORAGE"
        );

        updateRequest = new UpdateLocationRequest(
            1L,
            "LOC-A-01-02",
            "A", "01", "02",
            null, null, null,
            null, null, null, null,
            2,
            "STORAGE",
            "AVAILABLE",
            true
        );
    }

    @Test
    void shouldGetAllLocations_WhenCalled() throws Exception {
        // Given
        when(locationService.getAll()).thenReturn(List.of(testLocation));
        when(locationMapper.toDto(testLocation)).thenReturn(testLocationDto);

        // When & Then
        mockMvc.perform(get("/api/locations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].code").value("LOC-A-01-01"))
                .andExpect(jsonPath("$[0].aisle").value("A"))
                .andExpect(jsonPath("$[0].bay").value("01"))
                .andExpect(jsonPath("$[0].level").value("01"))
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"));

        verify(locationService).getAll();
        verify(locationMapper).toDto(testLocation);
    }

    @Test
    void shouldGetLocationById_WhenValidId() throws Exception {
        // Given
        when(locationService.getById(1L)).thenReturn(Optional.of(testLocation));
        when(locationMapper.toDto(testLocation)).thenReturn(testLocationDto);

        // When & Then
        mockMvc.perform(get("/api/locations/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.code").value("LOC-A-01-01"));

        verify(locationService).getById(1L);
        verify(locationMapper).toDto(testLocation);
    }

    @Test
    void shouldReturn404_WhenLocationNotFound() throws Exception {
        // Given
        when(locationService.getById(999L)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/locations/999"))
                .andExpect(status().isNotFound());

        verify(locationService).getById(999L);
        verify(locationMapper, never()).toDto(any(Location.class));
    }

    @Test
    void shouldCreateLocation_WhenValidRequest() throws Exception {
        // Given
        Location newLocation = new Location();
        newLocation.setCode("LOC-B-02-02");
        newLocation.setZone(testZone);

        LocationDto newLocationDto = new LocationDto(
            2L, 1L, "ZONE-A", "LOC-B-02-02",
            "B", "02", "02",
            null, null, null, null, null, null, null,
            3, "STORAGE", "AVAILABLE", true
        );

        when(zoneRepository.findById(1L)).thenReturn(Optional.of(testZone));
        when(locationMapper.toEntity(any(CreateLocationRequest.class), eq(testZone)))
            .thenReturn(newLocation);
        when(locationService.create(any(Location.class))).thenReturn(newLocation);
        when(locationMapper.toDto(newLocation)).thenReturn(newLocationDto);

        // When & Then
        mockMvc.perform(post("/api/locations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.code").value("LOC-B-02-02"))
                .andExpect(jsonPath("$.aisle").value("B"));

        verify(zoneRepository).findById(1L);
        verify(locationMapper).toEntity(any(CreateLocationRequest.class), eq(testZone));
        verify(locationService).create(any(Location.class));
    }

    @Test
    void shouldReturn404_WhenCreatingLocationWithInvalidZone() throws Exception {
        // Given
        when(zoneRepository.findById(999L)).thenReturn(Optional.empty());

        CreateLocationRequest invalidRequest = new CreateLocationRequest(
            999L, "LOC-X", null, null, null, null, null, null, null, null, null, null, 1, null
        );

        // When & Then
        mockMvc.perform(post("/api/locations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isNotFound());

        verify(zoneRepository).findById(999L);
        verify(locationService, never()).create(any(Location.class));
    }

    @Test
    void shouldUpdateLocation_WhenValidRequest() throws Exception {
        // Given
        LocationDto updatedDto = new LocationDto(
            1L, 1L, "ZONE-A", "LOC-A-01-02",
            "A", "01", "02",
            null, null, null, null, null, null, null,
            2, "STORAGE", "AVAILABLE", true
        );

        when(locationService.getById(1L)).thenReturn(Optional.of(testLocation));
        when(zoneRepository.findById(1L)).thenReturn(Optional.of(testZone));
        doNothing().when(locationMapper).updateEntity(
            any(Location.class), 
            any(UpdateLocationRequest.class), 
            any(Zone.class)
        );
        when(locationService.update(eq(1L), any(Location.class))).thenReturn(testLocation);
        when(locationMapper.toDto(testLocation)).thenReturn(updatedDto);

        // When & Then
        mockMvc.perform(put("/api/locations/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("LOC-A-01-02"))
                .andExpect(jsonPath("$.level").value("02"));

        verify(locationService).getById(1L);
        verify(zoneRepository).findById(1L);
        verify(locationService).update(eq(1L), any(Location.class));
    }

    @Test
    void shouldReturn404_WhenUpdatingNonexistentLocation() throws Exception {
        // Given
        when(locationService.getById(999L)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(put("/api/locations/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound());

        verify(locationService).getById(999L);
        verify(locationService, never()).update(anyLong(), any(Location.class));
    }

    @Test
    void shouldDeleteLocation_WhenValidId() throws Exception {
        // Given
        doNothing().when(locationService).delete(1L);

        // When & Then
        mockMvc.perform(delete("/api/locations/1"))
                .andExpect(status().isNoContent());

        verify(locationService).delete(1L);
    }

    @Test
    void shouldBlockLocation_WhenValidId() throws Exception {
        // Given
        testLocation.setStatus(LocationStatus.BLOCKED);
        LocationDto blockedDto = new LocationDto(
            1L, 1L, "ZONE-A", "LOC-A-01-01",
            "A", "01", "01",
            null, null, null, null, null, null, null,
            2, "STORAGE", "BLOCKED", true
        );

        when(locationService.blockLocation(1L)).thenReturn(testLocation);
        when(locationMapper.toDto(testLocation)).thenReturn(blockedDto);

        // When & Then
        mockMvc.perform(post("/api/locations/1/block"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("BLOCKED"));

        verify(locationService).blockLocation(1L);
        verify(locationMapper).toDto(testLocation);
    }

    @Test
    void shouldUnblockLocation_WhenValidId() throws Exception {
        // Given
        testLocation.setStatus(LocationStatus.AVAILABLE);
        LocationDto unblockedDto = new LocationDto(
            1L, 1L, "ZONE-A", "LOC-A-01-01",
            "A", "01", "01",
            null, null, null, null, null, null, null,
            2, "STORAGE", "AVAILABLE", true
        );

        when(locationService.unblockLocation(1L)).thenReturn(testLocation);
        when(locationMapper.toDto(testLocation)).thenReturn(unblockedDto);

        // When & Then
        mockMvc.perform(post("/api/locations/1/unblock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("AVAILABLE"));

        verify(locationService).unblockLocation(1L);
        verify(locationMapper).toDto(testLocation);
    }

    @Test
    void shouldReturnEmptyList_WhenNoLocations() throws Exception {
        // Given
        when(locationService.getAll()).thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/locations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(locationService).getAll();
    }
}
