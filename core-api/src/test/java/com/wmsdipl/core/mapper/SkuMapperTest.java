package com.wmsdipl.core.mapper;

import com.wmsdipl.contracts.dto.CreateSkuRequest;
import com.wmsdipl.contracts.dto.SkuDto;
import com.wmsdipl.core.domain.Sku;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SkuMapperTest {

    private SkuMapper skuMapper;

    @BeforeEach
    void setUp() {
        skuMapper = new SkuMapper();
    }

    @Test
    void shouldMapSkuToDto_WithAllFields() {
        // Given
        Sku sku = new Sku();
        sku.setId(1L);
        sku.setCode("SKU001");
        sku.setName("Test Product");
        sku.setUom("ШТ");

        // When
        SkuDto dto = skuMapper.toDto(sku);

        // Then
        assertNotNull(dto);
        assertEquals(1L, dto.id());
        assertEquals("SKU001", dto.code());
        assertEquals("Test Product", dto.name());
        assertEquals("ШТ", dto.uom());
    }

    @Test
    void shouldReturnNull_WhenSkuIsNull() {
        // When
        SkuDto dto = skuMapper.toDto(null);

        // Then
        assertNull(dto);
    }

    @Test
    void shouldMapRequestToEntity_WithAllFields() {
        // Given
        CreateSkuRequest request = new CreateSkuRequest("SKU001", "Test Product", "ШТ");

        // When
        Sku entity = skuMapper.toEntity(request);

        // Then
        assertNotNull(entity);
        assertEquals("SKU001", entity.getCode());
        assertEquals("Test Product", entity.getName());
        assertEquals("ШТ", entity.getUom());
        assertNull(entity.getId()); // ID not set from request
    }

    @Test
    void shouldReturnNull_WhenRequestIsNull() {
        // When
        Sku entity = skuMapper.toEntity(null);

        // Then
        assertNull(entity);
    }

    @Test
    void shouldUpdateEntity_WithRequestData() {
        // Given
        Sku sku = new Sku();
        sku.setId(1L);
        sku.setCode("SKU001");
        sku.setName("Test Product");
        sku.setUom("ШТ");

        CreateSkuRequest updateRequest = new CreateSkuRequest("SKU002", "Updated Product", "КГ");

        // When
        skuMapper.updateEntity(sku, updateRequest);

        // Then
        assertEquals("SKU002", sku.getCode());
        assertEquals("Updated Product", sku.getName());
        assertEquals("КГ", sku.getUom());
        assertEquals(1L, sku.getId()); // ID should remain unchanged
    }

    @Test
    void shouldNotUpdateEntity_WhenSkuIsNull() {
        // Given
        CreateSkuRequest updateRequest = new CreateSkuRequest("SKU002", "Updated", "КГ");

        // When & Then (should not throw exception)
        assertDoesNotThrow(() -> skuMapper.updateEntity(null, updateRequest));
    }

    @Test
    void shouldNotUpdateEntity_WhenRequestIsNull() {
        // Given
        Sku sku = new Sku();
        sku.setId(1L);
        sku.setCode("SKU001");
        sku.setName("Test Product");
        sku.setUom("ШТ");

        // When
        skuMapper.updateEntity(sku, null);

        // Then - Verify entity unchanged
        assertEquals("SKU001", sku.getCode());
        assertEquals("Test Product", sku.getName());
    }
}
