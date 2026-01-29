package com.wmsdipl.core.web;

import com.wmsdipl.contracts.dto.CreateZoneRequest;
import com.wmsdipl.contracts.dto.UpdateZoneRequest;
import com.wmsdipl.contracts.dto.ZoneDto;
import com.wmsdipl.core.domain.Zone;
import com.wmsdipl.core.mapper.ZoneMapper;
import com.wmsdipl.core.service.ZoneService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/zones")
@Tag(name = "Zones", description = "Warehouse zone management for logical storage areas")
public class ZoneController {

    private final ZoneService zoneService;
    private final ZoneMapper zoneMapper;

    public ZoneController(ZoneService zoneService, ZoneMapper zoneMapper) {
        this.zoneService = zoneService;
        this.zoneMapper = zoneMapper;
    }

    @GetMapping
    @Operation(summary = "List all zones", description = "Retrieves all warehouse zones (e.g., receiving, picking, shipping)")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'PC_OPERATOR')")
    public List<ZoneDto> getAll() {
        return zoneService.getAll().stream()
                .map(zoneMapper::toDto)
                .toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get zone by ID", description = "Retrieves a single zone by its unique identifier")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'PC_OPERATOR')")
    public ResponseEntity<ZoneDto> getById(@PathVariable Long id) {
        return zoneService.getById(id)
                .map(zoneMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create zone", description = "Creates a new warehouse zone (ADMIN only)")
    public ResponseEntity<ZoneDto> create(@Valid @RequestBody CreateZoneRequest request) {
        Zone zone = zoneMapper.toEntity(request);
        Zone created = zoneService.create(zone);
        ZoneDto dto = zoneMapper.toDto(created);
        return ResponseEntity.created(URI.create("/api/zones/" + created.getId())).body(dto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update zone", description = "Updates an existing zone's details (ADMIN only)")
    public ResponseEntity<ZoneDto> update(@PathVariable Long id, @Valid @RequestBody UpdateZoneRequest request) {
        Zone existing = zoneService.getById(id)
                .orElseThrow(() -> new IllegalArgumentException("Zone not found: " + id));
        zoneMapper.updateEntity(existing, request);
        Zone updated = zoneService.update(id, existing);
        return ResponseEntity.ok(zoneMapper.toDto(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete zone", description = "Removes a zone from the system (ADMIN only)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        zoneService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
