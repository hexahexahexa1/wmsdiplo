package com.wmsdipl.contracts.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for Scan entity.
 * Represents a single barcode scan event during receiving/placement operations.
 */
public record ScanDto(
    Long id,
    Long taskId,
    String palletCode,
    String sscc,
    String barcode,
    BigDecimal qty,
    String deviceId,
    Boolean discrepancy,
    LocalDateTime scannedAt
) {
}
