package com.wmsdipl.core.web;

import com.wmsdipl.contracts.dto.CreateSkuRequest;
import com.wmsdipl.contracts.dto.SkuDto;
import com.wmsdipl.core.service.SkuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

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
    public List<SkuDto> listAll() {
        return skuService.findAll();
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

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete SKU", description = "Deletes a SKU from the catalog")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        skuService.delete(id);
    }
}
