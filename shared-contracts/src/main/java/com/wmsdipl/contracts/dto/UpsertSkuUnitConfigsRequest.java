package com.wmsdipl.contracts.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record UpsertSkuUnitConfigsRequest(
    @NotEmpty List<@Valid Config> configs
) {
    public record Config(
        Long id,
        @NotBlank String unitCode,
        @NotNull @DecimalMin(value = "0.000001", inclusive = true) BigDecimal factorToBase,
        @NotNull @DecimalMin(value = "0.001", inclusive = true) BigDecimal unitsPerPallet,
        @NotNull Boolean isBase,
        @NotNull Boolean active
    ) {
    }
}
