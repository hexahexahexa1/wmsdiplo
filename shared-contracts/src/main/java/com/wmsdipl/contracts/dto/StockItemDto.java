package com.wmsdipl.contracts.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Stock inventory item DTO.
 * Represents a single pallet in the warehouse with all relevant information
 * for inventory views and reports.
 */
public record StockItemDto(
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
