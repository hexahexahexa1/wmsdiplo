package com.wmsdipl.desktop.model;

import java.math.BigDecimal;

public record SkuUnitConfig(
    Long id,
    Long skuId,
    String unitCode,
    BigDecimal factorToBase,
    BigDecimal unitsPerPallet,
    Boolean isBase,
    Boolean active
) {
}
