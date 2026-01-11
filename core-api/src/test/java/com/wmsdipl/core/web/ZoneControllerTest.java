package com.wmsdipl.core.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wmsdipl.contracts.dto.CreateZoneRequest;
import com.wmsdipl.contracts.dto.UpdateZoneRequest;
import com.wmsdipl.contracts.dto.ZoneDto;
import com.wmsdipl.core.domain.Zone;
import com.wmsdipl.core.mapper.ZoneMapper;
import com.wmsdipl.core.service.ZoneService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for ZoneController.
 * Tests zone management REST API endpoints with security disabled.
 */
@WebMvcTest(ZoneController.class)
@AutoConfigureMockMvc(addFilters = false)
class ZoneControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ZoneService zoneService;

    @MockBean
    private ZoneMapper zoneMapper;

    private Zone testZone;
    private ZoneDto testZoneDto;
    private CreateZoneRequest createRequest;
    private UpdateZoneRequest updateRequest;

    @BeforeEach
    void setUp() throws Exception {
        testZone = new Zone();
        testZone.setCode("ZONE-A");
        testZone.setName("Zone A");
        testZone.setPriorityRank(1);
        testZone.setDescription("Test zone");
        testZone.setActive(true);

        // Use reflection to set id
        java.lang.reflect.Field idField = Zone.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(testZone, 1L);

        testZoneDto = new ZoneDto(
            1L,
            "ZONE-A",
            "Zone A",
            1,
            "Test zone",
            true
        );

        createRequest = new CreateZoneRequest(
            "ZONE-B",
            "Zone B",
            2,
            "New zone"
        );

        updateRequest = new UpdateZoneRequest(
            "ZONE-A-UPDATED",
            "Zone A Updated",
            3,
            "Updated description",
            false
        );
    }

    @Test
    void shouldGetAllZones_WhenCalled() throws Exception {
        // Given
        when(zoneService.getAll()).thenReturn(List.of(testZone));
        when(zoneMapper.toDto(testZone)).thenReturn(testZoneDto);

        // When & Then
        mockMvc.perform(get("/api/zones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].code").value("ZONE-A"))
                .andExpect(jsonPath("$[0].name").value("Zone A"))
                .andExpect(jsonPath("$[0].priorityRank").value(1))
                .andExpect(jsonPath("$[0].active").value(true));

        verify(zoneService).getAll();
        verify(zoneMapper).toDto(testZone);
    }

    @Test
    void shouldGetZoneById_WhenValidId() throws Exception {
        // Given
        when(zoneService.getById(1L)).thenReturn(Optional.of(testZone));
        when(zoneMapper.toDto(testZone)).thenReturn(testZoneDto);

        // When & Then
        mockMvc.perform(get("/api/zones/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.code").value("ZONE-A"))
                .andExpect(jsonPath("$.name").value("Zone A"));

        verify(zoneService).getById(1L);
        verify(zoneMapper).toDto(testZone);
    }

    @Test
    void shouldReturn404_WhenZoneNotFound() throws Exception {
        // Given
        when(zoneService.getById(999L)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/zones/999"))
                .andExpect(status().isNotFound());

        verify(zoneService).getById(999L);
        verify(zoneMapper, never()).toDto(any(Zone.class));
    }

    @Test
    void shouldCreateZone_WhenValidRequest() throws Exception {
        // Given
        Zone newZone = new Zone();
        newZone.setCode("ZONE-B");
        newZone.setName("Zone B");

        ZoneDto newZoneDto = new ZoneDto(
            2L, "ZONE-B", "Zone B", 2, "New zone", true
        );

        when(zoneMapper.toEntity(any(CreateZoneRequest.class))).thenReturn(newZone);
        when(zoneService.create(any(Zone.class))).thenReturn(newZone);
        when(zoneMapper.toDto(newZone)).thenReturn(newZoneDto);

        // When & Then
        mockMvc.perform(post("/api/zones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.code").value("ZONE-B"))
                .andExpect(jsonPath("$.name").value("Zone B"));

        verify(zoneMapper).toEntity(any(CreateZoneRequest.class));
        verify(zoneService).create(any(Zone.class));
        verify(zoneMapper).toDto(newZone);
    }

    @Test
    void shouldUpdateZone_WhenValidRequest() throws Exception {
        // Given
        ZoneDto updatedDto = new ZoneDto(
            1L,
            "ZONE-A-UPDATED",
            "Zone A Updated",
            3,
            "Updated description",
            false
        );

        when(zoneService.getById(1L)).thenReturn(Optional.of(testZone));
        doNothing().when(zoneMapper).updateEntity(any(Zone.class), any(UpdateZoneRequest.class));
        when(zoneService.update(eq(1L), any(Zone.class))).thenReturn(testZone);
        when(zoneMapper.toDto(testZone)).thenReturn(updatedDto);

        // When & Then
        mockMvc.perform(put("/api/zones/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("ZONE-A-UPDATED"))
                .andExpect(jsonPath("$.name").value("Zone A Updated"))
                .andExpect(jsonPath("$.active").value(false));

        verify(zoneService).getById(1L);
        verify(zoneMapper).updateEntity(any(Zone.class), any(UpdateZoneRequest.class));
        verify(zoneService).update(eq(1L), any(Zone.class));
    }

    @Test
    void shouldDeleteZone_WhenValidId() throws Exception {
        // Given
        doNothing().when(zoneService).delete(1L);

        // When & Then
        mockMvc.perform(delete("/api/zones/1"))
                .andExpect(status().isNoContent());

        verify(zoneService).delete(1L);
    }

    @Test
    void shouldReturnEmptyList_WhenNoZones() throws Exception {
        // Given
        when(zoneService.getAll()).thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/zones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(zoneService).getAll();
    }
}
