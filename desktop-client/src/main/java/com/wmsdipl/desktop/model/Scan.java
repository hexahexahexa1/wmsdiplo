package com.wmsdipl.desktop.model;

import java.time.LocalDateTime;

/**
 * Desktop client model for Scan.
 * Represents a single barcode scan event during receiving workflow.
 * Terminal uses Integer qty for simplicity.
 */
public record Scan(
    Long id,
    String palletCode,
    String sscc,
    String barcode,
    Integer qty,
    String deviceId,
    Boolean discrepancy,
    LocalDateTime scannedAt
) {
}
