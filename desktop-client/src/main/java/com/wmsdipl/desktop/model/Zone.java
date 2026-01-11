package com.wmsdipl.desktop.model;

public record Zone(
    Long id, 
    String code, 
    String name,
    Integer priorityRank,
    String description,
    Boolean active
) {
}
