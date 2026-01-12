package com.wmsdipl.core.mapper;

import com.wmsdipl.contracts.dto.CreateReceiptRequest;
import com.wmsdipl.contracts.dto.ReceiptDto;
import com.wmsdipl.contracts.dto.ReceiptLineDto;
import com.wmsdipl.core.domain.Receipt;
import com.wmsdipl.core.domain.ReceiptLine;
import com.wmsdipl.core.domain.ReceiptStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReceiptMapperTest {

    private ReceiptMapper receiptMapper;

    @BeforeEach
    void setUp() {
        receiptMapper = new ReceiptMapper();
    }

    @Test
    void shouldMapReceiptToDto_WithAllFields() {
        // Given
        Receipt receipt = mock(Receipt.class);
        when(receipt.getId()).thenReturn(1L);
        when(receipt.getDocNo()).thenReturn("DOC001");
        when(receipt.getDocDate()).thenReturn(LocalDate.of(2026, 1, 10));
        when(receipt.getSupplier()).thenReturn("Test Supplier");
        when(receipt.getStatus()).thenReturn(ReceiptStatus.DRAFT);
        when(receipt.getMessageId()).thenReturn("MSG001");
        when(receipt.getCreatedAt()).thenReturn(LocalDateTime.of(2026, 1, 10, 10, 0));

        // When
        ReceiptDto dto = receiptMapper.toDto(receipt);

        // Then
        assertNotNull(dto);
        assertEquals(1L, dto.id());
        assertEquals("DOC001", dto.docNo());
        assertEquals(LocalDate.of(2026, 1, 10), dto.docDate());
        assertEquals("Test Supplier", dto.supplier());
        assertEquals("DRAFT", dto.status());
        assertEquals("MSG001", dto.messageId());
        assertEquals(LocalDateTime.of(2026, 1, 10, 10, 0), dto.createdAt());
    }

    @Test
    void shouldMapReceiptLineToDto_WithAllFields() {
        // Given
        ReceiptLine line = mock(ReceiptLine.class);
        when(line.getId()).thenReturn(10L);
        when(line.getLineNo()).thenReturn(1);
        when(line.getSkuId()).thenReturn(100L);
        when(line.getPackagingId()).thenReturn(200L);
        when(line.getUom()).thenReturn("ШТ");
        when(line.getQtyExpected()).thenReturn(BigDecimal.TEN);
        when(line.getSsccExpected()).thenReturn("SSCC001");

        // When
        ReceiptLineDto dto = receiptMapper.toLineDto(line);

        // Then
        assertNotNull(dto);
        assertEquals(10L, dto.id());
        assertEquals(1, dto.lineNo());
        assertEquals(100L, dto.skuId());
        assertEquals(200L, dto.packagingId());
        assertEquals("ШТ", dto.uom());
        assertEquals(BigDecimal.TEN, dto.qtyExpected());
        assertEquals("SSCC001", dto.ssccExpected());
    }

    @Test
    void shouldMapRequestLineToEntity_WithAllFields() {
        // Given
        CreateReceiptRequest.Line lineRequest = new CreateReceiptRequest.Line(
            1,
            100L,
            200L,
            "ШТ",
            BigDecimal.TEN,
            "SSCC001",
            null,  // lotNumberExpected
            null   // expiryDateExpected
        );

        // When
        ReceiptLine entity = receiptMapper.toLine(lineRequest);

        // Then
        assertNotNull(entity);
        assertEquals(1, entity.getLineNo());
        assertEquals(100L, entity.getSkuId());
        assertEquals(200L, entity.getPackagingId());
        assertEquals("ШТ", entity.getUom());
        assertEquals(BigDecimal.TEN, entity.getQtyExpected());
        assertEquals("SSCC001", entity.getSsccExpected());
    }

    @Test
    void shouldMapRequestLineToEntity_WithNullQuantity() {
        // Given
        CreateReceiptRequest.Line lineRequest = new CreateReceiptRequest.Line(
            1,
            100L,
            200L,
            "ШТ",
            null, // null quantity
            "SSCC001",
            null,  // lotNumberExpected
            null   // expiryDateExpected
        );

        // When
        ReceiptLine entity = receiptMapper.toLine(lineRequest);

        // Then
        assertNotNull(entity);
        assertEquals(BigDecimal.ZERO, entity.getQtyExpected()); // Should default to ZERO
    }

    @Test
    void shouldMapReceipt_WithDifferentStatuses() {
        // Test CONFIRMED status
        Receipt receipt = mock(Receipt.class);
        when(receipt.getId()).thenReturn(1L);
        when(receipt.getDocNo()).thenReturn("DOC001");
        when(receipt.getDocDate()).thenReturn(LocalDate.now());
        when(receipt.getSupplier()).thenReturn("Supplier");
        when(receipt.getCreatedAt()).thenReturn(LocalDateTime.now());
        
        when(receipt.getStatus()).thenReturn(ReceiptStatus.CONFIRMED);
        ReceiptDto dtoConfirmed = receiptMapper.toDto(receipt);
        assertEquals("CONFIRMED", dtoConfirmed.status());

        when(receipt.getStatus()).thenReturn(ReceiptStatus.IN_PROGRESS);
        ReceiptDto dtoInProgress = receiptMapper.toDto(receipt);
        assertEquals("IN_PROGRESS", dtoInProgress.status());

        when(receipt.getStatus()).thenReturn(ReceiptStatus.ACCEPTED);
        ReceiptDto dtoAccepted = receiptMapper.toDto(receipt);
        assertEquals("ACCEPTED", dtoAccepted.status());
    }
}
