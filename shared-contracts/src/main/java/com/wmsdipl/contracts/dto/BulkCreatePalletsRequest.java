package com.wmsdipl.contracts.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request to bulk create pallets with sequential numbering.
 * Example: startNumber=100, count=50 creates PLT-100 through PLT-149
 */
public record BulkCreatePalletsRequest(
    @NotNull(message = "Start number is required")
    @Min(value = 1, message = "Start number must be at least 1")
    Integer startNumber,
    
    @NotNull(message = "Count is required")
    @Min(value = 1, message = "Count must be at least 1")
    Integer count
) {
}
