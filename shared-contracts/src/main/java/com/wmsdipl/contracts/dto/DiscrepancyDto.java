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
    String type,
    BigDecimal qtyExpected,
    BigDecimal qtyActual,
    String comment,
    Boolean resolved,
    LocalDateTime createdAt
) {
}
