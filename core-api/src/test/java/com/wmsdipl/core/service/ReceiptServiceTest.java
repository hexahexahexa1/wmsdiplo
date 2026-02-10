package com.wmsdipl.core.service;

import com.wmsdipl.contracts.dto.CreateReceiptRequest;
import com.wmsdipl.contracts.dto.CreateReceiptDraftRequest;
import com.wmsdipl.contracts.dto.ImportPayload;
import com.wmsdipl.contracts.dto.ReceiptDiscrepancyDto;
import com.wmsdipl.contracts.dto.ReceiptDto;
import com.wmsdipl.contracts.dto.ReceiptLineDto;
import com.wmsdipl.contracts.dto.ReceiptSummaryDto;
import com.wmsdipl.contracts.dto.UpsertReceiptLineRequest;
import com.wmsdipl.core.domain.Receipt;
import com.wmsdipl.core.domain.ReceiptLine;
import com.wmsdipl.core.domain.ReceiptStatus;
import com.wmsdipl.core.domain.Pallet;
import com.wmsdipl.core.domain.Sku;
import com.wmsdipl.core.domain.SkuStatus;
import com.wmsdipl.core.domain.SkuUnitConfig;
import com.wmsdipl.core.mapper.ReceiptMapper;
import com.wmsdipl.core.repository.ReceiptRepository;
import com.wmsdipl.core.repository.PalletRepository;
import com.wmsdipl.core.repository.SkuRepository;
import com.wmsdipl.core.repository.TaskRepository;
import com.wmsdipl.core.repository.DiscrepancyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

    @Mock
    private SkuService skuService;

    @Mock
    private PalletRepository palletRepository;

    @Mock
    private SkuRepository skuRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private DiscrepancyRepository discrepancyRepository;

    @Mock
    private ReceiptWorkflowBlockerService receiptWorkflowBlockerService;

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
            false,  // crossDock
            null,   // outboundRef
            LocalDateTime.now()
        );

        CreateReceiptRequest.Line line = new CreateReceiptRequest.Line(
            1,
            100L,
            1L,
            "PCS",
            BigDecimal.TEN,
            "SSCC001",
            null,  // lotNumberExpected
            null   // expiryDateExpected
        );

        testCreateRequest = new CreateReceiptRequest(
            "DOC001",
            LocalDate.now(),
            "Test Supplier",
            false,  // crossDock
            null,   // outboundRef
            List.of(line)
        );

        Sku activeSku = new Sku();
        activeSku.setId(100L);
        activeSku.setCode("SKU001");
        activeSku.setName("Test SKU");
        activeSku.setUom("PCS");
        activeSku.setStatus(SkuStatus.ACTIVE);
        lenient().when(skuRepository.findById(anyLong())).thenReturn(Optional.of(activeSku));

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

        ReceiptLineDto lineDto = new ReceiptLineDto(
            1L,
            1,
            100L,
            1L,
            "PCS",
            BigDecimal.TEN,
            BigDecimal.TEN,
            BigDecimal.ONE,
            new BigDecimal("10"),
            "SSCC001",
            null,
            null,
            false,
            null
        );

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
        stubActiveSkuUnitConfig("PCS", new BigDecimal("10"));
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
    void shouldCreateDraftReceipt_WhenValidHeader() {
        CreateReceiptDraftRequest request = new CreateReceiptDraftRequest(
            "RCP-DRAFT-001",
            LocalDate.now(),
            "Supplier A",
            true,
            "OUT-001"
        );

        Receipt saved = new Receipt();
        saved.setDocNo("RCP-DRAFT-001");
        saved.setStatus(ReceiptStatus.DRAFT);
        saved.setCrossDock(true);

        ReceiptDto dto = new ReceiptDto(
            10L,
            "RCP-DRAFT-001",
            LocalDate.now(),
            "Supplier A",
            "DRAFT",
            null,
            true,
            "OUT-001",
            LocalDateTime.now()
        );

        when(receiptRepository.existsByDocNoAndSupplier("RCP-DRAFT-001", "Supplier A")).thenReturn(false);
        when(receiptRepository.save(any(Receipt.class))).thenReturn(saved);
        when(receiptMapper.toDto(any(Receipt.class))).thenReturn(dto);

        ReceiptDto result = receiptService.createDraft(request);

        assertNotNull(result);
        assertEquals("RCP-DRAFT-001", result.docNo());
        assertEquals("DRAFT", result.status());
        verify(receiptRepository).save(any(Receipt.class));
    }

    @Test
    void shouldAddDraftLine_WhenReceiptInDraft() throws Exception {
        stubActiveSkuUnitConfig("BOX", new BigDecimal("12"));

        Receipt draft = new Receipt();
        draft.setStatus(ReceiptStatus.DRAFT);
        setEntityId(draft, 100L);
        draft.setLines(new ArrayList<>());

        UpsertReceiptLineRequest request = new UpsertReceiptLineRequest(
            1,
            500L,
            null,
            "BOX",
            new BigDecimal("2"),
            "SSCC-001",
            null,
            null
        );

        ReceiptLineDto mapped = new ReceiptLineDto(
            200L,
            1,
            500L,
            null,
            "BOX",
            new BigDecimal("2"),
            new BigDecimal("2.000"),
            BigDecimal.ONE,
            new BigDecimal("12.000"),
            "SSCC-001",
            null,
            null,
            false,
            null
        );

        when(receiptRepository.findById(100L)).thenReturn(Optional.of(draft));
        when(receiptRepository.save(any(Receipt.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(receiptMapper.toLineDto(any(ReceiptLine.class))).thenReturn(mapped);

        ReceiptLineDto result = receiptService.addDraftLine(100L, request);

        assertNotNull(result);
        assertEquals(1, draft.getLines().size());
        verify(receiptRepository).save(draft);
        verify(skuService).getActiveUnitConfigOrThrow(500L, "BOX");
    }

    @Test
    void shouldRejectDraftLineUpdate_WhenReceiptIsNotDraft() {
        Receipt nonDraft = new Receipt();
        nonDraft.setStatus(ReceiptStatus.CONFIRMED);
        when(receiptRepository.findById(100L)).thenReturn(Optional.of(nonDraft));

        UpsertReceiptLineRequest request = new UpsertReceiptLineRequest(
            1,
            500L,
            null,
            "PCS",
            BigDecimal.ONE,
            null,
            null,
            null
        );

        assertThrows(ResponseStatusException.class, () -> receiptService.updateDraftLine(100L, 1L, request));
        verify(receiptRepository, never()).save(any(Receipt.class));
    }

    @Test
    void shouldRevalidateLines_WhenConfirmingDraft() throws Exception {
        stubActiveSkuUnitConfig("PCS", new BigDecimal("10"));

        Receipt draft = new Receipt();
        draft.setStatus(ReceiptStatus.DRAFT);
        setEntityId(draft, 1L);
        ReceiptLine line = new ReceiptLine();
        line.setSkuId(42L);
        line.setUom("PCS");
        line.setQtyExpected(new BigDecimal("5"));
        draft.addLine(line);

        when(receiptRepository.findById(1L)).thenReturn(Optional.of(draft));

        receiptService.confirm(1L);

        assertEquals(ReceiptStatus.CONFIRMED, draft.getStatus());
        verify(skuService).getActiveUnitConfigOrThrow(42L, "PCS");
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
            1, 100L, 1L, "PCS", BigDecimal.TEN, "SSCC001", null, null
        );
        CreateReceiptRequest invalidRequest = new CreateReceiptRequest(
            "",
            LocalDate.now(),
            "Test Supplier",
            false,
            null,
            List.of(line)
        );

        // When & Then
        assertThrows(ResponseStatusException.class, () -> receiptService.createManual(invalidRequest));
        verify(receiptRepository, never()).save(any(Receipt.class));
    }

    @Test
    void shouldCreateFromImport_WhenValidPayload() {
        // Given
        stubActiveSkuUnitConfig("PCS", new BigDecimal("10"));
        ImportPayload.Line importLine = new ImportPayload.Line(
            1,
            "SKU001",
            "Product Name",
            "PCS",
            BigDecimal.TEN,
            "PKG001",
            "SSCC001",
            "LOT-001",
            LocalDate.of(2026, 1, 31)
        );
        ImportPayload payload = new ImportPayload(
            "MSG001",
            "DOC001",
            LocalDate.now(),
            "Test Supplier",
            false,
            null,
            List.of(importLine)
        );

        com.wmsdipl.core.domain.Sku mockSku = new com.wmsdipl.core.domain.Sku();
        mockSku.setId(1L);
        mockSku.setCode("SKU001");

        when(receiptRepository.findByMessageId("MSG001")).thenReturn(Optional.empty());
        when(skuService.findOrCreateActive("SKU001", "Product Name", "PCS")).thenReturn(mockSku);
        when(receiptRepository.save(any(Receipt.class))).thenReturn(testReceipt);
        when(receiptMapper.toDto(any(Receipt.class))).thenReturn(testReceiptDto);

        // When
        ReceiptDto result = receiptService.createFromImport(payload);

        // Then
        assertNotNull(result);
        ArgumentCaptor<Receipt> receiptCaptor = ArgumentCaptor.forClass(Receipt.class);
        verify(receiptRepository, times(1)).save(receiptCaptor.capture());
        Receipt savedReceipt = receiptCaptor.getValue();
        assertNotNull(savedReceipt);
        assertEquals(1, savedReceipt.getLines().size());
        ReceiptLine savedLine = savedReceipt.getLines().get(0);
        assertEquals("LOT-001", savedLine.getLotNumberExpected());
        assertEquals(LocalDate.of(2026, 1, 31), savedLine.getExpiryDateExpected());
    }

    @Test
    void shouldReturnExisting_WhenImportWithDuplicateMessageId() {
        // Given
        ImportPayload.Line importLine = new ImportPayload.Line(
            1, "SKU001", "Product Name", "PCS", BigDecimal.TEN, "PKG001", "SSCC001", null, null
        );
        ImportPayload payload = new ImportPayload(
            "MSG001",
            "DOC001",
            LocalDate.now(),
            "Test Supplier",
            false,
            null,
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
            1, "SKU001", "Product Name", "PCS", BigDecimal.TEN, "PKG001", "SSCC001", null, null
        );
        ImportPayload payload = new ImportPayload(
            null,
            "DOC001",
            LocalDate.now(),
            "Test Supplier",
            false,
            null,
            List.of(importLine)
        );

        // When & Then
        assertThrows(ResponseStatusException.class, () -> receiptService.createFromImport(payload));
        verify(receiptRepository, never()).save(any(Receipt.class));
    }

    @Test
    void shouldConfirmReceipt_WhenStatusIsDraft() {
        // Given
        stubActiveSkuUnitConfig("PCS", new BigDecimal("10"));
        ReceiptLine line = new ReceiptLine();
        line.setSkuId(100L);
        line.setUom("PCS");
        line.setQtyExpected(BigDecimal.ONE);
        testReceipt.addLine(line);
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
        when(taskRepository.findByReceiptId(1L)).thenReturn(List.of());
        when(discrepancyRepository.findByReceipt(testReceipt)).thenReturn(List.of());

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

    @Test
    void shouldRejectAccept_WhenOpenTasksExist() throws Exception {
        // Given
        testReceipt.setStatus(ReceiptStatus.CONFIRMED);
        when(receiptRepository.findById(1L)).thenReturn(Optional.of(testReceipt));

        com.wmsdipl.core.domain.Task openTask = new com.wmsdipl.core.domain.Task();
        setEntityId(openTask, 10L);
        openTask.setTaskType(com.wmsdipl.core.domain.TaskType.RECEIVING);
        openTask.setStatus(com.wmsdipl.core.domain.TaskStatus.IN_PROGRESS);
        when(taskRepository.findByReceiptId(1L)).thenReturn(List.of(openTask));

        // When
        ReceiptAcceptBlockedException ex = assertThrows(ReceiptAcceptBlockedException.class, () -> receiptService.accept(1L));

        // Then
        assertEquals(1L, ex.getReceiptId());
        assertEquals(1, ex.getBlockers().size());
        assertEquals(10L, ex.getBlockers().get(0).taskId());
    }

    @Test
    void shouldDeleteDraftReceipt_WhenNoLinkedArtifacts() {
        testReceipt.setStatus(ReceiptStatus.DRAFT);
        when(receiptRepository.findById(1L)).thenReturn(Optional.of(testReceipt));
        when(taskRepository.findByReceiptId(1L)).thenReturn(List.of());
        when(palletRepository.findByReceipt(testReceipt)).thenReturn(List.of());
        when(discrepancyRepository.findByReceipt(testReceipt)).thenReturn(List.of());

        receiptService.deleteReceipt(1L);

        verify(receiptRepository).delete(testReceipt);
    }

    @Test
    void shouldRejectDeleteReceipt_WhenNotDraft() {
        testReceipt.setStatus(ReceiptStatus.ACCEPTED);
        when(receiptRepository.findById(1L)).thenReturn(Optional.of(testReceipt));

        assertThrows(ResponseStatusException.class, () -> receiptService.deleteReceipt(1L));
        verify(receiptRepository, never()).delete(any(Receipt.class));
    }

    @Test
    void shouldRejectDeleteReceipt_WhenTasksExist() {
        testReceipt.setStatus(ReceiptStatus.DRAFT);
        when(receiptRepository.findById(1L)).thenReturn(Optional.of(testReceipt));
        when(taskRepository.findByReceiptId(1L)).thenReturn(List.of(new com.wmsdipl.core.domain.Task()));

        assertThrows(ResponseStatusException.class, () -> receiptService.deleteReceipt(1L));
        verify(receiptRepository, never()).delete(any(Receipt.class));
    }

    @Test
    void shouldGetSummary_WhenReceiptHasLinesAndPallets() throws Exception {
        // Given
        Receipt receipt = createReceiptWithLines();
        List<Pallet> pallets = createPalletsForReceipt(receipt);
        
        when(receiptRepository.findById(1L)).thenReturn(Optional.of(receipt));
        when(palletRepository.findByReceipt(receipt)).thenReturn(pallets);
        
        // When
        ReceiptSummaryDto summary = receiptService.getSummary(1L);
        
        // Then
        assertNotNull(summary);
        assertEquals(1L, summary.receiptId());
        assertEquals(2, summary.totalLines());
        assertEquals(3, summary.totalPallets());
        assertEquals(0, BigDecimal.valueOf(100).compareTo(summary.totalQtyExpected()));
        assertEquals(0, BigDecimal.valueOf(95).compareTo(summary.totalQtyReceived()));
        assertTrue(summary.hasDiscrepancies());
        assertEquals(2, summary.linesSummary().size());
        
        verify(receiptRepository).findById(1L);
        verify(palletRepository).findByReceipt(receipt);
    }

    @Test
    void shouldGetSummary_WhenNoDiscrepancies() throws Exception {
        // Given
        Receipt receipt = createReceiptWithLines();
        List<Pallet> pallets = createMatchingPalletsForReceipt(receipt);
        
        when(receiptRepository.findById(1L)).thenReturn(Optional.of(receipt));
        when(palletRepository.findByReceipt(receipt)).thenReturn(pallets);
        
        // When
        ReceiptSummaryDto summary = receiptService.getSummary(1L);
        
        // Then
        assertNotNull(summary);
        assertFalse(summary.hasDiscrepancies());
        assertEquals(0, BigDecimal.valueOf(100).compareTo(summary.totalQtyExpected()));
        assertEquals(0, BigDecimal.valueOf(100).compareTo(summary.totalQtyReceived()));
        
        summary.linesSummary().forEach(line -> 
            assertFalse(line.hasDiscrepancy(), "Line " + line.lineNo() + " should not have discrepancy")
        );
    }

    @Test
    void shouldGetDiscrepancies_WhenReceiptHasMismatchedQuantities() throws Exception {
        // Given
        Receipt receipt = createReceiptWithLines();
        List<Pallet> pallets = createPalletsForReceipt(receipt);
        
        when(receiptRepository.findById(1L)).thenReturn(Optional.of(receipt));
        when(palletRepository.findByReceipt(receipt)).thenReturn(pallets);
        
        // When
        ReceiptDiscrepancyDto discrepancies = receiptService.getDiscrepancies(1L);
        
        // Then
        assertNotNull(discrepancies);
        assertEquals(1L, discrepancies.receiptId());
        assertTrue(discrepancies.hasDiscrepancies());
        assertEquals(2, discrepancies.lineDiscrepancies().size());
        
        // Check first line (matches)
        ReceiptDiscrepancyDto.LineDiscrepancy line1 = discrepancies.lineDiscrepancies().get(0);
        assertEquals("MATCH", line1.discrepancyType());
        assertEquals("INFO", line1.severity());
        assertEquals(0, BigDecimal.ZERO.compareTo(line1.difference()));
        
        // Check second line (under)
        ReceiptDiscrepancyDto.LineDiscrepancy line2 = discrepancies.lineDiscrepancies().get(1);
        assertEquals("UNDER", line2.discrepancyType());
        assertEquals("CRITICAL", line2.severity());
        assertEquals(0, BigDecimal.valueOf(-5).compareTo(line2.difference()));
        
        verify(receiptRepository).findById(1L);
        verify(palletRepository).findByReceipt(receipt);
    }

    @Test
    void shouldGetDiscrepancies_WhenOverReceived() throws Exception {
        // Given
        Receipt receipt = createReceiptWithLines();
        List<Pallet> pallets = createOverReceivedPalletsForReceipt(receipt);
        
        when(receiptRepository.findById(1L)).thenReturn(Optional.of(receipt));
        when(palletRepository.findByReceipt(receipt)).thenReturn(pallets);
        
        // When
        ReceiptDiscrepancyDto discrepancies = receiptService.getDiscrepancies(1L);
        
        // Then
        assertNotNull(discrepancies);
        assertTrue(discrepancies.hasDiscrepancies());
        
        // Find the over-received line
        ReceiptDiscrepancyDto.LineDiscrepancy overLine = discrepancies.lineDiscrepancies().stream()
            .filter(line -> "OVER".equals(line.discrepancyType()))
            .findFirst()
            .orElseThrow();
        
        assertEquals("OVER", overLine.discrepancyType());
        assertEquals("WARNING", overLine.severity());
        assertTrue(overLine.difference().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void shouldThrowException_WhenReceiptNotFoundForSummary() {
        // Given
        when(receiptRepository.findById(999L)).thenReturn(Optional.empty());
        
        // When & Then
        assertThrows(ResponseStatusException.class, () -> receiptService.getSummary(999L));
        verify(receiptRepository).findById(999L);
    }

    @Test
    void shouldThrowException_WhenReceiptNotFoundForDiscrepancies() {
        // Given
        when(receiptRepository.findById(999L)).thenReturn(Optional.empty());
        
        // When & Then
        assertThrows(ResponseStatusException.class, () -> receiptService.getDiscrepancies(999L));
        verify(receiptRepository).findById(999L);
    }

    @Test
    void shouldHandleEmptyPalletList_WhenGettingSummary() throws Exception {
        // Given
        Receipt receipt = createReceiptWithLines();
        
        when(receiptRepository.findById(1L)).thenReturn(Optional.of(receipt));
        when(palletRepository.findByReceipt(receipt)).thenReturn(List.of());
        
        // When
        ReceiptSummaryDto summary = receiptService.getSummary(1L);
        
        // Then
        assertNotNull(summary);
        assertEquals(0, summary.totalPallets());
        assertEquals(0, BigDecimal.ZERO.compareTo(summary.totalQtyReceived()));
        assertTrue(summary.hasDiscrepancies()); // Expected > 0 but received 0
    }

    // Helper methods for report tests
    
    private Receipt createReceiptWithLines() throws Exception {
        Receipt receipt = new Receipt();
        receipt.setDocNo("RCP-001");
        receipt.setSupplier("SUP-001");
        receipt.setStatus(ReceiptStatus.ACCEPTED);
        
        // Set ID using reflection
        java.lang.reflect.Field idField = Receipt.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(receipt, 1L);
        
        ReceiptLine line1 = createReceiptLine(1L, 1, 1L, BigDecimal.valueOf(50));
        ReceiptLine line2 = createReceiptLine(2L, 2, 2L, BigDecimal.valueOf(50));
        
        receipt.addLine(line1);
        receipt.addLine(line2);
        
        return receipt;
    }
    
    private ReceiptLine createReceiptLine(Long id, int lineNo, Long skuId, BigDecimal qtyExpected) throws Exception {
        ReceiptLine line = new ReceiptLine();
        
        // Set ID using reflection
        java.lang.reflect.Field idField = ReceiptLine.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(line, id);
        
        line.setLineNo(lineNo);
        line.setSkuId(skuId);
        line.setUom("ШТ");
        line.setQtyExpected(qtyExpected);
        
        return line;
    }
    
    private List<Pallet> createPalletsForReceipt(Receipt receipt) {
        List<Pallet> pallets = new ArrayList<>();
        
        ReceiptLine line1 = receipt.getLines().get(0);
        ReceiptLine line2 = receipt.getLines().get(1);
        
        // Line 1: 2 pallets totaling 50 (matches expected)
        pallets.add(createPallet(1L, receipt, line1, BigDecimal.valueOf(25)));
        pallets.add(createPallet(2L, receipt, line1, BigDecimal.valueOf(25)));
        
        // Line 2: 1 pallet with 45 (under by 5)
        pallets.add(createPallet(3L, receipt, line2, BigDecimal.valueOf(45)));
        
        return pallets;
    }
    
    private List<Pallet> createMatchingPalletsForReceipt(Receipt receipt) {
        List<Pallet> pallets = new ArrayList<>();
        
        ReceiptLine line1 = receipt.getLines().get(0);
        ReceiptLine line2 = receipt.getLines().get(1);
        
        // Both lines match exactly
        pallets.add(createPallet(1L, receipt, line1, BigDecimal.valueOf(50)));
        pallets.add(createPallet(2L, receipt, line2, BigDecimal.valueOf(50)));
        
        return pallets;
    }
    
    private List<Pallet> createOverReceivedPalletsForReceipt(Receipt receipt) {
        List<Pallet> pallets = new ArrayList<>();
        
        ReceiptLine line1 = receipt.getLines().get(0);
        ReceiptLine line2 = receipt.getLines().get(1);
        
        // Line 1: over by 10
        pallets.add(createPallet(1L, receipt, line1, BigDecimal.valueOf(60)));
        
        // Line 2: matches
        pallets.add(createPallet(2L, receipt, line2, BigDecimal.valueOf(50)));
        
        return pallets;
    }
    
    private Pallet createPallet(Long id, Receipt receipt, ReceiptLine line, BigDecimal quantity) {
        Pallet pallet = new Pallet();
        try {
            java.lang.reflect.Field idField = Pallet.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(pallet, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        pallet.setCode("PLT-" + id);
        pallet.setReceipt(receipt);
        pallet.setReceiptLine(line);
        pallet.setSkuId(line.getSkuId());
        pallet.setQuantity(quantity);
        pallet.setUom(line.getUom());

        return pallet;
    }

    private void setEntityId(Object entity, Long id) throws Exception {
        java.lang.reflect.Field idField = entity.getClass().getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(entity, id);
    }

    private void stubActiveSkuUnitConfig(String uom, BigDecimal unitsPerPallet) {
        SkuUnitConfig unitConfig = new SkuUnitConfig();
        unitConfig.setUnitCode(uom);
        unitConfig.setFactorToBase(BigDecimal.ONE);
        unitConfig.setUnitsPerPallet(unitsPerPallet);
        unitConfig.setActive(true);
        when(skuService.getActiveUnitConfigOrThrow(anyLong(), anyString())).thenReturn(unitConfig);
    }
}
