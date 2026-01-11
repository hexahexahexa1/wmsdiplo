package com.wmsdipl.contracts.dto;

import java.math.BigDecimal;
import java.util.List;

public record ReceiptDiscrepancyDto(
        Long receiptId,
        String docNo,
        boolean hasDiscrepancies,
        List<LineDiscrepancy> lineDiscrepancies
) {
    public record LineDiscrepancy(
            Long lineId,
            Integer lineNo,
            Long skuId,
            String uom,
            BigDecimal qtyExpected,
            BigDecimal qtyReceived,
            BigDecimal difference,
            String discrepancyType,  // "OVER", "UNDER", "MATCH"
            String severity          // "CRITICAL", "WARNING", "INFO"
    ) {}
}
