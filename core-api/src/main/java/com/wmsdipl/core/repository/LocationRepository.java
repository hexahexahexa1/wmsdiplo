package com.wmsdipl.core.repository;

import com.wmsdipl.core.domain.Location;
import com.wmsdipl.core.domain.LocationStatus;
import com.wmsdipl.core.domain.LocationType;
import com.wmsdipl.core.domain.Zone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LocationRepository extends JpaRepository<Location, Long> {
    Optional<Location> findByCode(String code);
    boolean existsByCode(String code);
    List<Location> findByZone(Zone zone);
    List<Location> findByZoneAndStatusAndActiveTrue(Zone zone, LocationStatus status);
    List<Location> findByStatusAndActiveTrue(LocationStatus status);
    
    /**
     * Finds first available location with specified type.
     * Used to determine transit location for receiving workflow.
     * 
     * @param locationType type of location (e.g., RECEIVING, STORAGE)
     * @param status location status (typically AVAILABLE)
     * @return first matching location, ordered by ID
     */
    Optional<Location> findFirstByLocationTypeAndStatusAndActiveTrueOrderByIdAsc(
        LocationType locationType, LocationStatus status);
}
