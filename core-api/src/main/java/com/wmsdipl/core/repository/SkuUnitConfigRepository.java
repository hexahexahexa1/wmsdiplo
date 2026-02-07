package com.wmsdipl.core.repository;

import com.wmsdipl.core.domain.SkuUnitConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SkuUnitConfigRepository extends JpaRepository<SkuUnitConfig, Long> {
    List<SkuUnitConfig> findBySkuIdOrderByIsBaseDescUnitCodeAsc(Long skuId);
    Optional<SkuUnitConfig> findBySkuIdAndUnitCodeIgnoreCase(Long skuId, String unitCode);
    Optional<SkuUnitConfig> findBySkuIdAndIsBaseTrue(Long skuId);
    List<SkuUnitConfig> findBySkuIdInAndIsBaseTrueAndActiveTrue(Collection<Long> skuIds);
}
