package com.wmsdipl.contracts.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreateReceiptRequest(
    @NotBlank String docNo,
    LocalDate docDate,
    String supplier,
    Boolean crossDock,
    @NotEmpty List<@Valid Line> lines
) {
    public record Line(
        Integer lineNo,
        @NotNull Long skuId,
        Long packagingId,
        String uom,
        @NotNull @DecimalMin(value = "0.001", inclusive = true) BigDecimal qtyExpected,
        String ssccExpected,
        String lotNumberExpected,
        LocalDate expiryDateExpected
    ) {}
}
