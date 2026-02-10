package com.wmsdipl.core.web;

import com.wmsdipl.contracts.dto.CreateSkuRequest;
import com.wmsdipl.contracts.dto.SkuDto;
import com.wmsdipl.contracts.dto.SkuUnitConfigDto;
import com.wmsdipl.contracts.dto.UpsertSkuUnitConfigsRequest;
import com.wmsdipl.core.domain.SkuStatus;
import com.wmsdipl.core.service.SkuService;
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

import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * REST controller for SKU (Stock Keeping Unit) catalog management.
 */
@RestController
@RequestMapping("/api/skus")
@Tag(name = "SKUs", description = "SKU catalog management - list, create, update, and delete products")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
public class SkuController {

    private final SkuService skuService;

    public SkuController(SkuService skuService) {
        this.skuService = skuService;
    }

    @GetMapping
    @Operation(summary = "List all SKUs", description = "Returns all SKUs in the catalog")
    public List<SkuDto> listAll(@RequestParam(required = false) String status) {
        if (status == null || status.isBlank()) {
            return skuService.findAll();
        }
        try {
            return skuService.findAllByStatus(SkuStatus.valueOf(status.trim().toUpperCase()));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(BAD_REQUEST, "Unknown SKU status: " + status);
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get SKU by ID", description = "Returns a single SKU by its ID")
    public SkuDto getById(@PathVariable Long id) {
        return skuService.findById(id);
    }

    @GetMapping("/by-code/{code}")
    @Operation(summary = "Get SKU by code", description = "Returns a single SKU by its unique code")
    public ResponseEntity<SkuDto> getByCode(@PathVariable String code) {
        return skuService.findByCode(code)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create SKU", description = "Creates a new SKU in the catalog")
    public ResponseEntity<SkuDto> create(@RequestBody @Valid CreateSkuRequest request) {
        SkuDto created = skuService.create(request);
        return ResponseEntity
            .created(URI.create("/api/skus/" + created.id()))
            .body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update SKU", description = "Updates an existing SKU")
    public SkuDto update(@PathVariable Long id, @RequestBody @Valid CreateSkuRequest request) {
        return skuService.update(id, request);
    }

    @PostMapping("/{id}/approve-draft")
    @Operation(summary = "Approve draft SKU", description = "Switches SKU status from DRAFT to ACTIVE")
    public SkuDto approveDraft(@PathVariable Long id) {
        return skuService.approveDraft(id);
    }

    @PostMapping("/{id}/reject-draft")
    @Operation(summary = "Reject draft SKU", description = "Switches SKU status from DRAFT to REJECTED and excludes related lines")
    public SkuDto rejectDraft(@PathVariable Long id) {
        return skuService.rejectDraft(id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete SKU", description = "Deletes a SKU from the catalog")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        skuService.delete(id);
    }

    @GetMapping("/{id}/unit-configs")
    @Operation(summary = "Get SKU unit configs", description = "Returns configured units and palletization rules for a SKU")
    public List<SkuUnitConfigDto> getUnitConfigs(@PathVariable Long id) {
        return skuService.getUnitConfigs(id);
    }

    @PutMapping("/{id}/unit-configs")
    @Operation(summary = "Replace SKU unit configs", description = "Replaces full set of units/palletization configs for a SKU")
    public List<SkuUnitConfigDto> replaceUnitConfigs(
        @PathVariable Long id,
        @RequestBody @Valid UpsertSkuUnitConfigsRequest request
    ) {
        return skuService.replaceUnitConfigs(id, request);
    }
}
