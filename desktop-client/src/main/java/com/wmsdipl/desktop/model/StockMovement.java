package com.wmsdipl.desktop.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Stock movement history item for desktop client.
 * Matches StockMovementDto from shared-contracts.
 */
public record StockMovement(
    Long id,
    Long palletId,
    String palletCode,
    String movementType,
    
    // Location transition
    Long fromLocationId,
    String fromLocationCode,
    Long toLocationId,
    String toLocationCode,
    
    // Quantity involved in movement
    BigDecimal quantity,
    
    // Related task (nullable for manual movements)
    Long taskId,
    
    // Movement metadata
    String movedBy,
    LocalDateTime movedAt
) {
}
