package com.wmsdipl.core.service;

import com.wmsdipl.contracts.dto.CreateSkuRequest;
import com.wmsdipl.contracts.dto.SkuDto;
import com.wmsdipl.core.domain.Sku;
import com.wmsdipl.core.domain.SkuUnitConfig;
import com.wmsdipl.core.mapper.SkuMapper;
import com.wmsdipl.core.repository.ReceiptLineRepository;
import com.wmsdipl.core.repository.SkuRepository;
import com.wmsdipl.core.repository.SkuUnitConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class SkuServiceTest {

    @Mock
    private SkuRepository skuRepository;

    @Mock
    private SkuMapper skuMapper;

    @Mock
    private SkuUnitConfigRepository skuUnitConfigRepository;

    @Mock
    private ReceiptLineRepository receiptLineRepository;

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
        testSku.setUom("PCS");

        testSkuDto = new SkuDto(1L, "SKU001", "Test SKU", "PCS");
        createRequest = new CreateSkuRequest("SKU001", "Test SKU", "PCS");

        org.mockito.Mockito.lenient().when(skuUnitConfigRepository.findBySkuIdAndIsBaseTrue(anyLong())).thenReturn(Optional.empty());
        org.mockito.Mockito.lenient().when(skuUnitConfigRepository.findBySkuIdOrderByIsBaseDescUnitCodeAsc(anyLong())).thenReturn(List.of());
        org.mockito.Mockito.lenient().when(skuUnitConfigRepository.findBySkuIdAndUnitCodeIgnoreCase(anyLong(), anyString())).thenReturn(Optional.empty());
        org.mockito.Mockito.lenient().when(skuUnitConfigRepository.save(any(SkuUnitConfig.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldFindAllSkus_WhenCalled() {
        when(skuRepository.findAll()).thenReturn(List.of(testSku));
        when(skuMapper.toDto(testSku)).thenReturn(testSkuDto);

        List<SkuDto> result = skuService.findAll();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("SKU001", result.get(0).code());
        verify(skuRepository, times(1)).findAll();
    }

    @Test
    void shouldFindSkuById_WhenValidId() {
        when(skuRepository.findById(1L)).thenReturn(Optional.of(testSku));
        when(skuMapper.toDto(testSku)).thenReturn(testSkuDto);

        SkuDto result = skuService.findById(1L);

        assertNotNull(result);
        assertEquals("SKU001", result.code());
        verify(skuRepository, times(1)).findById(1L);
    }

    @Test
    void shouldThrowException_WhenSkuNotFoundById() {
        when(skuRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> skuService.findById(999L));
        assertEquals(NOT_FOUND, exception.getStatusCode());
        assertTrue(exception.getReason().contains("SKU not found"));
    }

    @Test
    void shouldFindSkuByCode_WhenValidCode() {
        when(skuRepository.findByCode("SKU001")).thenReturn(Optional.of(testSku));
        when(skuMapper.toDto(testSku)).thenReturn(testSkuDto);

        Optional<SkuDto> result = skuService.findByCode("SKU001");

        assertTrue(result.isPresent());
        assertEquals("SKU001", result.get().code());
        verify(skuRepository, times(1)).findByCode("SKU001");
    }

    @Test
    void shouldCreateSku_WhenValidRequest() {
        when(skuRepository.findByCode("SKU001")).thenReturn(Optional.empty());
        when(skuMapper.toEntity(createRequest)).thenReturn(testSku);
        when(skuRepository.save(testSku)).thenReturn(testSku);
        when(skuMapper.toDto(testSku)).thenReturn(testSkuDto);

        SkuDto result = skuService.create(createRequest);

        assertNotNull(result);
        assertEquals("SKU001", result.code());
        verify(skuRepository, times(1)).save(testSku);
    }

    @Test
    void shouldThrowException_WhenDuplicateSkuCode() {
        when(skuRepository.findByCode("SKU001")).thenReturn(Optional.of(testSku));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> skuService.create(createRequest));
        assertEquals(CONFLICT, exception.getStatusCode());
        assertTrue(exception.getReason().contains("already exists"));
        verify(skuRepository, never()).save(any());
    }

    @Test
    void shouldUpdateSku_WhenValidRequest() {
        CreateSkuRequest updateRequest = new CreateSkuRequest("SKU001", "Updated SKU", "KG");
        when(skuRepository.findById(1L)).thenReturn(Optional.of(testSku));
        when(skuRepository.findByCode("SKU001")).thenReturn(Optional.of(testSku));
        when(skuRepository.save(testSku)).thenReturn(testSku);
        when(skuMapper.toDto(testSku)).thenReturn(testSkuDto);

        SkuDto result = skuService.update(1L, updateRequest);

        assertNotNull(result);
        verify(skuMapper, times(1)).updateEntity(testSku, updateRequest);
        verify(skuRepository, times(1)).save(testSku);
    }

    @Test
    void shouldDeleteSku_WhenValidId() {
        when(skuRepository.existsById(1L)).thenReturn(true);

        skuService.delete(1L);

        verify(skuRepository, times(1)).deleteById(1L);
    }

    @Test
    void shouldFindOrCreateExistingSku_WhenSkuExists() {
        when(skuRepository.findByCode("SKU001")).thenReturn(Optional.of(testSku));

        Sku result = skuService.findOrCreate("SKU001", "Test", "PCS");

        assertNotNull(result);
        assertEquals("SKU001", result.getCode());
        verify(skuRepository, never()).save(any());
    }

    @Test
    void shouldCreateNewSku_WhenNotExists() {
        when(skuRepository.findByCode("SKU002")).thenReturn(Optional.empty());
        when(skuRepository.save(any(Sku.class))).thenAnswer(invocation -> {
            Sku sku = invocation.getArgument(0);
            sku.setId(2L);
            return sku;
        });

        Sku result = skuService.findOrCreate("SKU002", "New SKU", "KG");

        assertNotNull(result);
        assertEquals("SKU002", result.getCode());
        assertEquals("New SKU", result.getName());
        assertEquals("KG", result.getUom());
        verify(skuRepository, times(1)).save(any(Sku.class));
    }
}
