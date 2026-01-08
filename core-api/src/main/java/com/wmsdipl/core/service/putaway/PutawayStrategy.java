package com.wmsdipl.core.service.putaway;

import com.wmsdipl.core.domain.Location;
import com.wmsdipl.core.domain.Pallet;

import java.util.Optional;

public interface PutawayStrategy {
    Optional<Location> findLocation(Pallet pallet, PutawayContext context);
    String getStrategyType();
}
