package com.wmsdipl.core.service;

import com.wmsdipl.core.domain.Location;
import com.wmsdipl.core.domain.LocationStatus;
import com.wmsdipl.core.domain.Pallet;
import com.wmsdipl.core.domain.PalletStatus;
import com.wmsdipl.core.domain.Receipt;
import com.wmsdipl.core.domain.Task;
import com.wmsdipl.core.domain.TaskStatus;
import com.wmsdipl.core.domain.TaskType;
import com.wmsdipl.core.repository.LocationRepository;
import com.wmsdipl.core.repository.PalletRepository;
import com.wmsdipl.core.repository.ReceiptRepository;
import com.wmsdipl.core.repository.TaskRepository;
import com.wmsdipl.core.service.putaway.LocationSelectionService;
import com.wmsdipl.core.service.putaway.PutawayContext;
import com.wmsdipl.core.service.putaway.PutawayContextBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing putaway operations - determining optimal storage locations
 * for pallets and moving pallets to those locations.
 * 
 * Refactored to use LocationSelectionService and PutawayContextBuilder
 * to reduce dependencies from 7 to 4.
 */
@Service
public class PutawayService {

    private final PalletRepository palletRepository;
    private final LocationRepository locationRepository;
    private final TaskRepository taskRepository;
    private final ReceiptRepository receiptRepository;
    private final LocationSelectionService locationSelectionService;
    private final PutawayContextBuilder contextBuilder;

    public PutawayService(
            PalletRepository palletRepository,
            LocationRepository locationRepository,
            TaskRepository taskRepository,
            ReceiptRepository receiptRepository,
            LocationSelectionService locationSelectionService,
            PutawayContextBuilder contextBuilder
    ) {
        this.palletRepository = palletRepository;
        this.locationRepository = locationRepository;
        this.taskRepository = taskRepository;
        this.receiptRepository = receiptRepository;
        this.locationSelectionService = locationSelectionService;
        this.contextBuilder = contextBuilder;
    }

    /**
     * Determines the optimal storage location for a pallet based on putaway rules.
     * 
     * @param palletId the pallet ID
     * @return Optional containing the selected location, or empty if no suitable location found
     * @throws IllegalArgumentException if pallet not found
     * @throws IllegalStateException if pallet is not eligible for putaway
     */
    @Transactional(readOnly = true)
    public Optional<Location> determineLocation(Long palletId) {
        Pallet pallet = palletRepository.findById(palletId)
                .orElseThrow(() -> new IllegalArgumentException("Pallet not found: " + palletId));
        
        if (pallet.getStatus() != PalletStatus.RECEIVED && pallet.getStatus() != PalletStatus.IN_TRANSIT) {
            throw new IllegalStateException("Pallet is not eligible for putaway: " + pallet.getStatus());
        }

        PutawayContext context = contextBuilder.buildContext(pallet);
        return locationSelectionService.determineLocation(pallet, context);
    }

    /**
     * Moves a pallet to the specified location and updates pallet status to PLACED.
     * 
     * @param palletId the pallet ID
     * @param locationId the target location ID
     * @return the updated pallet
     * @throws IllegalArgumentException if pallet or location not found
     * @throws IllegalStateException if location is not available
     */
    @Transactional
    public Pallet moveToLocation(Long palletId, Long locationId) {
        Pallet pallet = palletRepository.findById(palletId)
                .orElseThrow(() -> new IllegalArgumentException("Pallet not found: " + palletId));
        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new IllegalArgumentException("Location not found: " + locationId));

        if (location.getStatus() != LocationStatus.AVAILABLE) {
            throw new IllegalStateException("Location is not available for putaway");
        }

        pallet.setLocation(location);
        pallet.setStatus(PalletStatus.PLACED);
        return palletRepository.save(pallet);
    }

    /**
     * Generates placement tasks for all pallets that need placement.
     * Processes pallets in RECEIVED, DAMAGED, and QUARANTINE statuses.
     * For each pallet, determines the target location and creates a task.
     * Updates pallet status to IN_TRANSIT.
     * 
     * @param receiptId the receipt ID
     * @return list of created placement tasks
     * @throws IllegalArgumentException if receipt not found
     */
    @Transactional
    public List<Task> generatePlacementTasks(Long receiptId) {
        Receipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new IllegalArgumentException("Receipt not found: " + receiptId));

        // Get all pallets that need placement
        List<Pallet> receivedPallets = palletRepository.findByReceiptAndStatus(receipt, PalletStatus.RECEIVED);
        List<Pallet> damagedPallets = palletRepository.findByReceiptAndStatus(receipt, PalletStatus.DAMAGED);
        List<Pallet> quarantinePallets = palletRepository.findByReceiptAndStatus(receipt, PalletStatus.QUARANTINE);
        
        List<Pallet> allPallets = new ArrayList<>();
        allPallets.addAll(receivedPallets);
        allPallets.addAll(damagedPallets);
        allPallets.addAll(quarantinePallets);
        
        List<Task> tasks = new ArrayList<>();

        for (Pallet pallet : allPallets) {
            Optional<Location> targetOpt = determineLocationForPallet(pallet);
            if (targetOpt.isEmpty()) {
                continue; // No suitable location found, skip this pallet
            }
            Location target = targetOpt.get();

            Task task = new Task();
            task.setReceipt(receipt);
            task.setTaskType(TaskType.PLACEMENT);
            task.setStatus(TaskStatus.NEW);
            task.setPalletId(pallet.getId());
            task.setSourceLocationId(pallet.getLocation() != null ? pallet.getLocation().getId() : null);
            task.setTargetLocationId(target.getId());
            task.setQtyAssigned(pallet.getQuantity());
            tasks.add(taskRepository.save(task));

            pallet.setStatus(PalletStatus.IN_TRANSIT);
            palletRepository.save(pallet);
        }

        return tasks;
    }

    /**
     * Internal method to determine location for a pallet during task generation.
     * Uses LocationSelectionService to find the optimal location.
     */
    private Optional<Location> determineLocationForPallet(Pallet pallet) {
        PutawayContext context = contextBuilder.buildContext(pallet);
        return locationSelectionService.determineLocation(pallet, context);
    }
}
