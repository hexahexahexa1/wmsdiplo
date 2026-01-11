package com.wmsdipl.contracts.dto;

import java.math.BigDecimal;

/**
 * Stock summary DTO.
 * Represents aggregated inventory information grouped by SKU.
 */
public record StockSummaryDto(
    Long skuId,
    String skuCode,
    String skuName,
    String uom,
    
    // Aggregated quantities
    BigDecimal totalQuantity,
    Integer palletCount,
    
    // Location count (how many different locations have this SKU)
    Integer locationCount
) {
}
