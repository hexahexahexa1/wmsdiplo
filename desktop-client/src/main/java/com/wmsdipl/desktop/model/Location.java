package com.wmsdipl.desktop.model;

public record Location(
    Long id, 
    Long zoneId,
    String zoneCode,
    String code, 
    String locationType,
    String status, 
    Integer maxPallets, 
    String aisle,
    String bay,
    String level,
    Boolean active
) {
}
