package com.wmsdipl.contracts.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Stock movement history DTO.
 * Represents a single movement event in a pallet's lifecycle.
 */
public record StockMovementDto(
    Long id,
    Long palletId,
    String palletCode,
    String movementType,
    
    // Location transition
    Long fromLocationId,
    String fromLocationCode,
    Long toLocationId,
    String toLocationCode,
    
    // Quantity involved in movement (nullable - null means full pallet)
    BigDecimal quantity,
    
    // Related task (nullable for manual movements)
    Long taskId,
    
    // Movement metadata
    String movedBy,
    LocalDateTime movedAt
) {
}
