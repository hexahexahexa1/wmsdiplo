package com.wmsdipl.core.service;

import com.wmsdipl.core.domain.Location;
import com.wmsdipl.core.domain.MovementType;
import com.wmsdipl.core.domain.Pallet;
import com.wmsdipl.core.domain.PalletMovement;
import com.wmsdipl.core.repository.PalletMovementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Service for recording pallet movements.
 * Creates audit trail for stock inventory history.
 */
@Service
@Transactional
public class StockMovementService {

    private final PalletMovementRepository palletMovementRepository;

    public StockMovementService(PalletMovementRepository palletMovementRepository) {
        this.palletMovementRepository = palletMovementRepository;
    }

    /**
     * Record pallet receipt (initial receiving).
     * 
     * @param pallet Pallet being received
     * @param toLocation Initial location (receiving dock/staging area)
     * @param movedBy User who performed the action
     * @param taskId Related task ID (nullable)
     * @return Created movement record
     */
    public PalletMovement recordReceive(
            Pallet pallet,
            Location toLocation,
            String movedBy,
            Long taskId) {
        
        PalletMovement movement = new PalletMovement();
        movement.setPallet(pallet);
        movement.setFromLocation(null); // No previous location
        movement.setToLocation(toLocation);
        movement.setMovementType(MovementType.RECEIVE);
        movement.setQuantity(pallet.getQuantity());
        movement.setMovedBy(movedBy);
        movement.setTaskId(taskId);
        movement.setMovedAt(LocalDateTime.now());
        
        return palletMovementRepository.save(movement);
    }

    /**
     * Record pallet placement into storage location.
     * 
     * @param pallet Pallet being placed
     * @param fromLocation Previous location (receiving area)
     * @param toLocation Target storage location
     * @param movedBy User who performed the action
     * @param taskId Related task ID (nullable)
     * @return Created movement record
     */
    public PalletMovement recordPlacement(
            Pallet pallet,
            Location fromLocation,
            Location toLocation,
            String movedBy,
            Long taskId) {
        
        PalletMovement movement = new PalletMovement();
        movement.setPallet(pallet);
        movement.setFromLocation(fromLocation);
        movement.setToLocation(toLocation);
        movement.setMovementType(MovementType.PLACE);
        movement.setQuantity(pallet.getQuantity());
        movement.setMovedBy(movedBy);
        movement.setTaskId(taskId);
        movement.setMovedAt(LocalDateTime.now());
        
        return palletMovementRepository.save(movement);
    }

    /**
     * Record pallet relocation between storage locations.
     * 
     * @param pallet Pallet being moved
     * @param fromLocation Source location
     * @param toLocation Target location
     * @param movedBy User who performed the action
     * @param taskId Related task ID (nullable)
     * @return Created movement record
     */
    public PalletMovement recordMove(
            Pallet pallet,
            Location fromLocation,
            Location toLocation,
            String movedBy,
            Long taskId) {
        
        PalletMovement movement = new PalletMovement();
        movement.setPallet(pallet);
        movement.setFromLocation(fromLocation);
        movement.setToLocation(toLocation);
        movement.setMovementType(MovementType.MOVE);
        movement.setQuantity(pallet.getQuantity());
        movement.setMovedBy(movedBy);
        movement.setTaskId(taskId);
        movement.setMovedAt(LocalDateTime.now());
        
        return palletMovementRepository.save(movement);
    }

    /**
     * Record pallet pick for outbound order.
     * 
     * @param pallet Pallet being picked
     * @param fromLocation Source location
     * @param quantity Quantity picked (null = full pallet)
     * @param movedBy User who performed the action
     * @param taskId Related task ID (nullable)
     * @return Created movement record
     */
    public PalletMovement recordPick(
            Pallet pallet,
            Location fromLocation,
            BigDecimal quantity,
            String movedBy,
            Long taskId) {
        
        PalletMovement movement = new PalletMovement();
        movement.setPallet(pallet);
        movement.setFromLocation(fromLocation);
        movement.setToLocation(null); // Picked items leave warehouse
        movement.setMovementType(MovementType.PICK);
        movement.setQuantity(quantity != null ? quantity : pallet.getQuantity());
        movement.setMovedBy(movedBy);
        movement.setTaskId(taskId);
        movement.setMovedAt(LocalDateTime.now());
        
        return palletMovementRepository.save(movement);
    }

    /**
     * Record manual inventory adjustment.
     * 
     * @param pallet Pallet being adjusted
     * @param location Current location
     * @param newQuantity New quantity after adjustment
     * @param movedBy User who performed the action
     * @return Created movement record
     */
    public PalletMovement recordAdjustment(
            Pallet pallet,
            Location location,
            BigDecimal newQuantity,
            String movedBy) {
        
        PalletMovement movement = new PalletMovement();
        movement.setPallet(pallet);
        movement.setFromLocation(location);
        movement.setToLocation(location);
        movement.setMovementType(MovementType.ADJUST);
        movement.setQuantity(newQuantity);
        movement.setMovedBy(movedBy);
        movement.setTaskId(null);
        movement.setMovedAt(LocalDateTime.now());
        
        return palletMovementRepository.save(movement);
    }
}
