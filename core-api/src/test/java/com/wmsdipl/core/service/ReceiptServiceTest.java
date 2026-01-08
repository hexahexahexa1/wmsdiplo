package com.wmsdipl.core.service;

import com.wmsdipl.contracts.dto.CreateReceiptRequest;
import com.wmsdipl.contracts.dto.ImportPayload;
import com.wmsdipl.contracts.dto.ReceiptDto;
import com.wmsdipl.contracts.dto.ReceiptLineDto;
import com.wmsdipl.core.domain.Receipt;
import com.wmsdipl.core.domain.ReceiptLine;
import com.wmsdipl.core.domain.ReceiptStatus;
import com.wmsdipl.core.mapper.ReceiptMapper;
import com.wmsdipl.core.repository.ReceiptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReceiptServiceTest {

    @Mock
    private ReceiptRepository receiptRepository;

    @Mock
    private ReceiptMapper receiptMapper;

    @InjectMocks
    private ReceiptService receiptService;

    private Receipt testReceipt;
    private ReceiptDto testReceiptDto;
    private CreateReceiptRequest testCreateRequest;

    @BeforeEach
    void setUp() {
        testReceipt = new Receipt();
        testReceipt.setDocNo("DOC001");
        testReceipt.setDocDate(LocalDate.now());
        testReceipt.setSupplier("Test Supplier");
        testReceipt.setStatus(ReceiptStatus.DRAFT);

        testReceiptDto = new ReceiptDto(
            1L,
            "DOC001",
            LocalDate.now(),
            "Test Supplier",
            "DRAFT",
            null,
            LocalDateTime.now()
        );

        CreateReceiptRequest.Line line = new CreateReceiptRequest.Line(
            1,
            100L,
            1L,
            "PCS",
            BigDecimal.TEN,
            "SSCC001"
        );

        testCreateRequest = new CreateReceiptRequest(
            "DOC001",
            LocalDate.now(),
            "Test Supplier",
            List.of(line)
        );
    }

    @Test
    void shouldListAllReceipts_WhenCalled() {
        // Given
        List<Receipt> receipts = List.of(testReceipt);
        when(receiptRepository.findAll()).thenReturn(receipts);
        when(receiptMapper.toDto(any(Receipt.class))).thenReturn(testReceiptDto);

        // When
        List<ReceiptDto> result = receiptService.list();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(receiptRepository, times(1)).findAll();
        verify(receiptMapper, times(1)).toDto(any(Receipt.class));
    }

    @Test
    void shouldReturnReceipt_WhenValidIdProvided() {
        // Given
        when(receiptRepository.findById(1L)).thenReturn(Optional.of(testReceipt));
        when(receiptMapper.toDto(testReceipt)).thenReturn(testReceiptDto);

        // When
        ReceiptDto result = receiptService.get(1L);

        // Then
        assertNotNull(result);
        assertEquals("DOC001", result.docNo());
        verify(receiptRepository, times(1)).findById(1L);
    }

    @Test
    void shouldThrowException_WhenReceiptNotFound() {
        // Given
        when(receiptRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResponseStatusException.class, () -> receiptService.get(999L));
        verify(receiptRepository, times(1)).findById(999L);
    }

    @Test
    void shouldListLines_WhenValidReceiptId() {
        // Given
        ReceiptLine line = new ReceiptLine();
        line.setLineNo(1);
        testReceipt.addLine(line);

        ReceiptLineDto lineDto = new ReceiptLineDto(1L, 1, 100L, 1L, "PCS", BigDecimal.TEN, "SSCC001");

        when(receiptRepository.findById(1L)).thenReturn(Optional.of(testReceipt));
        when(receiptMapper.toLineDto(any(ReceiptLine.class))).thenReturn(lineDto);

        // When
        List<ReceiptLineDto> result = receiptService.listLines(1L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(receiptRepository, times(1)).findById(1L);
    }

    @Test
    void shouldCreateManualReceipt_WhenValidRequest() {
        // Given
        ReceiptLine line = new ReceiptLine();
        when(receiptRepository.existsByDocNoAndSupplier(anyString(), anyString())).thenReturn(false);
        when(receiptMapper.toLine(any())).thenReturn(line);
        when(receiptRepository.save(any(Receipt.class))).thenReturn(testReceipt);
        when(receiptMapper.toDto(any(Receipt.class))).thenReturn(testReceiptDto);

        // When
        ReceiptDto result = receiptService.createManual(testCreateRequest);

        // Then
        assertNotNull(result);
        verify(receiptRepository, times(1)).save(any(Receipt.class));
    }

    @Test
    void shouldThrowException_WhenDuplicateDocNoAndSupplier() {
        // Given
        when(receiptRepository.existsByDocNoAndSupplier("DOC001", "Test Supplier")).thenReturn(true);

        // When & Then
        assertThrows(ResponseStatusException.class, () -> receiptService.createManual(testCreateRequest));
        verify(receiptRepository, never()).save(any(Receipt.class));
    }

    @Test
    void shouldThrowException_WhenDocNoIsBlank() {
        // Given
        CreateReceiptRequest.Line line = new CreateReceiptRequest.Line(
            1, 100L, 1L, "PCS", BigDecimal.TEN, "SSCC001"
        );
        CreateReceiptRequest invalidRequest = new CreateReceiptRequest(
            "",
            LocalDate.now(),
            "Test Supplier",
            List.of(line)
        );

        // When & Then
        assertThrows(ResponseStatusException.class, () -> receiptService.createManual(invalidRequest));
        verify(receiptRepository, never()).save(any(Receipt.class));
    }

    @Test
    void shouldCreateFromImport_WhenValidPayload() {
        // Given
        ImportPayload.Line importLine = new ImportPayload.Line(
            1, "SKU001", "Product Name", "PCS", BigDecimal.TEN, "PKG001", "SSCC001"
        );
        ImportPayload payload = new ImportPayload(
            "MSG001",
            "DOC001",
            LocalDate.now(),
            "Test Supplier",
            List.of(importLine)
        );

        when(receiptRepository.findByMessageId("MSG001")).thenReturn(Optional.empty());
        when(receiptRepository.save(any(Receipt.class))).thenReturn(testReceipt);
        when(receiptMapper.toDto(any(Receipt.class))).thenReturn(testReceiptDto);

        // When
        ReceiptDto result = receiptService.createFromImport(payload);

        // Then
        assertNotNull(result);
        verify(receiptRepository, times(1)).save(any(Receipt.class));
    }

    @Test
    void shouldReturnExisting_WhenImportWithDuplicateMessageId() {
        // Given
        ImportPayload.Line importLine = new ImportPayload.Line(
            1, "SKU001", "Product Name", "PCS", BigDecimal.TEN, "PKG001", "SSCC001"
        );
        ImportPayload payload = new ImportPayload(
            "MSG001",
            "DOC001",
            LocalDate.now(),
            "Test Supplier",
            List.of(importLine)
        );

        when(receiptRepository.findByMessageId("MSG001")).thenReturn(Optional.of(testReceipt));
        when(receiptMapper.toDto(testReceipt)).thenReturn(testReceiptDto);

        // When
        ReceiptDto result = receiptService.createFromImport(payload);

        // Then
        assertNotNull(result);
        verify(receiptRepository, never()).save(any(Receipt.class));
    }

    @Test
    void shouldThrowException_WhenImportWithoutMessageId() {
        // Given
        ImportPayload.Line importLine = new ImportPayload.Line(
            1, "SKU001", "Product Name", "PCS", BigDecimal.TEN, "PKG001", "SSCC001"
        );
        ImportPayload payload = new ImportPayload(
            null,
            "DOC001",
            LocalDate.now(),
            "Test Supplier",
            List.of(importLine)
        );

        // When & Then
        assertThrows(ResponseStatusException.class, () -> receiptService.createFromImport(payload));
        verify(receiptRepository, never()).save(any(Receipt.class));
    }

    @Test
    void shouldConfirmReceipt_WhenStatusIsDraft() {
        // Given
        when(receiptRepository.findById(1L)).thenReturn(Optional.of(testReceipt));

        // When
        receiptService.confirm(1L);

        // Then
        assertEquals(ReceiptStatus.CONFIRMED, testReceipt.getStatus());
        verify(receiptRepository, times(1)).findById(1L);
    }

    @Test
    void shouldThrowException_WhenConfirmNonDraft() {
        // Given
        testReceipt.setStatus(ReceiptStatus.CONFIRMED);
        when(receiptRepository.findById(1L)).thenReturn(Optional.of(testReceipt));

        // When & Then
        assertThrows(ResponseStatusException.class, () -> receiptService.confirm(1L));
    }

    @Test
    void shouldAcceptReceipt_WhenStatusIsConfirmed() {
        // Given
        testReceipt.setStatus(ReceiptStatus.CONFIRMED);
        when(receiptRepository.findById(1L)).thenReturn(Optional.of(testReceipt));

        // When
        receiptService.accept(1L);

        // Then
        assertEquals(ReceiptStatus.ACCEPTED, testReceipt.getStatus());
        verify(receiptRepository, times(1)).findById(1L);
    }

    @Test
    void shouldThrowException_WhenAcceptNonConfirmed() {
        // Given
        when(receiptRepository.findById(1L)).thenReturn(Optional.of(testReceipt));

        // When & Then
        assertThrows(ResponseStatusException.class, () -> receiptService.accept(1L));
    }
}
