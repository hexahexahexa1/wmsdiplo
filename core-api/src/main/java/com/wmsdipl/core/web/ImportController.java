package com.wmsdipl.core.web;

import com.wmsdipl.contracts.dto.ImportPayload;
import com.wmsdipl.contracts.dto.ReceiptDto;
import com.wmsdipl.core.service.ReceiptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/imports")
@Tag(name = "Imports", description = "Import operations for creating receipts from external data sources")
public class ImportController {

    private final ReceiptService receiptService;

    public ImportController(ReceiptService receiptService) {
        this.receiptService = receiptService;
    }

    @PostMapping
    @Operation(summary = "Create receipt from import", description = "Creates a new receipt from an external import payload (e.g., XML from import-service)")
    public ResponseEntity<ReceiptDto> createFromImport(@RequestBody @Valid ImportPayload payload) {
        return ResponseEntity.status(HttpStatus.CREATED).body(receiptService.createFromImport(payload));
    }
}

