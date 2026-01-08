package com.wmsdipl.contracts.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for Pallet entity.
 * Represents a pallet with simplified references (IDs instead of full objects).
 */
public record PalletDto(
    Long id,
    String code,
    String codeType,
    String status,
    Long skuId,
    String uom,
    BigDecimal quantity,
    Long locationId,
    String locationCode,
    Long receiptId,
    Long receiptLineId,
    String lotNumber,
    LocalDate expiryDate,
    BigDecimal weightKg,
    BigDecimal heightCm,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
