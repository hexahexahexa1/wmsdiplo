package com.wmsdipl.core.service.putaway;

import com.wmsdipl.core.domain.Location;
import com.wmsdipl.core.domain.Pallet;
import com.wmsdipl.core.domain.PutawayRule;
import com.wmsdipl.core.service.PutawayRuleService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service responsible for selecting the optimal storage location for a pallet
 * based on putaway rules and strategies.
 */
@Service
public class LocationSelectionService {

    private final PutawayRuleService ruleService;
    private final StrategyRegistry strategyRegistry;

    public LocationSelectionService(PutawayRuleService ruleService, StrategyRegistry strategyRegistry) {
        this.ruleService = ruleService;
        this.strategyRegistry = strategyRegistry;
    }

    /**
     * Determines the best location for a pallet based on active putaway rules.
     * 
     * @param pallet the pallet to place
     * @param context the putaway context with SKU and zone information
     * @return Optional containing the selected location, or empty if no suitable location found
     */
    public Optional<Location> determineLocation(Pallet pallet, PutawayContext context) {
        List<PutawayRule> rules = ruleService.getActiveRules();

        for (PutawayRule rule : rules) {
            if (!matchesRule(rule, pallet, context)) {
                continue;
            }

            Optional<PutawayStrategy> strategyOpt = strategyRegistry.get(rule.getStrategyType());
            if (strategyOpt.isEmpty()) {
                continue;
            }

            Optional<Location> candidate = strategyOpt.get().findLocation(pallet, context);
            if (candidate.isPresent()) {
                return candidate;
            }
        }

        return Optional.empty();
    }

    /**
     * Checks if a pallet matches the criteria defined in a putaway rule.
     */
    private boolean matchesRule(PutawayRule rule, Pallet pallet, PutawayContext context) {
        if (!Boolean.TRUE.equals(rule.getActive())) {
            return false;
        }

        // Zone matching
        if (rule.getZone() != null) {
            if (context.getPreferredZone() == null || 
                !rule.getZone().getId().equals(context.getPreferredZone().getId())) {
                return false;
            }
        }

        // Velocity class matching
        if (rule.getVelocityClass() != null && context.getVelocityClass() != null) {
            if (!rule.getVelocityClass().equalsIgnoreCase(context.getVelocityClass())) {
                return false;
            }
        }

        // SKU category matching
        if (rule.getSkuCategory() != null && context.getSkuCategory() != null) {
            if (!rule.getSkuCategory().equalsIgnoreCase(context.getSkuCategory())) {
                return false;
            }
        }

        return true;
    }
}
