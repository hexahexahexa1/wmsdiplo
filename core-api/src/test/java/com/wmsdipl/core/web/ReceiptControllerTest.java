package com.wmsdipl.core.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wmsdipl.contracts.dto.CreateReceiptRequest;
import com.wmsdipl.contracts.dto.ReceiptDiscrepancyDto;
import com.wmsdipl.contracts.dto.ReceiptDto;
import com.wmsdipl.contracts.dto.ReceiptLineDto;
import com.wmsdipl.contracts.dto.ReceiptSummaryDto;
import com.wmsdipl.core.domain.Receipt;
import com.wmsdipl.core.repository.ReceiptRepository;
import com.wmsdipl.core.service.CsvExportService;
import com.wmsdipl.core.service.ReceiptService;
import com.wmsdipl.core.service.workflow.PlacementWorkflowService;
import com.wmsdipl.core.service.workflow.ReceivingWorkflowService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * REST API tests for ReceiptController using MockMvc.
 * Tests receipt workflow endpoints and data transformation.
 */
@WebMvcTest(ReceiptController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReceiptControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReceiptService receiptService;

    @MockBean
    private ReceivingWorkflowService receivingWorkflowService;

    @MockBean
    private PlacementWorkflowService placementWorkflowService;

    @MockBean
    private CsvExportService csvExportService;

    @MockBean
    private ReceiptRepository receiptRepository;

    @Test
    void shouldListAllReceipts_WhenCalled() throws Exception {
        // Given
        ReceiptDto receipt = createMockReceiptDto(1L, "RCP-001");
        when(receiptService.list()).thenReturn(List.of(receipt));

        // When & Then
        mockMvc.perform(get("/api/receipts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].docNo").value("RCP-001"));
    }

    @Test
    void shouldGetReceiptById_WhenValidId() throws Exception {
        // Given
        ReceiptDto receipt = createMockReceiptDto(1L, "RCP-001");
        when(receiptService.get(1L)).thenReturn(receipt);

        // When & Then
        mockMvc.perform(get("/api/receipts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.docNo").value("RCP-001"));

        verify(receiptService).get(1L);
    }

    @Test
    void shouldGetReceiptLines_WhenValidId() throws Exception {
        // Given
        ReceiptLineDto line = new ReceiptLineDto(
                1L,              // id
                1,               // lineNo (Integer, not Long)
                1L,              // skuId
                null,            // packagingId
                "ШТ",            // uom
                BigDecimal.TEN,  // qtyExpected
                null,            // ssccExpected
                null,            // lotNumberExpected
                null             // expiryDateExpected
        );
        when(receiptService.listLines(1L)).thenReturn(List.of(line));

        // When & Then
        mockMvc.perform(get("/api/receipts/1/lines"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].lineNo").value(1))
                .andExpect(jsonPath("$[0].qtyExpected").value(10));

        verify(receiptService).listLines(1L);
    }

    @Test
    void shouldCreateReceipt_WhenValidRequest() throws Exception {
        // Given
        ReceiptDto created = createMockReceiptDto(1L, "RCP-001");
        when(receiptService.createManual(any(CreateReceiptRequest.class))).thenReturn(created);

        String requestBody = """
                {
                    "docNo": "RCP-001",
                    "docDate": "2026-01-10",
                    "supplier": "SUP-001",
                    "lines": [
                        {
                            "lineNo": 1,
                            "skuId": 1,
                            "qtyExpected": 100
                        }
                    ]
                }
                """;

        // When & Then
        mockMvc.perform(post("/api/receipts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.docNo").value("RCP-001"));

        verify(receiptService).createManual(any(CreateReceiptRequest.class));
    }

    @Test
    void shouldConfirmReceipt_WhenValidId() throws Exception {
        // Given
        doNothing().when(receiptService).confirm(1L);

        // When & Then
        mockMvc.perform(post("/api/receipts/1/confirm"))
                .andExpect(status().isAccepted());

        verify(receiptService).confirm(1L);
    }

    @Test
    void shouldStartReceiving_WhenValidId() throws Exception {
        // Given
        doNothing().when(receivingWorkflowService).startReceiving(1L);

        // When & Then
        mockMvc.perform(post("/api/receipts/1/start-receiving"))
                .andExpect(status().isAccepted());

        verify(receivingWorkflowService).startReceiving(1L);
    }

    @Test
    void shouldCompleteReceiving_WhenValidId() throws Exception {
        // Given
        Receipt receipt = mock(Receipt.class);
        when(receivingWorkflowService.completeReceiving(1L)).thenReturn(receipt);

        // When & Then
        mockMvc.perform(post("/api/receipts/1/complete-receiving"))
                .andExpect(status().isAccepted());

        verify(receivingWorkflowService).completeReceiving(1L);
    }

    @Test
    void shouldAcceptReceipt_WhenValidId() throws Exception {
        // Given
        Receipt receipt = mock(Receipt.class);
        when(receivingWorkflowService.completeReceiving(1L)).thenReturn(receipt);

        // When & Then
        mockMvc.perform(post("/api/receipts/1/accept"))
                .andExpect(status().isAccepted());

        verify(receivingWorkflowService).completeReceiving(1L);
    }

    @Test
    void shouldResolvePending_WhenValidId() throws Exception {
        // Given
        Receipt receipt = mock(Receipt.class);
        when(receivingWorkflowService.resolveAndContinue(1L)).thenReturn(receipt);

        // When & Then
        mockMvc.perform(post("/api/receipts/1/resolve-pending"))
                .andExpect(status().isAccepted());

        verify(receivingWorkflowService).resolveAndContinue(1L);
    }

    @Test
    void shouldCancelReceipt_WhenValidId() throws Exception {
        // Given
        Receipt receipt = mock(Receipt.class);
        when(receivingWorkflowService.cancel(1L)).thenReturn(receipt);

        // When & Then
        mockMvc.perform(post("/api/receipts/1/cancel"))
                .andExpect(status().isAccepted());

        verify(receivingWorkflowService).cancel(1L);
    }

    @Test
    void shouldStartPlacement_WhenValidId() throws Exception {
        // Given
        doNothing().when(placementWorkflowService).startPlacement(1L);

        // When & Then
        mockMvc.perform(post("/api/receipts/1/start-placement"))
                .andExpect(status().isAccepted());

        verify(placementWorkflowService).startPlacement(1L);
    }

    @Test
    void shouldCompletePlacement_WhenValidId() throws Exception {
        // Given
        doNothing().when(placementWorkflowService).completePlacement(1L);

        // When & Then
        mockMvc.perform(post("/api/receipts/1/complete-placement"))
                .andExpect(status().isAccepted());

        verify(placementWorkflowService).completePlacement(1L);
    }

    @Test
    void shouldValidateRequest_WhenCreatingReceiptWithInvalidData() throws Exception {
        // Given - empty docNo (should fail validation)
        String requestBody = """
                {
                    "docNo": "",
                    "supplier": "SUP-001",
                    "lines": []
                }
                """;

        // When & Then
        mockMvc.perform(post("/api/receipts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        verify(receiptService, never()).createManual(any());
    }

    @Test
    void shouldGetReceiptSummary_WhenValidId() throws Exception {
        // Given
        ReceiptSummaryDto summary = createMockSummaryDto(1L);
        when(receiptService.getSummary(1L)).thenReturn(summary);

        // When & Then
        mockMvc.perform(get("/api/receipts/1/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receiptId").value(1))
                .andExpect(jsonPath("$.docNo").value("RCP-001"))
                .andExpect(jsonPath("$.totalLines").value(2))
                .andExpect(jsonPath("$.totalPallets").value(3))
                .andExpect(jsonPath("$.totalQtyExpected").value(100))
                .andExpect(jsonPath("$.totalQtyReceived").value(95))
                .andExpect(jsonPath("$.hasDiscrepancies").value(true))
                .andExpect(jsonPath("$.linesSummary[0].lineNo").value(1))
                .andExpect(jsonPath("$.linesSummary[0].qtyExpected").value(50))
                .andExpect(jsonPath("$.linesSummary[0].qtyReceived").value(50))
                .andExpect(jsonPath("$.linesSummary[0].hasDiscrepancy").value(false));

        verify(receiptService).getSummary(1L);
    }

    @Test
    void shouldGetReceiptDiscrepancies_WhenValidId() throws Exception {
        // Given
        ReceiptDiscrepancyDto discrepancy = createMockDiscrepancyDto(1L);
        when(receiptService.getDiscrepancies(1L)).thenReturn(discrepancy);

        // When & Then
        mockMvc.perform(get("/api/receipts/1/discrepancies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receiptId").value(1))
                .andExpect(jsonPath("$.docNo").value("RCP-001"))
                .andExpect(jsonPath("$.hasDiscrepancies").value(true))
                .andExpect(jsonPath("$.lineDiscrepancies[0].lineNo").value(1))
                .andExpect(jsonPath("$.lineDiscrepancies[0].discrepancyType").value("MATCH"))
                .andExpect(jsonPath("$.lineDiscrepancies[0].severity").value("INFO"))
                .andExpect(jsonPath("$.lineDiscrepancies[1].lineNo").value(2))
                .andExpect(jsonPath("$.lineDiscrepancies[1].discrepancyType").value("UNDER"))
                .andExpect(jsonPath("$.lineDiscrepancies[1].severity").value("CRITICAL"))
                .andExpect(jsonPath("$.lineDiscrepancies[1].difference").value(-5));

        verify(receiptService).getDiscrepancies(1L);
    }

    @Test
    void shouldReturnSummaryWithNoDiscrepancies_WhenAllQuantitiesMatch() throws Exception {
        // Given - summary with no discrepancies
        ReceiptSummaryDto.LineSummary line1 = new ReceiptSummaryDto.LineSummary(
                1L, 1, 1L, "ШТ", 
                BigDecimal.valueOf(50), BigDecimal.valueOf(50), 
                2, false
        );
        
        ReceiptSummaryDto summary = new ReceiptSummaryDto(
                1L, "RCP-001", "SUP-001", "ACCEPTED",
                1, 2, 
                BigDecimal.valueOf(50), BigDecimal.valueOf(50),
                false, List.of(line1)
        );
        
        when(receiptService.getSummary(1L)).thenReturn(summary);

        // When & Then
        mockMvc.perform(get("/api/receipts/1/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasDiscrepancies").value(false))
                .andExpect(jsonPath("$.totalQtyExpected").value(50))
                .andExpect(jsonPath("$.totalQtyReceived").value(50));

        verify(receiptService).getSummary(1L);
    }

    // Helper methods

    private ReceiptDto createMockReceiptDto(Long id, String docNo) {
        return new ReceiptDto(
                id,                     // id
                docNo,                  // docNo
                LocalDate.now(),        // docDate (LocalDate, not String)
                "SUP-001",              // supplier
                "DRAFT",                // status (String)
                null,                   // messageId
                false,                  // crossDock
                LocalDateTime.now()     // createdAt
        );
    }

    private ReceiptSummaryDto createMockSummaryDto(Long id) {
        ReceiptSummaryDto.LineSummary line1 = new ReceiptSummaryDto.LineSummary(
                1L, 1, 1L, "ШТ", 
                BigDecimal.valueOf(50), BigDecimal.valueOf(50), 
                2, false
        );
        
        ReceiptSummaryDto.LineSummary line2 = new ReceiptSummaryDto.LineSummary(
                2L, 2, 2L, "ШТ", 
                BigDecimal.valueOf(50), BigDecimal.valueOf(45), 
                1, true
        );
        
        return new ReceiptSummaryDto(
                id, "RCP-001", "SUP-001", "ACCEPTED",
                2, 3, 
                BigDecimal.valueOf(100), BigDecimal.valueOf(95),
                true, List.of(line1, line2)
        );
    }

    private ReceiptDiscrepancyDto createMockDiscrepancyDto(Long id) {
        ReceiptDiscrepancyDto.LineDiscrepancy line1 = new ReceiptDiscrepancyDto.LineDiscrepancy(
                1L, 1, 1L, "ШТ",
                BigDecimal.valueOf(50), BigDecimal.valueOf(50),
                BigDecimal.ZERO, "MATCH", "INFO"
        );
        
        ReceiptDiscrepancyDto.LineDiscrepancy line2 = new ReceiptDiscrepancyDto.LineDiscrepancy(
                2L, 2, 2L, "ШТ",
                BigDecimal.valueOf(50), BigDecimal.valueOf(45),
                BigDecimal.valueOf(-5), "UNDER", "CRITICAL"
        );
        
        return new ReceiptDiscrepancyDto(
                id, "RCP-001", true, List.of(line1, line2)
        );
    }
}
