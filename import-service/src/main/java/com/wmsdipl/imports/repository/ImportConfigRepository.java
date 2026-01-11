package com.wmsdipl.imports.repository;

import com.wmsdipl.imports.domain.ImportConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ImportConfigRepository extends JpaRepository<ImportConfig, Long> {
    
    Optional<ImportConfig> findByConfigKey(String configKey);
}
