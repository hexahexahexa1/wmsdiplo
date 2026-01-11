package com.wmsdipl.contracts.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Запрос на создание расхождения при приемке.
 */
public record CreateDiscrepancyRequest(
    @NotNull(message = "Receipt ID is required")
    Long receiptId,
    
    Long lineId,
    
    @NotBlank(message = "Discrepancy type is required")
    String type,
    
    BigDecimal qtyExpected,
    BigDecimal qtyActual,
    
    String comment
) {
}
