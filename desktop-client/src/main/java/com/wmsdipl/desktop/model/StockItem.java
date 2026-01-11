package com.wmsdipl.desktop.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Stock inventory item for desktop client.
 * Matches StockItemDto from shared-contracts.
 */
public record StockItem(
    Long palletId,
    String palletCode,
    String palletStatus,
    
    // SKU information
    Long skuId,
    String skuCode,
    String skuName,
    
    // Quantity and UOM
    BigDecimal quantity,
    String uom,
    
    // Location information (nullable for unplaced pallets)
    Long locationId,
    String locationCode,
    
    // Receipt information
    Long receiptId,
    String receiptDocNumber,
    LocalDateTime receiptDate,
    
    // Product tracking
    String lotNumber,
    LocalDate expiryDate,
    
    // Timestamps
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
