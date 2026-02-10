package com.wmsdipl.contracts.dto;

import jakarta.validation.constraints.NotNull;

public record RemapDiscrepancySkuRequest(
    @NotNull(message = "targetSkuId is required")
    Long targetSkuId
) {
}
