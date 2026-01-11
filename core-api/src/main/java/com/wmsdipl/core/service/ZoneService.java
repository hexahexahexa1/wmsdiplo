package com.wmsdipl.core.service;

import com.wmsdipl.core.domain.Zone;
import com.wmsdipl.core.repository.ZoneRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ZoneService {

    private final ZoneRepository zoneRepository;

    public ZoneService(ZoneRepository zoneRepository) {
        this.zoneRepository = zoneRepository;
    }

    public List<Zone> getAll() {
        return zoneRepository.findAll();
    }

    public Optional<Zone> getById(Long id) {
        return zoneRepository.findById(id);
    }

    public Optional<Zone> getByCode(String code) {
        return zoneRepository.findByCode(code);
    }

    @Transactional
    public Zone create(Zone zone) {
        if (zoneRepository.existsByCode(zone.getCode())) {
            throw new IllegalArgumentException("Zone with code '" + zone.getCode() + "' already exists");
        }
        return zoneRepository.save(zone);
    }

    @Transactional
    public Zone update(Long id, Zone update) {
        Zone existing = zoneRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Zone not found: " + id));
        
        // Check if code is being changed and if new code already exists
        if (!existing.getCode().equals(update.getCode()) && zoneRepository.existsByCode(update.getCode())) {
            throw new IllegalArgumentException("Zone with code '" + update.getCode() + "' already exists");
        }
        
        existing.setCode(update.getCode());
        existing.setName(update.getName());
        existing.setPriorityRank(update.getPriorityRank());
        existing.setDescription(update.getDescription());
        existing.setActive(update.getActive());
        return zoneRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        Zone zone = zoneRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Zone not found: " + id));
        
        // Check if zone has locations
        if (!zone.getLocations().isEmpty()) {
            throw new IllegalStateException("Cannot delete zone with existing locations. Zone has " + 
                    zone.getLocations().size() + " location(s).");
        }
        
        zoneRepository.deleteById(id);
    }
}
