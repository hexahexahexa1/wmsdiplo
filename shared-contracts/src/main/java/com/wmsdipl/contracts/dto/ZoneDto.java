package com.wmsdipl.contracts.dto;

/**
 * DTO для отображения информации о складской зоне.
 */
public record ZoneDto(
    Long id,
    String code,
    String name,
    Integer priorityRank,
    String description,
    Boolean active
) {
}
