package com.wmsdipl.core.service;

import com.wmsdipl.core.domain.Location;
import com.wmsdipl.core.domain.Zone;
import com.wmsdipl.core.repository.LocationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class LocationService {

    private final LocationRepository locationRepository;

    public LocationService(LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
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
        return locationRepository.save(location);
    }

    @Transactional
    public Location update(Long id, Location update) {
        Location existing = locationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Location not found: " + id));
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
        existing.setStatus(update.getStatus());
        existing.setActive(update.getActive());
        return locationRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        locationRepository.deleteById(id);
    }
}
