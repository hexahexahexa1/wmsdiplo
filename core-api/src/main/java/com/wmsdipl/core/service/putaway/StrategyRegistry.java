package com.wmsdipl.core.service.putaway;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class StrategyRegistry {

    private final Map<String, PutawayStrategy> strategies = new HashMap<>();

    public StrategyRegistry(
            ClosestAvailableStrategy closestAvailableStrategy,
            AbcVelocityStrategy abcVelocityStrategy,
            ConsolidationStrategy consolidationStrategy,
            FifoDirectedStrategy fifoDirectedStrategy
    ) {
        register(closestAvailableStrategy);
        register(abcVelocityStrategy);
        register(consolidationStrategy);
        register(fifoDirectedStrategy);
    }

    public Optional<PutawayStrategy> get(String strategyType) {
        return Optional.ofNullable(strategies.get(strategyType));
    }

    private void register(PutawayStrategy strategy) {
        strategies.put(strategy.getStrategyType(), strategy);
    }
}
