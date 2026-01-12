package com.wmsdipl.contracts.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreateReceiptRequest(
    @NotBlank String docNo,
    LocalDate docDate,
    String supplier,
    Boolean crossDock,
    @NotNull List<Line> lines
) {
    public record Line(
        Integer lineNo,
        Long skuId,
        Long packagingId,
        String uom,
        BigDecimal qtyExpected,
        String ssccExpected,
        String lotNumberExpected,
        LocalDate expiryDateExpected
    ) {}
}
