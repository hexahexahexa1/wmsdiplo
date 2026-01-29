package com.wmsdipl.core.service.putaway;

import com.wmsdipl.core.domain.Location;
import com.wmsdipl.core.domain.LocationStatus;
import com.wmsdipl.core.domain.Pallet;
import com.wmsdipl.core.domain.TaskStatus;
import com.wmsdipl.core.repository.LocationRepository;
import com.wmsdipl.core.repository.PalletRepository;
import com.wmsdipl.core.repository.TaskRepository;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Optional;
import java.util.Set;

@Component
public class ConsolidationStrategy implements PutawayStrategy {

    private final LocationRepository locationRepository;
    private final PalletRepository palletRepository;
    private final TaskRepository taskRepository;
    private final ClosestAvailableStrategy fallback;

    private static final Set<TaskStatus> PENDING_STATUSES = Set.of(TaskStatus.NEW, TaskStatus.ASSIGNED, TaskStatus.IN_PROGRESS);

    public ConsolidationStrategy(LocationRepository locationRepository,
                                 PalletRepository palletRepository,
                                 TaskRepository taskRepository,
                                 ClosestAvailableStrategy fallback) {
        this.locationRepository = locationRepository;
        this.palletRepository = palletRepository;
        this.taskRepository = taskRepository;
        this.fallback = fallback;
    }

    @Override
    public Optional<Location> findLocation(Pallet pallet, PutawayContext context) {
        if (pallet.getSkuId() != null) {
            var locationType = context.getTargetLocationType();
            
            // Try locations that already hold the same SKU, match the target type, and are not blocked.
            Optional<Location> match = locationRepository.findAll().stream()
                .filter(loc -> loc.getLocationType() == locationType) // Filter by target location type
                .filter(loc -> loc.getStatus() == LocationStatus.AVAILABLE || loc.getStatus() == LocationStatus.OCCUPIED)
                .filter(loc -> palletRepository.findByLocation(loc).stream()
                    .anyMatch(p -> pallet.getSkuId().equals(p.getSkuId())))
                .sorted(Comparator.comparing(Location::getId))
                .filter(loc -> {
                    if (loc.getMaxPallets() == null) return true;
                    long currentPallets = palletRepository.countByLocation(loc);
                    long pendingArrivals = taskRepository.countByTargetLocationIdAndStatusIn(loc.getId(), PENDING_STATUSES);
                    return (currentPallets + pendingArrivals) < loc.getMaxPallets();
                })
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
