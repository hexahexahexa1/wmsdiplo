package com.wmsdipl.core.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wmsdipl.contracts.dto.CreateSkuRequest;
import com.wmsdipl.contracts.dto.SkuDto;
import com.wmsdipl.core.service.SkuService;
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
 * REST API tests for SkuController using MockMvc.
 * Tests SKU catalog management endpoints.
 */
@WebMvcTest(SkuController.class)
@AutoConfigureMockMvc(addFilters = false)
class SkuControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SkuService skuService;

    @Test
    void shouldListAllSkus_WhenCalled() throws Exception {
        // Given
        SkuDto sku = new SkuDto(1L, "SKU001", "Product A", "ШТ");
        when(skuService.findAll()).thenReturn(List.of(sku));

        // When & Then
        mockMvc.perform(get("/api/skus"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].code").value("SKU001"))
                .andExpect(jsonPath("$[0].name").value("Product A"))
                .andExpect(jsonPath("$[0].uom").value("ШТ"));

        verify(skuService).findAll();
    }

    @Test
    void shouldGetSkuById_WhenValidId() throws Exception {
        // Given
        SkuDto sku = new SkuDto(1L, "SKU001", "Product A", "ШТ");
        when(skuService.findById(1L)).thenReturn(sku);

        // When & Then
        mockMvc.perform(get("/api/skus/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.code").value("SKU001"))
                .andExpect(jsonPath("$.name").value("Product A"));

        verify(skuService).findById(1L);
    }

    @Test
    void shouldGetSkuByCode_WhenCodeExists() throws Exception {
        // Given
        SkuDto sku = new SkuDto(1L, "SKU001", "Product A", "ШТ");
        when(skuService.findByCode("SKU001")).thenReturn(Optional.of(sku));

        // When & Then
        mockMvc.perform(get("/api/skus/by-code/SKU001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.code").value("SKU001"));

        verify(skuService).findByCode("SKU001");
    }

    @Test
    void shouldReturnNotFound_WhenCodeDoesNotExist() throws Exception {
        // Given
        when(skuService.findByCode("NONEXISTENT")).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/skus/by-code/NONEXISTENT"))
                .andExpect(status().isNotFound());

        verify(skuService).findByCode("NONEXISTENT");
    }

    @Test
    void shouldCreateSku_WhenValidRequest() throws Exception {
        // Given
        SkuDto created = new SkuDto(1L, "SKU001", "Product A", "ШТ");
        when(skuService.create(any(CreateSkuRequest.class))).thenReturn(created);

        String requestBody = """
                {
                    "code": "SKU001",
                    "name": "Product A",
                    "uom": "ШТ"
                }
                """;

        // When & Then
        mockMvc.perform(post("/api/skus")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/skus/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.code").value("SKU001"))
                .andExpect(jsonPath("$.name").value("Product A"));

        verify(skuService).create(any(CreateSkuRequest.class));
    }

    @Test
    void shouldUpdateSku_WhenValidRequest() throws Exception {
        // Given
        SkuDto updated = new SkuDto(1L, "SKU001", "Updated Product", "ШТ");
        when(skuService.update(eq(1L), any(CreateSkuRequest.class))).thenReturn(updated);

        String requestBody = """
                {
                    "code": "SKU001",
                    "name": "Updated Product",
                    "uom": "ШТ"
                }
                """;

        // When & Then
        mockMvc.perform(put("/api/skus/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Updated Product"));

        verify(skuService).update(eq(1L), any(CreateSkuRequest.class));
    }

    @Test
    void shouldDeleteSku_WhenValidId() throws Exception {
        // Given
        doNothing().when(skuService).delete(1L);

        // When & Then
        mockMvc.perform(delete("/api/skus/1"))
                .andExpect(status().isNoContent());

        verify(skuService).delete(1L);
    }

    @Test
    void shouldValidateRequest_WhenCreatingSkuWithInvalidData() throws Exception {
        // Given - empty code (should fail validation)
        String requestBody = """
                {
                    "code": "",
                    "name": "Product A",
                    "uom": "ШТ"
                }
                """;

        // When & Then
        mockMvc.perform(post("/api/skus")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        verify(skuService, never()).create(any());
    }

    @Test
    void shouldValidateRequest_WhenUpdatingSkuWithInvalidData() throws Exception {
        // Given - empty name (should fail validation)
        String requestBody = """
                {
                    "code": "SKU001",
                    "name": "",
                    "uom": "ШТ"
                }
                """;

        // When & Then
        mockMvc.perform(put("/api/skus/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        verify(skuService, never()).update(anyLong(), any());
    }

    @Test
    void shouldReturnEmptyList_WhenNoSkusExist() throws Exception {
        // Given
        when(skuService.findAll()).thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/skus"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        verify(skuService).findAll();
    }
}
