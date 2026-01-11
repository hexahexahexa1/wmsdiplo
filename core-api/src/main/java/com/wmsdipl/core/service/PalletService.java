package com.wmsdipl.core.service;

import com.wmsdipl.core.domain.Location;
import com.wmsdipl.core.domain.MovementType;
import com.wmsdipl.core.domain.Pallet;
import com.wmsdipl.core.domain.PalletMovement;
import com.wmsdipl.core.domain.PalletStatus;
import com.wmsdipl.core.repository.LocationRepository;
import com.wmsdipl.core.repository.PalletCodePoolRepository;
import com.wmsdipl.core.repository.PalletMovementRepository;
import com.wmsdipl.core.repository.PalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class PalletService {

    private final PalletRepository palletRepository;
    private final PalletMovementRepository movementRepository;
    private final PalletCodePoolRepository codePoolRepository;
    private final LocationRepository locationRepository;
    private final PalletCodeGenerator codeGenerator;

    public PalletService(
            PalletRepository palletRepository,
            PalletMovementRepository movementRepository,
            PalletCodePoolRepository codePoolRepository,
            LocationRepository locationRepository,
            PalletCodeGenerator codeGenerator
    ) {
        this.palletRepository = palletRepository;
        this.movementRepository = movementRepository;
        this.codePoolRepository = codePoolRepository;
        this.locationRepository = locationRepository;
        this.codeGenerator = codeGenerator;
    }

    public List<Pallet> getAll() {
        return palletRepository.findAll();
    }

    public Optional<Pallet> getById(Long id) {
        return palletRepository.findById(id);
    }

    @Transactional
    public Pallet create(Pallet pallet) {
        return palletRepository.save(pallet);
    }

    /**
     * Creates multiple pallets in a single batch operation.
     * More efficient than creating pallets one by one.
     *
     * @param pallets list of pallets to create
     * @return list of created pallets with generated IDs
     */
    @Transactional
    public List<Pallet> createBatch(List<Pallet> pallets) {
        if (pallets == null || pallets.isEmpty()) {
            return List.of();
        }
        return palletRepository.saveAll(pallets);
    }

    @Transactional
    public Pallet move(Long palletId, Long toLocationId, String movementType, String movedBy) {
        Pallet pallet = palletRepository.findById(palletId)
                .orElseThrow(() -> new IllegalArgumentException("Pallet not found: " + palletId));
        Location from = pallet.getLocation();
        Location to = locationRepository.findById(toLocationId)
                .orElseThrow(() -> new IllegalArgumentException("Location not found: " + toLocationId));
        pallet.setLocation(to);
        pallet.setStatus(PalletStatus.PLACED);
        Pallet saved = palletRepository.save(pallet);

        PalletMovement movement = new PalletMovement();
        movement.setPallet(saved);
        movement.setFromLocation(from);
        movement.setToLocation(to);
        // Convert String to MovementType enum
        try {
            movement.setMovementType(MovementType.valueOf(movementType));
        } catch (IllegalArgumentException e) {
            movement.setMovementType(MovementType.MOVE); // Default fallback
        }
        movement.setMovedBy(movedBy);
        movementRepository.save(movement);
        return saved;
    }

    public List<PalletMovement> getMovements(Long palletId) {
        Pallet pallet = palletRepository.findById(palletId)
                .orElseThrow(() -> new IllegalArgumentException("Pallet not found: " + palletId));
        return movementRepository.findByPallet(pallet);
    }

    @Transactional
    public List<String> generateInternal(String prefix, int count, String codeType) {
        List<String> codes = codeGenerator.generateInternalCodes(prefix, count);
        var entities = codes.stream().map(code -> {
            var pool = new com.wmsdipl.core.domain.PalletCodePool();
            pool.setCode(code);
            pool.setCodeType(codeType == null ? "INTERNAL" : codeType);
            return pool;
        }).toList();
        codePoolRepository.saveAll(entities);
        return codes;
    }

    @Transactional
    public List<String> generateSSCC(String companyPrefix, int count) {
        List<String> codes = codeGenerator.generateSSCC(companyPrefix, count);
        var entities = new ArrayList<com.wmsdipl.core.domain.PalletCodePool>();
        for (String code : codes) {
            var pool = new com.wmsdipl.core.domain.PalletCodePool();
            pool.setCode(code);
            pool.setCodeType("SSCC");
            entities.add(pool);
        }
        codePoolRepository.saveAll(entities);
        return codes;
    }
}
