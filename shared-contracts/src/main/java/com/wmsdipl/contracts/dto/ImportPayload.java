package com.wmsdipl.contracts.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ImportPayload(
    @NotBlank String messageId,
    @NotBlank String docNo,
    LocalDate docDate,
    String supplier,
    @NotNull List<Line> lines
) {
    public record Line(
        Integer lineNo,
        String sku,
        String name,
        String uom,
        BigDecimal qtyExpected,
        String packaging,
        String sscc
    ) {}
}
