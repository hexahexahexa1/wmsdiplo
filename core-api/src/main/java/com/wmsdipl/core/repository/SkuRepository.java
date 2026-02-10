package com.wmsdipl.core.repository;

import com.wmsdipl.core.domain.Sku;
import com.wmsdipl.core.domain.SkuStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SkuRepository extends JpaRepository<Sku, Long> {
    Optional<Sku> findByCode(String code);
    List<Sku> findByStatus(SkuStatus status);
}
