package com.wmsdipl.core.service.putaway;

import com.wmsdipl.core.domain.Location;
import com.wmsdipl.core.domain.Pallet;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AbcVelocityStrategy implements PutawayStrategy {

    private final ClosestAvailableStrategy closest;

    public AbcVelocityStrategy(ClosestAvailableStrategy closest) {
        this.closest = closest;
    }

    @Override
    public Optional<Location> findLocation(Pallet pallet, PutawayContext context) {
        // In this simplified version we just delegate to closest within preferred zone.
        return closest.findLocation(pallet, context);
    }

    @Override
    public String getStrategyType() {
        return "ABC";
    }
}
