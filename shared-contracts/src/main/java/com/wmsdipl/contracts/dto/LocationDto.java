package com.wmsdipl.contracts.dto;

import java.math.BigDecimal;

/**
 * DTO для отображения информации о складской ячейке.
 */
public record LocationDto(
    Long id,
    Long zoneId,
    String zoneCode,
    String code,
    String aisle,
    String bay,
    String level,
    BigDecimal xCoord,
    BigDecimal yCoord,
    BigDecimal zCoord,
    BigDecimal maxWeightKg,
    BigDecimal maxHeightCm,
    BigDecimal maxWidthCm,
    BigDecimal maxDepthCm,
    Integer maxPallets,
    String locationType,
    String status,
    Boolean active
) {
}
