package com.wmsdipl.core.repository;

import com.wmsdipl.core.domain.Discrepancy;
import com.wmsdipl.core.domain.Receipt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Collection;

public interface DiscrepancyRepository extends JpaRepository<Discrepancy, Long> {
    List<Discrepancy> findByReceipt(Receipt receipt);
    List<Discrepancy> findByResolvedFalse();
    List<Discrepancy> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);
    long countByResolvedTrueAndCreatedAtBetween(LocalDateTime from, LocalDateTime to);
    long countByTypeInAndCreatedAtBetween(Collection<String> types, LocalDateTime from, LocalDateTime to);
}
