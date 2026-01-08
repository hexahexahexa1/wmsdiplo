package com.wmsdipl.core.web;

import com.wmsdipl.core.domain.Location;
import com.wmsdipl.core.service.LocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/locations")
@Tag(name = "Locations", description = "Warehouse location management for storage positions")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping
    @Operation(summary = "List all locations", description = "Retrieves all warehouse storage locations")
    public List<Location> getAll() {
        return locationService.getAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get location by ID", description = "Retrieves a single location by its unique identifier")
    public ResponseEntity<Location> getById(@PathVariable Long id) {
        return locationService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create location", description = "Creates a new warehouse storage location")
    public ResponseEntity<Location> create(@RequestBody Location location) {
        Location created = locationService.create(location);
        return ResponseEntity.created(URI.create("/api/locations/" + created.getId())).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update location", description = "Updates an existing location's details")
    public ResponseEntity<Location> update(@PathVariable Long id, @RequestBody Location location) {
        return ResponseEntity.ok(locationService.update(id, location));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete location", description = "Removes a location from the system")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        locationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
