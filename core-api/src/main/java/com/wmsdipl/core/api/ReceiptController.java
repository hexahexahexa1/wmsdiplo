package com.wmsdipl.core.api;

import com.wmsdipl.core.api.dto.CreateReceiptRequest;
import com.wmsdipl.core.api.dto.ReceiptDto;
import com.wmsdipl.core.api.dto.ReceiptLineDto;
import com.wmsdipl.core.service.ReceiptService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/receipts")
public class ReceiptController {

    private final ReceiptService receiptService;

    public ReceiptController(ReceiptService receiptService) {
        this.receiptService = receiptService;
    }

    @GetMapping
    public List<ReceiptDto> list() {
        return receiptService.list();
    }

    @GetMapping("/{id}")
    public ReceiptDto get(@PathVariable Long id) {
        return receiptService.get(id);
    }

    @GetMapping("/{id}/lines")
    public List<ReceiptLineDto> lines(@PathVariable Long id) {
        return receiptService.listLines(id);
    }

    @PostMapping
    public ResponseEntity<ReceiptDto> create(@RequestBody @Valid CreateReceiptRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(receiptService.createManual(request));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<Void> confirm(@PathVariable Long id) {
        receiptService.confirm(id);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<Void> accept(@PathVariable Long id) {
        receiptService.accept(id);
        return ResponseEntity.accepted().build();
    }
}
