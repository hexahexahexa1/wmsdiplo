package com.wmsdipl.contracts.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for Scan entity.
 * Represents a single barcode scan event during receiving/placement operations.
 */
public record ScanDto(
    Long id,
    Long taskId,
    String requestId,
    String palletCode,
    String sscc,
    String barcode,
    BigDecimal qty,
    String deviceId,
    Boolean discrepancy,
    Boolean damageFlag,
    DamageType damageType,
    String damageDescription,
    String lotNumber,
    LocalDate expiryDate,
    LocalDateTime scannedAt,
    Boolean duplicate,
    Boolean idempotentReplay,
    List<String> warnings
) {
}
