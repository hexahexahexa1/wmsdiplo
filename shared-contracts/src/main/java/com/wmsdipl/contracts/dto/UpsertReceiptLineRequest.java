package com.wmsdipl.contracts.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpsertReceiptLineRequest(
    Integer lineNo,
    @NotNull Long skuId,
    Long packagingId,
    String uom,
    @NotNull @DecimalMin(value = "0.001", inclusive = true) BigDecimal qtyExpected,
    String ssccExpected,
    String lotNumberExpected,
    LocalDate expiryDateExpected
) {
}
