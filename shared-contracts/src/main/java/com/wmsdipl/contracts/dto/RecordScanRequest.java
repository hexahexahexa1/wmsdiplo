package com.wmsdipl.contracts.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record RecordScanRequest(
    String requestId,

    @NotBlank(message = "Pallet code is required")
    String palletCode,

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    Integer qty,

    String sscc,
    String barcode,
    String locationCode,
    String deviceId,
    String comment,
    
    Boolean damageFlag,
    DamageType damageType,
    String damageDescription,
    String lotNumber,
    LocalDate expiryDate
) {
}
