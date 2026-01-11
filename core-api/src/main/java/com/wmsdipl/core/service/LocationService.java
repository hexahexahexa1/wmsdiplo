package com.wmsdipl.core.service;

import com.wmsdipl.core.domain.Location;
import com.wmsdipl.core.domain.LocationStatus;
import com.wmsdipl.core.domain.Zone;
import com.wmsdipl.core.repository.LocationRepository;
import com.wmsdipl.core.repository.PalletRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class LocationService {

    private final LocationRepository locationRepository;
    private final PalletRepository palletRepository;

    public LocationService(LocationRepository locationRepository, PalletRepository palletRepository) {
        this.locationRepository = locationRepository;
        this.palletRepository = palletRepository;
    }

    public List<Location> getAll() {
        return locationRepository.findAll();
    }

    public Optional<Location> getById(Long id) {
        return locationRepository.findById(id);
    }

    public Optional<Location> getByCode(String code) {
        return locationRepository.findByCode(code);
    }

    public List<Location> getByZone(Zone zone) {
        return locationRepository.findByZone(zone);
    }

    @Transactional
    public Location create(Location location) {
        // Validate unique code
        if (locationRepository.existsByCode(location.getCode())) {
            throw new IllegalArgumentException("Location with code '" + location.getCode() + "' already exists");
        }
        return locationRepository.save(location);
    }

    @Transactional
    public Location update(Long id, Location update) {
        Location existing = locationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Location not found: " + id));
        
        // Check if location is occupied (has pallets)
        long palletCount = palletRepository.countByLocation(existing);
        if (palletCount > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ячейка занята, изменение невозможно");
        }
        
        // Check if code is being changed and if new code already exists
        if (!existing.getCode().equals(update.getCode()) && locationRepository.existsByCode(update.getCode())) {
            throw new IllegalArgumentException("Location with code '" + update.getCode() + "' already exists");
        }
        
        existing.setZone(update.getZone());
        existing.setCode(update.getCode());
        existing.setAisle(update.getAisle());
        existing.setBay(update.getBay());
        existing.setLevel(update.getLevel());
        existing.setXCoord(update.getXCoord());
        existing.setYCoord(update.getYCoord());
        existing.setZCoord(update.getZCoord());
        existing.setMaxWeightKg(update.getMaxWeightKg());
        existing.setMaxHeightCm(update.getMaxHeightCm());
        existing.setMaxWidthCm(update.getMaxWidthCm());
        existing.setMaxDepthCm(update.getMaxDepthCm());
        existing.setMaxPallets(update.getMaxPallets());
        existing.setLocationType(update.getLocationType());
        existing.setStatus(update.getStatus());
        existing.setActive(update.getActive());
        return locationRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Location not found: " + id));
        
        // Only allow deletion if location is not occupied
        if (location.getStatus() == LocationStatus.OCCUPIED) {
            throw new IllegalStateException("Cannot delete occupied location: " + location.getCode());
        }
        
        locationRepository.deleteById(id);
    }

    /**
     * Blocks a location, preventing it from being used for new placements.
     *
     * @param id location ID
     * @return updated location
     */
    @Transactional
    public Location blockLocation(Long id) {
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Location not found: " + id));
        
        if (location.getStatus() == LocationStatus.BLOCKED) {
            throw new IllegalStateException("Location is already blocked: " + location.getCode());
        }
        
        if (location.getStatus() == LocationStatus.OCCUPIED) {
            throw new IllegalStateException("Cannot block occupied location: " + location.getCode());
        }
        
        location.setStatus(LocationStatus.BLOCKED);
        return locationRepository.save(location);
    }

    /**
     * Unblocks a location, making it available for use.
     *
     * @param id location ID
     * @return updated location
     */
    @Transactional
    public Location unblockLocation(Long id) {
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Location not found: " + id));
        
        if (location.getStatus() != LocationStatus.BLOCKED) {
            throw new IllegalStateException("Location is not blocked: " + location.getCode());
        }
        
        location.setStatus(LocationStatus.AVAILABLE);
        return locationRepository.save(location);
    }

    /**
     * Updates location status directly.
     *
     * @param id location ID
     * @param status new status
     * @return updated location
     */
    @Transactional
    public Location updateStatus(Long id, LocationStatus status) {
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Location not found: " + id));
        
        location.setStatus(status);
        return locationRepository.save(location);
    }
}
