package com.wmsdipl.core.service.putaway;

import com.wmsdipl.core.domain.Location;
import com.wmsdipl.core.domain.Pallet;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class FifoDirectedStrategy implements PutawayStrategy {

    private final ClosestAvailableStrategy fallback;

    public FifoDirectedStrategy(ClosestAvailableStrategy fallback) {
        this.fallback = fallback;
    }

    @Override
    public Optional<Location> findLocation(Pallet pallet, PutawayContext context) {
        // Simplified: delegate to closest. FIFO sequencing can be refined later using lot/expiry grouping.
        return fallback.findLocation(pallet, context);
    }

    @Override
    public String getStrategyType() {
        return "FIFO";
    }
}
