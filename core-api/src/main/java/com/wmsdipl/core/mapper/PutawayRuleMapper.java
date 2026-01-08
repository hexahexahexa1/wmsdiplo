package com.wmsdipl.core.mapper;

import com.wmsdipl.contracts.dto.PutawayRuleDto;
import com.wmsdipl.core.domain.PutawayRule;
import org.springframework.stereotype.Component;

@Component
public class PutawayRuleMapper {

    public PutawayRuleDto toDto(PutawayRule rule) {
        return new PutawayRuleDto(
            rule.getId(),
            rule.getPriority(),
            rule.getName(),
            rule.getStrategyType(),
            rule.getZone() != null ? rule.getZone().getId() : null,
            rule.getSkuCategory(),
            rule.getVelocityClass(),
            rule.getParams(),
            rule.getActive()
        );
    }

    public PutawayRule toEntity(PutawayRuleDto dto) {
        PutawayRule rule = new PutawayRule();
        rule.setPriority(dto.priority());
        rule.setName(dto.name());
        rule.setStrategyType(dto.strategyType());
        rule.setSkuCategory(dto.skuCategory());
        rule.setVelocityClass(dto.velocityClass());
        rule.setParams(dto.params());
        rule.setActive(dto.active());
        return rule;
    }
}
