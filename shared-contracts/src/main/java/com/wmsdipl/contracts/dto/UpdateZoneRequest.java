package com.wmsdipl.contracts.dto;

import jakarta.validation.constraints.Positive;

/**
 * Запрос на обновление складской зоны.
 */
public record UpdateZoneRequest(
    String code,
    String name,
    
    @Positive(message = "Priority rank must be positive")
    Integer priorityRank,
    
    String description,
    Boolean active
) {
}
