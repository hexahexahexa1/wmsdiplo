package com.wmsdipl.core.service.putaway;

import com.wmsdipl.core.domain.Location;
import com.wmsdipl.core.domain.LocationStatus;
import com.wmsdipl.core.domain.Pallet;
import com.wmsdipl.core.repository.LocationRepository;
import com.wmsdipl.core.repository.PalletRepository;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Optional;

@Component
public class ConsolidationStrategy implements PutawayStrategy {

    private final LocationRepository locationRepository;
    private final PalletRepository palletRepository;
    private final ClosestAvailableStrategy fallback;

    public ConsolidationStrategy(LocationRepository locationRepository,
                                 PalletRepository palletRepository,
                                 ClosestAvailableStrategy fallback) {
        this.locationRepository = locationRepository;
        this.palletRepository = palletRepository;
        this.fallback = fallback;
    }

    @Override
    public Optional<Location> findLocation(Pallet pallet, PutawayContext context) {
        if (pallet.getSkuId() != null) {
            // Try locations that already hold the same SKU and are not blocked.
            Optional<Location> match = locationRepository.findAll().stream()
                .filter(loc -> loc.getStatus() == LocationStatus.AVAILABLE || loc.getStatus() == LocationStatus.OCCUPIED)
                .filter(loc -> palletRepository.findByLocation(loc).stream()
                    .anyMatch(p -> pallet.getSkuId().equals(p.getSkuId())))
                .sorted(Comparator.comparing(Location::getId))
                .filter(loc -> loc.getMaxPallets() == null || palletRepository.countByLocation(loc) < loc.getMaxPallets())
                .findFirst();
            if (match.isPresent()) {
                return match;
            }
        }
        return fallback.findLocation(pallet, context);
    }

    @Override
    public String getStrategyType() {
        return "CONSOLIDATION";
    }
}
