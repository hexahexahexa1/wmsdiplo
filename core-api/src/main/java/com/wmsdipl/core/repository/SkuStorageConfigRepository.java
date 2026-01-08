package com.wmsdipl.core.repository;

import com.wmsdipl.core.domain.SkuStorageConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SkuStorageConfigRepository extends JpaRepository<SkuStorageConfig, Long> {
    Optional<SkuStorageConfig> findBySkuId(Long skuId);
}
