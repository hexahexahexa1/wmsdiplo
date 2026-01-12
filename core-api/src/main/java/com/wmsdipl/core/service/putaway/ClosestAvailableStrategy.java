package com.wmsdipl.core.service.putaway;

import com.wmsdipl.core.domain.Location;
import com.wmsdipl.core.domain.LocationStatus;
import com.wmsdipl.core.domain.Pallet;
import com.wmsdipl.core.domain.Zone;
import com.wmsdipl.core.repository.LocationRepository;
import com.wmsdipl.core.repository.PalletRepository;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
public class ClosestAvailableStrategy implements PutawayStrategy {

    private final LocationRepository locationRepository;
    private final PalletRepository palletRepository;

    public ClosestAvailableStrategy(LocationRepository locationRepository, PalletRepository palletRepository) {
        this.locationRepository = locationRepository;
        this.palletRepository = palletRepository;
    }

    @Override
    public Optional<Location> findLocation(Pallet pallet, PutawayContext context) {
        List<Zone> zones = context.getPreferredZone() != null
            ? List.of(context.getPreferredZone())
            : Collections.emptyList();
        
        // Get target location type from context (STORAGE, CROSS_DOCK, or DAMAGED)
        var locationType = context.getTargetLocationType();
        
        // If no zones specified, find any available location of target type
        if (zones.isEmpty()) {
            return findFirstFit(
                locationRepository.findByLocationTypeAndStatusAndActiveTrue(locationType, LocationStatus.AVAILABLE)
            );
        }
        
        // Try to find location in preferred zone with target type
        for (Zone zone : zones) {
            Optional<Location> fit = findFirstFit(
                locationRepository.findByZoneAndLocationTypeAndStatusAndActiveTrue(zone, locationType, LocationStatus.AVAILABLE)
            );
            if (fit.isPresent()) {
                return fit;
            }
        }
        
        return Optional.empty();
    }

    private Optional<Location> findFirstFit(List<Location> candidates) {
        return candidates.stream()
            .filter(loc -> loc.getMaxPallets() == null || palletRepository.countByLocation(loc) < loc.getMaxPallets())
            .findFirst();
    }

    @Override
    public String getStrategyType() {
        return "CLOSEST";
    }
}
