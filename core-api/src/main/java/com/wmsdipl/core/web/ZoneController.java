package com.wmsdipl.core.web;

import com.wmsdipl.core.domain.Zone;
import com.wmsdipl.core.service.ZoneService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/zones")
@Tag(name = "Zones", description = "Warehouse zone management for logical storage areas")
public class ZoneController {

    private final ZoneService zoneService;

    public ZoneController(ZoneService zoneService) {
        this.zoneService = zoneService;
    }

    @GetMapping
    @Operation(summary = "List all zones", description = "Retrieves all warehouse zones (e.g., receiving, picking, shipping)")
    public List<Zone> getAll() {
        return zoneService.getAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get zone by ID", description = "Retrieves a single zone by its unique identifier")
    public ResponseEntity<Zone> getById(@PathVariable Long id) {
        return zoneService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create zone", description = "Creates a new warehouse zone")
    public ResponseEntity<Zone> create(@RequestBody Zone zone) {
        Zone created = zoneService.create(zone);
        return ResponseEntity.created(URI.create("/api/zones/" + created.getId())).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update zone", description = "Updates an existing zone's details")
    public ResponseEntity<Zone> update(@PathVariable Long id, @RequestBody Zone zone) {
        return ResponseEntity.ok(zoneService.update(id, zone));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete zone", description = "Removes a zone from the system")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        zoneService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
