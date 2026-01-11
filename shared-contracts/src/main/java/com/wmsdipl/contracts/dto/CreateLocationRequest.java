package com.wmsdipl.contracts.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Запрос на создание новой складской ячейки.
 */
public record CreateLocationRequest(
    @NotNull(message = "Zone ID is required")
    Long zoneId,
    
    @NotBlank(message = "Location code is required")
    String code,
    
    String aisle,
    String bay,
    String level,
    
    BigDecimal xCoord,
    BigDecimal yCoord,
    BigDecimal zCoord,
    
    @Positive(message = "Max weight must be positive")
    BigDecimal maxWeightKg,
    
    @Positive(message = "Max height must be positive")
    BigDecimal maxHeightCm,
    
    @Positive(message = "Max width must be positive")
    BigDecimal maxWidthCm,
    
    @Positive(message = "Max depth must be positive")
    BigDecimal maxDepthCm,
    
    @Positive(message = "Max pallets must be positive")
    Integer maxPallets,
    
    String locationType
) {
}
