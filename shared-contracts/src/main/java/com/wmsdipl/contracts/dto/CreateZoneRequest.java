package com.wmsdipl.contracts.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Запрос на создание новой складской зоны.
 */
public record CreateZoneRequest(
    @NotBlank(message = "Zone code is required")
    String code,
    
    @NotBlank(message = "Zone name is required")
    String name,
    
    @Positive(message = "Priority rank must be positive")
    Integer priorityRank,
    
    String description
) {
}
