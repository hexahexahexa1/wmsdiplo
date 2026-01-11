package com.wmsdipl.core.service;

import com.wmsdipl.contracts.dto.CreateSkuRequest;
import com.wmsdipl.contracts.dto.SkuDto;
import com.wmsdipl.core.domain.Sku;
import com.wmsdipl.core.mapper.SkuMapper;
import com.wmsdipl.core.repository.SkuRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class SkuServiceTest {

    @Mock
    private SkuRepository skuRepository;

    @Mock
    private SkuMapper skuMapper;

    @InjectMocks
    private SkuService skuService;

    private Sku testSku;
    private SkuDto testSkuDto;
    private CreateSkuRequest createRequest;

    @BeforeEach
    void setUp() {
        testSku = new Sku();
        testSku.setId(1L);
        testSku.setCode("SKU001");
        testSku.setName("Test SKU");
        testSku.setUom("ШТ");

        testSkuDto = new SkuDto(1L, "SKU001", "Test SKU", "ШТ");
        createRequest = new CreateSkuRequest("SKU001", "Test SKU", "ШТ");
    }

    @Test
    void shouldFindAllSkus_WhenCalled() {
        // Given
        when(skuRepository.findAll()).thenReturn(List.of(testSku));
        when(skuMapper.toDto(testSku)).thenReturn(testSkuDto);

        // When
        List<SkuDto> result = skuService.findAll();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("SKU001", result.get(0).code());
        verify(skuRepository, times(1)).findAll();
    }

    @Test
    void shouldFindSkuById_WhenValidId() {
        // Given
        when(skuRepository.findById(1L)).thenReturn(Optional.of(testSku));
        when(skuMapper.toDto(testSku)).thenReturn(testSkuDto);

        // When
        SkuDto result = skuService.findById(1L);

        // Then
        assertNotNull(result);
        assertEquals("SKU001", result.code());
        verify(skuRepository, times(1)).findById(1L);
    }

    @Test
    void shouldThrowException_WhenSkuNotFoundById() {
        // Given
        when(skuRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> skuService.findById(999L));
        assertEquals(NOT_FOUND, exception.getStatusCode());
        assertTrue(exception.getReason().contains("SKU not found"));
    }

    @Test
    void shouldFindSkuByCode_WhenValidCode() {
        // Given
        when(skuRepository.findByCode("SKU001")).thenReturn(Optional.of(testSku));
        when(skuMapper.toDto(testSku)).thenReturn(testSkuDto);

        // When
        Optional<SkuDto> result = skuService.findByCode("SKU001");

        // Then
        assertTrue(result.isPresent());
        assertEquals("SKU001", result.get().code());
        verify(skuRepository, times(1)).findByCode("SKU001");
    }

    @Test
    void shouldCreateSku_WhenValidRequest() {
        // Given
        when(skuRepository.findByCode("SKU001")).thenReturn(Optional.empty());
        when(skuMapper.toEntity(createRequest)).thenReturn(testSku);
        when(skuRepository.save(testSku)).thenReturn(testSku);
        when(skuMapper.toDto(testSku)).thenReturn(testSkuDto);

        // When
        SkuDto result = skuService.create(createRequest);

        // Then
        assertNotNull(result);
        assertEquals("SKU001", result.code());
        verify(skuRepository, times(1)).save(testSku);
    }

    @Test
    void shouldThrowException_WhenDuplicateSkuCode() {
        // Given
        when(skuRepository.findByCode("SKU001")).thenReturn(Optional.of(testSku));

        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> skuService.create(createRequest));
        assertEquals(CONFLICT, exception.getStatusCode());
        assertTrue(exception.getReason().contains("already exists"));
        verify(skuRepository, never()).save(any());
    }

    @Test
    void shouldUpdateSku_WhenValidRequest() {
        // Given
        CreateSkuRequest updateRequest = new CreateSkuRequest("SKU001", "Updated SKU", "КГ");
        when(skuRepository.findById(1L)).thenReturn(Optional.of(testSku));
        when(skuRepository.findByCode("SKU001")).thenReturn(Optional.of(testSku));
        when(skuRepository.save(testSku)).thenReturn(testSku);
        when(skuMapper.toDto(testSku)).thenReturn(testSkuDto);

        // When
        SkuDto result = skuService.update(1L, updateRequest);

        // Then
        assertNotNull(result);
        verify(skuMapper, times(1)).updateEntity(testSku, updateRequest);
        verify(skuRepository, times(1)).save(testSku);
    }

    @Test
    void shouldDeleteSku_WhenValidId() {
        // Given
        when(skuRepository.existsById(1L)).thenReturn(true);

        // When
        skuService.delete(1L);

        // Then
        verify(skuRepository, times(1)).deleteById(1L);
    }

    @Test
    void shouldFindOrCreateExistingSku_WhenSkuExists() {
        // Given
        when(skuRepository.findByCode("SKU001")).thenReturn(Optional.of(testSku));

        // When
        Sku result = skuService.findOrCreate("SKU001", "Test", "ШТ");

        // Then
        assertNotNull(result);
        assertEquals("SKU001", result.getCode());
        verify(skuRepository, never()).save(any());
    }

    @Test
    void shouldCreateNewSku_WhenNotExists() {
        // Given
        when(skuRepository.findByCode("SKU002")).thenReturn(Optional.empty());
        when(skuRepository.save(any(Sku.class))).thenAnswer(invocation -> {
            Sku sku = invocation.getArgument(0);
            sku.setId(2L);
            return sku;
        });

        // When
        Sku result = skuService.findOrCreate("SKU002", "New SKU", "КГ");

        // Then
        assertNotNull(result);
        assertEquals("SKU002", result.getCode());
        assertEquals("New SKU", result.getName());
        assertEquals("КГ", result.getUom());
        verify(skuRepository, times(1)).save(any(Sku.class));
    }
}
