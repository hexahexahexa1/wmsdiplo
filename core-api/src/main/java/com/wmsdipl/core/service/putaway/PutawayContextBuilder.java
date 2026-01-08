package com.wmsdipl.core.service.putaway;

import com.wmsdipl.core.domain.Pallet;
import com.wmsdipl.core.domain.SkuStorageConfig;
import com.wmsdipl.core.repository.SkuStorageConfigRepository;
import org.springframework.stereotype.Component;

/**
 * Builder for creating PutawayContext from pallet data.
 * Encapsulates the logic for retrieving SKU storage configuration.
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
     * 
     * @param pallet the pallet to build context for
     * @return PutawayContext with SKU configuration and zone preferences
     */
    public PutawayContext buildContext(Pallet pallet) {
        SkuStorageConfig config = null;
        
        if (pallet.getSkuId() != null) {
            config = skuStorageConfigRepository.findBySkuId(pallet.getSkuId()).orElse(null);
        }

        return new PutawayContext(
                pallet.getReceipt(),
                config != null ? config.getPreferredZone() : null,
                config != null ? config.getVelocityClass() : null,
                null, // SKU category - currently not used
                pallet.getLocation()
        );
    }
}
