package com.wmsdipl.contracts.dto;

import java.math.BigDecimal;

public record SkuUnitConfigDto(
    Long id,
    Long skuId,
    String unitCode,
    BigDecimal factorToBase,
    BigDecimal unitsPerPallet,
    Boolean isBase,
    Boolean active
) {
}
