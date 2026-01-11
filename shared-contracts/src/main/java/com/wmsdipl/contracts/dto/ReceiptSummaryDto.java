package com.wmsdipl.contracts.dto;

import java.math.BigDecimal;
import java.util.List;

public record ReceiptSummaryDto(
        Long receiptId,
        String docNo,
        String supplier,
        String status,
        int totalLines,
        int totalPallets,
        BigDecimal totalQtyExpected,
        BigDecimal totalQtyReceived,
        boolean hasDiscrepancies,
        List<LineSummary> linesSummary
) {
    public record LineSummary(
            Long lineId,
            Integer lineNo,
            Long skuId,
            String uom,
            BigDecimal qtyExpected,
            BigDecimal qtyReceived,
            int palletCount,
            boolean hasDiscrepancy
    ) {}
}
