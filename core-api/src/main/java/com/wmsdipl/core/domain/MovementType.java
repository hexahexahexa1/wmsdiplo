package com.wmsdipl.core.domain;

/**
 * Enum representing types of pallet movements in the warehouse.
 * Used to track pallet lifecycle events for stock inventory history.
 */
public enum MovementType {
    /**
     * Pallet received from supplier (initial receiving).
     */
    RECEIVE,
    
    /**
     * Pallet placed into storage location (after receiving).
     */
    PLACE,
    
    /**
     * Pallet moved between locations (relocation/transfer).
     */
    MOVE,
    
    /**
     * Pallet picked for outbound order (full or partial).
     */
    PICK,
    
    /**
     * Manual inventory adjustment (damage, correction, etc.).
     */
    ADJUST
}
