package com.wmsdipl.core.service;

import com.wmsdipl.core.domain.PutawayRule;
import com.wmsdipl.core.repository.PutawayRuleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PutawayRuleService {

    private final PutawayRuleRepository repository;

    public PutawayRuleService(PutawayRuleRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<PutawayRule> getActiveRules() {
        return repository.findByActiveTrueOrderByPriorityAsc();
    }
}
