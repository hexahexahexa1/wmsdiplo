package com.wmsdipl.contracts.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RecordScanRequest(
    @NotBlank(message = "Pallet code is required")
    String palletCode,

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    Integer qty,

    String sscc,
    String barcode,
    String deviceId,
    String comment
) {
}
