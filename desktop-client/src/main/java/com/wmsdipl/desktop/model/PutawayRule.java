package com.wmsdipl.desktop.model;

public record PutawayRule(Long id,
                          Integer priority,
                          String name,
                          String strategyType,
                          Long zoneId,
                          String skuCategory,
                          String velocityClass,
                          Boolean active) {
}
