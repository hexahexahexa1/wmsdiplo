package com.wmsdipl.core.repository;

import com.wmsdipl.core.domain.PutawayRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PutawayRuleRepository extends JpaRepository<PutawayRule, Long> {
    List<PutawayRule> findByActiveTrueOrderByPriorityAsc();
}
