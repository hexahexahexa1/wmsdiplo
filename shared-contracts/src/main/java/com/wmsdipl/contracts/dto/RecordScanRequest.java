package com.wmsdipl.contracts.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record RecordScanRequest(
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
    
    // NEW FIELDS for receiving improvements
    Boolean damageFlag,          // Flag for damaged goods
    String damageType,            // PHYSICAL_DAMAGE, WATER_DAMAGE, EXPIRED, TEMPERATURE_ABUSE, CONTAMINATION, OTHER
    String damageDescription,     // Free text description of damage
    String lotNumber,             // Lot/batch number
    LocalDate expiryDate          // Expiry date
) {
}
