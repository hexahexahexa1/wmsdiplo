package com.wmsdipl.core.service;

import com.wmsdipl.core.domain.Discrepancy;
import com.wmsdipl.core.domain.Receipt;
import com.wmsdipl.core.domain.ReceiptLine;
import com.wmsdipl.core.domain.Sku;
import com.wmsdipl.core.domain.SkuStatus;
import com.wmsdipl.core.repository.DiscrepancyRepository;
import com.wmsdipl.core.repository.SkuRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReceiptWorkflowBlockerServiceTest {

    @Mock
    private SkuRepository skuRepository;

    @Mock
    private DiscrepancyRepository discrepancyRepository;

    @InjectMocks
    private ReceiptWorkflowBlockerService receiptWorkflowBlockerService;

    @Test
    void shouldBlock_WhenLineReferencesDraftSku() {
        Receipt receipt = new Receipt();
        receipt.setId(1L);
        ReceiptLine line = new ReceiptLine();
        line.setId(10L);
        line.setLineNo(1);
        line.setSkuId(100L);
        receipt.setLines(List.of(line));

        Sku draftSku = new Sku();
        draftSku.setId(100L);
        draftSku.setCode("NEW-100");
        draftSku.setStatus(SkuStatus.DRAFT);

        when(skuRepository.findById(100L)).thenReturn(Optional.of(draftSku));
        when(discrepancyRepository.findByReceipt(receipt)).thenReturn(List.of());

        ReceiptWorkflowBlockedException ex = assertThrows(
            ReceiptWorkflowBlockedException.class,
            () -> receiptWorkflowBlockerService.assertNoSkuStatusBlockers(receipt, "completeReceiving")
        );

        assertEquals(1L, ex.getReceiptId());
        assertEquals("completeReceiving", ex.getOperation());
        assertEquals(1, ex.getBlockers().size());
        assertEquals("LINE_SKU_NOT_ACTIVE", ex.getBlockers().get(0).code());
    }

    @Test
    void shouldIgnoreExcludedLineAndActiveDiscrepancies_WhenNoBlockingSku() {
        Receipt receipt = new Receipt();
        receipt.setId(2L);
        ReceiptLine excludedLine = new ReceiptLine();
        excludedLine.setId(20L);
        excludedLine.setLineNo(2);
        excludedLine.setSkuId(200L);
        excludedLine.setExcludedFromWorkflow(true);
        receipt.setLines(List.of(excludedLine));

        Discrepancy discrepancy = new Discrepancy();
        discrepancy.setId(30L);
        discrepancy.setDraftSkuId(300L);

        Sku activeSku = new Sku();
        activeSku.setId(300L);
        activeSku.setCode("ACTIVE-300");
        activeSku.setStatus(SkuStatus.ACTIVE);

        when(discrepancyRepository.findByReceipt(receipt)).thenReturn(List.of(discrepancy));
        when(skuRepository.findById(300L)).thenReturn(Optional.of(activeSku));

        receiptWorkflowBlockerService.assertNoSkuStatusBlockers(receipt, "startPlacement");
    }
}
