package com.wmsdipl.desktop.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Discrepancy(
    Long id,
    Long receiptId,
    String receiptDocNo,
    Long lineId,
    Integer lineNo,
    Long taskId,
    Long palletId,
    Long skuId,
    String operator,
    String type,
    BigDecimal qtyExpected,
    BigDecimal qtyActual,
    String comment,
    Boolean resolved,
    String resolvedBy,
    LocalDateTime resolvedAt,
    LocalDateTime createdAt
) {
}
