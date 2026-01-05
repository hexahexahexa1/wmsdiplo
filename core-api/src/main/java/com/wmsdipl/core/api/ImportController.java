package com.wmsdipl.core.api;

import com.wmsdipl.core.api.dto.ImportPayload;
import com.wmsdipl.core.api.dto.ReceiptDto;
import com.wmsdipl.core.service.ReceiptService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/imports")
public class ImportController {

    private final ReceiptService receiptService;

    public ImportController(ReceiptService receiptService) {
        this.receiptService = receiptService;
    }

    @PostMapping
    public ResponseEntity<ReceiptDto> createFromImport(@RequestBody @Valid ImportPayload payload) {
        return ResponseEntity.status(HttpStatus.CREATED).body(receiptService.createFromImport(payload));
    }
}
