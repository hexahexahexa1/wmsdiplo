package com.wmsdipl.core.web;

import com.wmsdipl.contracts.dto.PalletDto;
import com.wmsdipl.core.domain.Pallet;
import com.wmsdipl.core.domain.PalletMovement;
import com.wmsdipl.core.mapper.PalletMapper;
import com.wmsdipl.core.service.CsvExportService;
import com.wmsdipl.core.service.PalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/pallets")
@Tag(name = "Pallets", description = "Pallet management including SSCC generation and movement tracking")
public class PalletController {

    private final PalletService palletService;
    private final PalletMapper palletMapper;
    private final CsvExportService csvExportService;

    public PalletController(PalletService palletService, PalletMapper palletMapper, CsvExportService csvExportService) {
        this.palletService = palletService;
        this.palletMapper = palletMapper;
        this.csvExportService = csvExportService;
    }

    @GetMapping
    @Operation(summary = "List all pallets", description = "Retrieves all pallets in the warehouse")
    public List<PalletDto> getAll() {
        return palletService.getAll().stream()
                .map(palletMapper::toDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get pallet by ID", description = "Retrieves a single pallet by its unique identifier")
    public ResponseEntity<PalletDto> getById(@PathVariable Long id) {
        return palletService.getById(id)
                .map(palletMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create pallet", description = "Creates a new pallet record")
    public ResponseEntity<Pallet> create(@RequestBody Pallet pallet) {
        Pallet created = palletService.create(pallet);
        return ResponseEntity.created(URI.create("/api/pallets/" + created.getId())).body(created);
    }

    @PostMapping("/batch")
    @Operation(summary = "Create multiple pallets", description = "Creates multiple pallet records in a single batch operation for improved efficiency")
    public ResponseEntity<List<PalletDto>> createBatch(@RequestBody List<Pallet> pallets) {
        if (pallets == null || pallets.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        List<Pallet> created = palletService.createBatch(pallets);
        List<PalletDto> dtos = created.stream()
                .map(palletMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/generate")
    @Operation(summary = "Generate pallet codes", description = "Generates SSCC or internal pallet codes for labeling")
    public ResponseEntity<List<String>> generate(@RequestBody GenerateRequest request) {
        int count = request.count == null ? 1 : request.count;
        if (Boolean.TRUE.equals(request.sscc)) {
            return ResponseEntity.ok(palletService.generateSSCC(request.companyPrefix, count));
        }
        String codeType = request.codeType == null ? "INTERNAL" : request.codeType;
        return ResponseEntity.ok(palletService.generateInternal(request.prefix, count, codeType));
    }

    @GetMapping("/{id}/movements")
    @Operation(summary = "Get pallet movements", description = "Retrieves the movement history for a specific pallet")
    public List<PalletMovement> movements(@PathVariable Long id) {
        return palletService.getMovements(id);
    }

    @PutMapping("/{id}/location")
    @Operation(summary = "Move pallet", description = "Moves a pallet to a new location and records the movement")
    public ResponseEntity<Pallet> move(@PathVariable Long id, @RequestBody MoveRequest request) {
        if (request.locationId == null) {
            return ResponseEntity.badRequest().build();
        }
        Pallet updated = palletService.move(id, request.locationId, request.movementType, request.movedBy);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/export")
    @Operation(summary = "Export all pallets to CSV", description = "Downloads all pallets as CSV file")
    public ResponseEntity<byte[]> exportAllPallets() {
        List<Pallet> pallets = palletService.getAll();
        byte[] csv = csvExportService.exportPallets(pallets);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "pallets.csv");

        return ResponseEntity.ok()
                .headers(headers)
                .body(csv);
    }

    private static class GenerateRequest {
        public String prefix;
        public Integer count;
        public Boolean sscc;
        public String companyPrefix;
        public String codeType;
    }

    private static class MoveRequest {
        public Long locationId;
        public String movementType;
        public String movedBy;
    }
}
