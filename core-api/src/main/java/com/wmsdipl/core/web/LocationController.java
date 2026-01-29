package com.wmsdipl.core.web;

import com.wmsdipl.contracts.dto.CreateLocationRequest;
import com.wmsdipl.contracts.dto.LocationDto;
import com.wmsdipl.contracts.dto.UpdateLocationRequest;
import com.wmsdipl.core.domain.Location;
import com.wmsdipl.core.domain.Zone;
import com.wmsdipl.core.mapper.LocationMapper;
import com.wmsdipl.core.repository.ZoneRepository;
import com.wmsdipl.core.service.LocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/locations")
@Tag(name = "Locations", description = "Warehouse location management for storage positions")
public class LocationController {

    private final LocationService locationService;
    private final LocationMapper locationMapper;
    private final ZoneRepository zoneRepository;

    public LocationController(LocationService locationService, LocationMapper locationMapper, ZoneRepository zoneRepository) {
        this.locationService = locationService;
        this.locationMapper = locationMapper;
        this.zoneRepository = zoneRepository;
    }

    @GetMapping
    @Operation(summary = "List all locations", description = "Retrieves all warehouse storage locations")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'PC_OPERATOR')")
    public List<LocationDto> getAll() {
        return locationService.getAll().stream()
                .map(locationMapper::toDto)
                .toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get location by ID", description = "Retrieves a single location by its unique identifier")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'PC_OPERATOR')")
    public ResponseEntity<LocationDto> getById(@PathVariable Long id) {
        return locationService.getById(id)
                .map(locationMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create location", description = "Creates a new warehouse storage location")
    public ResponseEntity<LocationDto> create(@Valid @RequestBody CreateLocationRequest request) {
        Zone zone = zoneRepository.findById(request.zoneId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Zone not found: " + request.zoneId()));
        
        Location location = locationMapper.toEntity(request, zone);
        Location created = locationService.create(location);
        LocationDto dto = locationMapper.toDto(created);
        
        return ResponseEntity.created(URI.create("/api/locations/" + created.getId())).body(dto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update location", description = "Updates an existing location's details")
    public ResponseEntity<LocationDto> update(@PathVariable Long id, @Valid @RequestBody UpdateLocationRequest request) {
        Location location = locationService.getById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Location not found: " + id));
        
        Zone zone = null;
        if (request.zoneId() != null) {
            zone = zoneRepository.findById(request.zoneId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Zone not found: " + request.zoneId()));
        }
        
        locationMapper.updateEntity(location, request, zone);
        Location updated = locationService.update(id, location);
        
        return ResponseEntity.ok(locationMapper.toDto(updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete location", description = "Removes a location from the system")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        locationService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/block")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    @Operation(summary = "Block location", description = "Blocks a location to prevent new placements (ADMIN/SUPERVISOR only)")
    public ResponseEntity<LocationDto> blockLocation(@PathVariable Long id) {
        Location blocked = locationService.blockLocation(id);
        return ResponseEntity.ok(locationMapper.toDto(blocked));
    }

    @PostMapping("/{id}/unblock")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    @Operation(summary = "Unblock location", description = "Unblocks a location to make it available (ADMIN/SUPERVISOR only)")
    public ResponseEntity<LocationDto> unblockLocation(@PathVariable Long id) {
        Location unblocked = locationService.unblockLocation(id);
        return ResponseEntity.ok(locationMapper.toDto(unblocked));
    }
}
