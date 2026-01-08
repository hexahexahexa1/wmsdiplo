package com.wmsdipl.contracts.dto;

public record PutawayRuleDto(
    Long id,
    Integer priority,
    String name,
    String strategyType,
    Long zoneId,
    String skuCategory,
    String velocityClass,
    String params,
    Boolean active
) {
}
