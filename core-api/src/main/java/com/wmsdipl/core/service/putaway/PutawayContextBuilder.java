package com.wmsdipl.core.service.putaway;

import com.wmsdipl.core.domain.LocationType;
import com.wmsdipl.core.domain.Pallet;
import com.wmsdipl.core.domain.PalletStatus;
import com.wmsdipl.core.domain.SkuStorageConfig;
import com.wmsdipl.core.repository.SkuStorageConfigRepository;
import org.springframework.stereotype.Component;

/**
 * Builder for creating PutawayContext from pallet data.
 * Encapsulates the logic for retrieving SKU storage configuration
 * and determining target location type based on pallet status and receipt flags.
 */
@Component
public class PutawayContextBuilder {

    private final SkuStorageConfigRepository skuStorageConfigRepository;

    public PutawayContextBuilder(SkuStorageConfigRepository skuStorageConfigRepository) {
        this.skuStorageConfigRepository = skuStorageConfigRepository;
    }

    /**
     * Builds a PutawayContext from pallet information.
     * Retrieves SKU storage configuration if SKU ID is present.
     * Determines target location type based on:
     * - Pallet status (DAMAGED → DAMAGED location)
     * - Receipt cross-dock flag (crossDock=true → CROSS_DOCK location)
     * - Default: STORAGE location
     * 
     * @param pallet the pallet to build context for
     * @return PutawayContext with SKU configuration, zone preferences, and target location type
     */
    public PutawayContext buildContext(Pallet pallet) {
        SkuStorageConfig config = null;
        
        if (pallet.getSkuId() != null) {
            config = skuStorageConfigRepository.findBySkuId(pallet.getSkuId()).orElse(null);
        }

        LocationType targetLocationType = determineTargetLocationType(pallet);

        return new PutawayContext(
                pallet.getReceipt(),
                config != null ? config.getPreferredZone() : null,
                config != null ? config.getVelocityClass() : null,
                null, // SKU category - currently not used
                pallet.getLocation(),
                targetLocationType
        );
    }

    /**
     * Determines the target location type based on pallet and receipt characteristics.
     * Priority:
     * 1. DAMAGED if pallet status is DAMAGED
     * 2. CROSS_DOCK if receipt has crossDock flag set
     * 3. STORAGE as default
     */
    private LocationType determineTargetLocationType(Pallet pallet) {
        // Priority 1: Damaged pallets go to DAMAGED locations
        if (pallet.getStatus() == PalletStatus.DAMAGED) {
            return LocationType.DAMAGED;
        }

        // Priority 2: Cross-dock receipts go to CROSS_DOCK locations
        if (pallet.getReceipt() != null && Boolean.TRUE.equals(pallet.getReceipt().getCrossDock())) {
            return LocationType.CROSS_DOCK;
        }

        // Default: Regular storage
        return LocationType.STORAGE;
    }
}
