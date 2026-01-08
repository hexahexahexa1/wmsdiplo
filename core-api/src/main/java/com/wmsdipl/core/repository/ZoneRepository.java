package com.wmsdipl.core.repository;

import com.wmsdipl.core.domain.Zone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ZoneRepository extends JpaRepository<Zone, Long> {
    Optional<Zone> findByCode(String code);
    boolean existsByCode(String code);
}
