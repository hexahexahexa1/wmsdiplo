package com.wmsdipl.desktop.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Desktop client model for Scan.
 * Represents a single barcode scan event during receiving workflow.
 * Terminal uses Integer qty for simplicity.
 * 
 * Enhanced with damage tracking and lot/expiry fields.
 */
public record Scan(
    Long id,
    String palletCode,
    String sscc,
    String barcode,
    Integer qty,
    String deviceId,
    Boolean discrepancy,
    LocalDateTime scannedAt,
    // Damage tracking fields
    Boolean damageFlag,
    String damageType,
    String damageDescription,
    // Lot tracking fields
    String lotNumber,
    LocalDate expiryDate
) {
}
