package com.wmsdipl.contracts.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO для отображения информации о расхождении при приемке.
 */
public record DiscrepancyDto(
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
    String systemCommentKey,
    String systemCommentParams,
    Long draftSkuId,
    Boolean resolved,
    String resolvedBy,
    LocalDateTime resolvedAt,
    LocalDateTime createdAt
) {
}
