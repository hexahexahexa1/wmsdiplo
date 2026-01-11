package com.wmsdipl.core.service;

import com.wmsdipl.core.domain.Location;
import com.wmsdipl.core.domain.Pallet;
import com.wmsdipl.core.domain.PalletMovement;
import com.wmsdipl.core.domain.PalletStatus;
import com.wmsdipl.core.domain.Sku;
import com.wmsdipl.core.repository.LocationRepository;
import com.wmsdipl.core.repository.PalletMovementRepository;
import com.wmsdipl.core.repository.PalletRepository;
import com.wmsdipl.core.repository.SkuRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for stock inventory queries and reporting.
 * Provides current and historical views of warehouse inventory.
 */
@Service
@Transactional(readOnly = true)
public class StockService {

    private final PalletRepository palletRepository;
    private final PalletMovementRepository palletMovementRepository;
    private final SkuRepository skuRepository;
    private final LocationRepository locationRepository;

    public StockService(
            PalletRepository palletRepository,
            PalletMovementRepository palletMovementRepository,
            SkuRepository skuRepository,
            LocationRepository locationRepository) {
        this.palletRepository = palletRepository;
        this.palletMovementRepository = palletMovementRepository;
        this.skuRepository = skuRepository;
        this.locationRepository = locationRepository;
    }

    /**
     * Get paginated stock inventory with optional filters.
     * Loads SKU entities in batch for efficient mapping.
     * 
     * @param skuCode SKU code filter (optional)
     * @param locationCode Location code filter (optional)
     * @param palletBarcode Pallet barcode filter (optional)
     * @param receiptId Receipt ID filter (optional)
     * @param status Pallet status filter (optional)
     * @param asOfDate Point-in-time date (optional, defaults to current time)
     * @param pageable Pagination parameters
     * @return Page of pallets matching criteria with SKU map
     */
    public StockResult getStock(
            String skuCode,
            String locationCode,
            String palletBarcode,
            Long receiptId,
            String status,
            LocalDateTime asOfDate,
            Pageable pageable) {

        Specification<Pallet> spec = buildStockSpecification(
                skuCode, locationCode, palletBarcode, receiptId, status);

        Page<Pallet> pallets = palletRepository.findAll(spec, pageable);

        // If querying historical data, adjust locations based on movements
        if (asOfDate != null) {
            pallets.forEach(pallet -> adjustPalletLocationToDate(pallet, asOfDate));
        }

        // Load SKUs in batch
        Map<Long, Sku> skuMap = loadSkusForPallets(pallets.getContent());

        return new StockResult(pallets, skuMap);
    }

    /**
     * Get single pallet by ID with its SKU.
     * 
     * @param palletId Pallet ID
     * @return Pallet with SKU map
     * @throws ResponseStatusException if pallet not found
     */
    public StockResult getPalletById(Long palletId) {
        Pallet pallet = palletRepository.findById(palletId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Pallet not found: " + palletId));
        
        Map<Long, Sku> skuMap = loadSkusForPallets(List.of(pallet));
        
        return new StockResult(pallet, skuMap);
    }

    /**
     * Get movement history for a specific pallet.
     * 
     * @param palletId Pallet ID
     * @return List of movements ordered by date descending
     * @throws ResponseStatusException if pallet not found
     */
    public List<PalletMovement> getPalletHistory(Long palletId) {
        Pallet pallet = palletRepository.findById(palletId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Pallet not found: " + palletId));
        return palletMovementRepository.findByPalletOrderByMovedAtDesc(pallet);
    }

    /**
     * Get pallets in a specific location.
     * 
     * @param locationId Location ID
     * @param pageable Pagination parameters
     * @return Pallets with SKU map
     * @throws ResponseStatusException if location not found
     */
    public StockResult getPalletsByLocation(Long locationId, Pageable pageable) {
        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Location not found: " + locationId));

        Specification<Pallet> spec = (root, query, cb) -> 
            cb.and(
                cb.equal(root.get("location"), location),
                cb.greaterThan(root.get("quantity"), BigDecimal.ZERO)
            );

        Page<Pallet> pallets = palletRepository.findAll(spec, pageable);
        Map<Long, Sku> skuMap = loadSkusForPallets(pallets.getContent());
        
        return new StockResult(pallets, skuMap);
    }

    /**
     * Load SKU entities for a list of pallets in a single batch query.
     * 
     * @param pallets List of pallets
     * @return Map of skuId -> Sku entity
     */
    private Map<Long, Sku> loadSkusForPallets(List<Pallet> pallets) {
        Set<Long> skuIds = pallets.stream()
                .map(Pallet::getSkuId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        
        if (skuIds.isEmpty()) {
            return Map.of();
        }
        
        List<Sku> skus = skuRepository.findAllById(skuIds);
        return skus.stream()
                .collect(Collectors.toMap(Sku::getId, sku -> sku));
    }

    /**
     * Build JPA Specification for stock filtering.
     */
    private Specification<Pallet> buildStockSpecification(
            String skuCode,
            String locationCode,
            String palletBarcode,
            Long receiptId,
            String status) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Hide empty pallets from stock view
            predicates.add(cb.greaterThan(root.get("quantity"), BigDecimal.ZERO));

            // SKU filter - need to join to sku table
            if (skuCode != null && !skuCode.isBlank()) {
                // Since Pallet only has skuId, we need to query SKU first
                skuRepository.findByCode(skuCode).ifPresent(sku -> 
                    predicates.add(cb.equal(root.get("skuId"), sku.getId()))
                );
            }

            // Location filter
            if (locationCode != null && !locationCode.isBlank()) {
                predicates.add(cb.equal(root.get("location").get("code"), locationCode));
            }

            // Pallet barcode filter
            if (palletBarcode != null && !palletBarcode.isBlank()) {
                predicates.add(cb.like(
                    cb.lower(root.get("code")), 
                    "%" + palletBarcode.toLowerCase() + "%"));
            }

            // Receipt filter
            if (receiptId != null) {
                predicates.add(cb.equal(root.get("receipt").get("id"), receiptId));
            }

            // Status filter
            if (status != null && !status.isBlank()) {
                try {
                    PalletStatus palletStatus = PalletStatus.valueOf(status);
                    predicates.add(cb.equal(root.get("status"), palletStatus));
                } catch (IllegalArgumentException e) {
                    // Invalid status - ignore filter
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Adjust pallet location to reflect historical state at a specific date.
     * Modifies the pallet object in-place (transient change, not persisted).
     */
    private void adjustPalletLocationToDate(Pallet pallet, LocalDateTime asOfDate) {
        palletMovementRepository
                .findTopByPalletAndMovedAtLessThanEqualOrderByMovedAtDesc(pallet, asOfDate)
                .ifPresent(movement -> pallet.setLocation(movement.getToLocation()));
    }

    /**
     * Result wrapper containing pallets and their SKU entities.
     */
    public static class StockResult {
        public final Page<Pallet> pallets;
        public final Pallet singlePallet;
        public final Map<Long, Sku> skuMap;

        public StockResult(Page<Pallet> pallets, Map<Long, Sku> skuMap) {
            this.pallets = pallets;
            this.singlePallet = null;
            this.skuMap = skuMap;
        }

        public StockResult(Pallet pallet, Map<Long, Sku> skuMap) {
            this.pallets = null;
            this.singlePallet = pallet;
            this.skuMap = skuMap;
        }
    }
}
