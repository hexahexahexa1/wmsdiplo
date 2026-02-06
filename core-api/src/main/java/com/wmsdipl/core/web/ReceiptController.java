package com.wmsdipl.core.web;

import com.wmsdipl.contracts.dto.CreateReceiptRequest;
import com.wmsdipl.contracts.dto.ReceiptDiscrepancyDto;
import com.wmsdipl.contracts.dto.ReceiptDto;
import com.wmsdipl.contracts.dto.ReceiptLineDto;
import com.wmsdipl.contracts.dto.ReceiptSummaryDto;
import com.wmsdipl.core.domain.Receipt;
import com.wmsdipl.core.repository.ReceiptRepository;
import com.wmsdipl.core.service.CsvExportService;
import com.wmsdipl.core.service.workflow.PlacementWorkflowService;
import com.wmsdipl.core.service.workflow.ReceivingWorkflowService;
import com.wmsdipl.core.service.ReceiptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/receipts")
@Tag(name = "Receipts", description = "Receipt management and workflow operations")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'PC_OPERATOR')")
public class ReceiptController {

    private final ReceiptService receiptService;
    private final ReceivingWorkflowService receivingWorkflowService;
    private final PlacementWorkflowService placementWorkflowService;
    private final CsvExportService csvExportService;
    private final ReceiptRepository receiptRepository;

    public ReceiptController(ReceiptService receiptService,
                             ReceivingWorkflowService receivingWorkflowService,
                             PlacementWorkflowService placementWorkflowService,
                             CsvExportService csvExportService,
                             ReceiptRepository receiptRepository) {
        this.receiptService = receiptService;
        this.receivingWorkflowService = receivingWorkflowService;
        this.placementWorkflowService = placementWorkflowService;
        this.csvExportService = csvExportService;
        this.receiptRepository = receiptRepository;
    }

    @GetMapping
    @Operation(summary = "List all receipts", description = "Returns a list of all receipt records")
    public List<ReceiptDto> list() {
        return receiptService.list();
    }

    @GetMapping(params = {"page", "size"})
    @Operation(summary = "List receipts with filters and pagination", description = "Returns paginated receipts with optional status/supplier/date filters")
    public Page<ReceiptDto> listFiltered(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String supplier,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate toDate,
            @PageableDefault(size = 50, sort = "createdAt") Pageable pageable
    ) {
        return receiptService.listFiltered(status, supplier, fromDate, toDate, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get receipt by ID", description = "Returns a single receipt by its ID")
    public ReceiptDto get(@PathVariable Long id) {
        return receiptService.get(id);
    }

    @GetMapping("/{id}/lines")
    @Operation(summary = "Get receipt lines", description = "Returns all line items for a specific receipt")
    public List<ReceiptLineDto> lines(@PathVariable Long id) {
        return receiptService.listLines(id);
    }

    @PostMapping
    @Operation(summary = "Create receipt", description = "Creates a new receipt in DRAFT status")
    public ResponseEntity<ReceiptDto> create(@RequestBody @Valid CreateReceiptRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(receiptService.createManual(request));
    }

    @PostMapping("/{id}/confirm")
    @Operation(summary = "Confirm receipt", description = "Transitions receipt from DRAFT to CONFIRMED status")
    public ResponseEntity<Void> confirm(@PathVariable Long id) {
        receiptService.confirm(id);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/{id}/start-receiving")
    @Operation(summary = "Start receiving", description = "Begins receiving workflow - transitions to RECEIVING status")
    public ResponseEntity<java.util.Map<String, Integer>> startReceiving(@PathVariable Long id) {
        int count = receivingWorkflowService.startReceiving(id);
        return ResponseEntity.accepted().body(java.util.Map.of("count", count));
    }

    @PostMapping("/{id}/complete-receiving")
    @Operation(summary = "Complete receiving", description = "Completes receiving workflow - transitions to ACCEPTED status")
    public ResponseEntity<Void> completeReceiving(@PathVariable Long id) {
        receivingWorkflowService.completeReceiving(id);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/{id}/accept")
    @Operation(summary = "Accept receipt", description = "Legacy manual acceptance endpoint. Uses explicit receipt acceptance semantics.")
    public ResponseEntity<Void> accept(@PathVariable Long id) {
        receiptService.accept(id);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel receipt", description = "Cancels the receipt workflow")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        receivingWorkflowService.cancel(id);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/{id}/start-placement")
    @Operation(summary = "Start placement", description = "Begins placement workflow - automatically generates placement tasks and transitions to PLACING status")
    public ResponseEntity<java.util.Map<String, Integer>> startPlacement(@PathVariable Long id) {
        int count = placementWorkflowService.startPlacement(id);
        return ResponseEntity.accepted().body(java.util.Map.of("count", count));
    }

    @PostMapping("/{id}/complete-placement")
    @Operation(summary = "Complete placement (manual)", description = "Manually completes placement workflow - transitions to STOCKED status. Note: Receipt auto-completes to STOCKED when all placement tasks are done.")
    public ResponseEntity<Void> completePlacement(@PathVariable Long id) {
        placementWorkflowService.completePlacement(id);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/{id}/summary")
    @Operation(summary = "Get receipt summary", description = "Returns summary report with expected vs received quantities by line")
    public ReceiptSummaryDto getSummary(@PathVariable Long id) {
        return receiptService.getSummary(id);
    }

    @GetMapping("/{id}/discrepancies")
    @Operation(summary = "Get receipt discrepancies", description = "Returns detailed discrepancy report comparing expected vs actual quantities")
    public ReceiptDiscrepancyDto getDiscrepancies(@PathVariable Long id) {
        return receiptService.getDiscrepancies(id);
    }

    @GetMapping("/export")
    @Operation(summary = "Export all receipts to CSV", description = "Downloads all receipts as CSV file")
    public ResponseEntity<byte[]> exportAllReceipts() {
        List<Receipt> receipts = receiptRepository.findAll();
        byte[] csv = csvExportService.exportReceipts(receipts);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "receipts.csv");

        return ResponseEntity.ok()
                .headers(headers)
                .body(csv);
    }

    @GetMapping("/{id}/export")
    @Operation(summary = "Export receipt with lines to CSV", description = "Downloads receipt with all lines as CSV file")
    public ResponseEntity<byte[]> exportReceiptWithLines(@PathVariable Long id) {
        Receipt receipt = receiptRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Receipt not found"));
        
        byte[] csv = csvExportService.exportReceiptWithLines(receipt);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "receipt_" + receipt.getDocNo() + ".csv");

        return ResponseEntity.ok()
                .headers(headers)
                .body(csv);
    }
}
