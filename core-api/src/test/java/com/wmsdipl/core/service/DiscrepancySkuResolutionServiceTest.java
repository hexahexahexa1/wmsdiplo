package com.wmsdipl.core.service;

import com.wmsdipl.core.domain.Discrepancy;
import com.wmsdipl.core.domain.Pallet;
import com.wmsdipl.core.domain.Receipt;
import com.wmsdipl.core.domain.ReceiptLine;
import com.wmsdipl.core.domain.Sku;
import com.wmsdipl.core.domain.SkuStatus;
import com.wmsdipl.core.domain.SkuUnitConfig;
import com.wmsdipl.core.repository.DiscrepancyRepository;
import com.wmsdipl.core.repository.PalletRepository;
import com.wmsdipl.core.repository.ReceiptLineRepository;
import com.wmsdipl.core.repository.SkuRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class DiscrepancySkuResolutionServiceTest {

    @Mock
    private DiscrepancyRepository discrepancyRepository;
    @Mock
    private SkuRepository skuRepository;
    @Mock
    private ReceiptLineRepository receiptLineRepository;
    @Mock
    private PalletRepository palletRepository;
    @Mock
    private SkuService skuService;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private DiscrepancySkuResolutionService discrepancySkuResolutionService;

    @Test
    void shouldRemapDraftSkuAcrossLinePalletAndDiscrepancy() {
        Receipt receipt = new Receipt();
        receipt.setId(55L);

        ReceiptLine line = new ReceiptLine();
        line.setId(101L);
        line.setReceipt(receipt);
        line.setSkuId(11L);
        line.setUom("PCS");
        line.setQtyExpected(new BigDecimal("5"));

        Discrepancy discrepancy = new Discrepancy();
        discrepancy.setId(201L);
        discrepancy.setReceipt(receipt);
        discrepancy.setLine(line);
        discrepancy.setDraftSkuId(11L);

        Sku source = new Sku();
        source.setId(11L);
        source.setCode("SRC-11");
        source.setStatus(SkuStatus.DRAFT);

        Sku target = new Sku();
        target.setId(22L);
        target.setCode("TRG-22");
        target.setUom("PCS");
        target.setStatus(SkuStatus.ACTIVE);

        Pallet pallet = new Pallet();
        pallet.setId(301L);
        pallet.setSkuId(11L);

        SkuUnitConfig config = new SkuUnitConfig();
        config.setUnitCode("PCS");
        config.setFactorToBase(BigDecimal.ONE);
        config.setUnitsPerPallet(new BigDecimal("10"));

        when(discrepancyRepository.findById(201L)).thenReturn(Optional.of(discrepancy), Optional.of(discrepancy));
        when(skuRepository.findById(11L)).thenReturn(Optional.of(source));
        when(skuRepository.findById(22L)).thenReturn(Optional.of(target));
        when(receiptLineRepository.findByReceipt_IdAndSkuId(55L, 11L)).thenReturn(List.of(line));
        when(discrepancyRepository.findByReceipt_IdAndDraftSkuId(55L, 11L)).thenReturn(List.of(discrepancy));
        when(palletRepository.findByReceipt_IdAndSkuId(55L, 11L)).thenReturn(List.of(pallet));
        when(palletRepository.findByReceipt_IdAndReceiptLine_IdIn(55L, java.util.Set.of(101L))).thenReturn(List.of());
        when(skuService.getActiveUnitConfigOrThrow(22L, "PCS")).thenReturn(config);

        discrepancySkuResolutionService.remapDiscrepancySku(201L, 22L);

        assertEquals(22L, line.getSkuId());
        assertEquals(22L, pallet.getSkuId());
        assertEquals(null, discrepancy.getDraftSkuId());
        verify(receiptLineRepository, times(1)).saveAll(any());
        verify(palletRepository, times(1)).saveAll(any());
        verify(discrepancyRepository, times(1)).saveAll(any());
        verify(auditLogService).logUpdate(
            eq("DISCREPANCY"),
            eq(201L),
            anyString(),
            eq("draftSkuId"),
            eq("11"),
            isNull()
        );
    }

    @Test
    void shouldRemapPalletLinkedToAffectedLine_WhenPalletSkuEqualsOldLineSku() {
        Receipt receipt = new Receipt();
        receipt.setId(55L);

        ReceiptLine line = new ReceiptLine();
        line.setId(101L);
        line.setReceipt(receipt);
        line.setSkuId(309L);
        line.setUom("PCS");
        line.setQtyExpected(new BigDecimal("5"));

        Discrepancy discrepancy = new Discrepancy();
        discrepancy.setId(201L);
        discrepancy.setReceipt(receipt);
        discrepancy.setLine(line);
        discrepancy.setDraftSkuId(11L);

        Sku source = new Sku();
        source.setId(11L);
        source.setCode("SRC-11");
        source.setStatus(SkuStatus.DRAFT);

        Sku target = new Sku();
        target.setId(22L);
        target.setCode("TRG-22");
        target.setUom("PCS");
        target.setStatus(SkuStatus.ACTIVE);

        Pallet pallet = new Pallet();
        pallet.setId(301L);
        pallet.setSkuId(309L);
        pallet.setReceiptLine(line);

        SkuUnitConfig config = new SkuUnitConfig();
        config.setUnitCode("PCS");
        config.setFactorToBase(BigDecimal.ONE);
        config.setUnitsPerPallet(new BigDecimal("10"));

        when(discrepancyRepository.findById(201L)).thenReturn(Optional.of(discrepancy), Optional.of(discrepancy));
        when(skuRepository.findById(11L)).thenReturn(Optional.of(source));
        when(skuRepository.findById(22L)).thenReturn(Optional.of(target));
        when(receiptLineRepository.findByReceipt_IdAndSkuId(55L, 11L)).thenReturn(List.of());
        when(discrepancyRepository.findByReceipt_IdAndDraftSkuId(55L, 11L)).thenReturn(List.of(discrepancy));
        when(palletRepository.findByReceipt_IdAndSkuId(55L, 11L)).thenReturn(List.of());
        when(palletRepository.findByReceipt_IdAndReceiptLine_IdIn(55L, java.util.Set.of(101L))).thenReturn(List.of(pallet));
        when(skuService.getActiveUnitConfigOrThrow(22L, "PCS")).thenReturn(config);

        discrepancySkuResolutionService.remapDiscrepancySku(201L, 22L);

        assertEquals(22L, line.getSkuId());
        assertEquals(22L, pallet.getSkuId());
        verify(palletRepository, times(1)).saveAll(any());
        verify(palletRepository, never()).save(any(Pallet.class));
    }
}
