package com.wmsdipl.contracts.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to create or update a SKU.
 */
public record CreateSkuRequest(
    @NotBlank(message = "SKU code is required")
    @Size(max = 64, message = "Code must not exceed 64 characters")
    String code,
    
    @NotBlank(message = "SKU name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    String name,
    
    @NotBlank(message = "Unit of measure is required")
    @Size(max = 32, message = "UOM must not exceed 32 characters")
    String uom
) {}
