package com.wmsdipl.core.web;

import com.wmsdipl.contracts.dto.CreateReceiptRequest;
import com.wmsdipl.contracts.dto.ReceiptDto;
import com.wmsdipl.contracts.dto.ReceiptLineDto;
import com.wmsdipl.core.service.workflow.PlacementWorkflowService;
import com.wmsdipl.core.service.workflow.ReceivingWorkflowService;
import com.wmsdipl.core.service.ReceiptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/receipts")
@Tag(name = "Receipts", description = "Receipt management and workflow operations")
public class ReceiptController {

    private final ReceiptService receiptService;
    private final ReceivingWorkflowService receivingWorkflowService;
    private final PlacementWorkflowService placementWorkflowService;

    public ReceiptController(ReceiptService receiptService,
                             ReceivingWorkflowService receivingWorkflowService,
                             PlacementWorkflowService placementWorkflowService) {
        this.receiptService = receiptService;
        this.receivingWorkflowService = receivingWorkflowService;
        this.placementWorkflowService = placementWorkflowService;
    }

    @GetMapping
    @Operation(summary = "List all receipts", description = "Returns a list of all receipt records")
    public List<ReceiptDto> list() {
        return receiptService.list();
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
    public ResponseEntity<Void> startReceiving(@PathVariable Long id) {
        receivingWorkflowService.startReceiving(id);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/{id}/complete-receiving")
    @Operation(summary = "Complete receiving", description = "Completes receiving workflow - transitions to ACCEPTED status")
    public ResponseEntity<Void> completeReceiving(@PathVariable Long id) {
        receivingWorkflowService.completeReceiving(id);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/{id}/accept")
    @Operation(summary = "Accept receipt", description = "Accepts the receipt after receiving is complete")
    public ResponseEntity<Void> accept(@PathVariable Long id) {
        receivingWorkflowService.completeReceiving(id);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/{id}/resolve-pending")
    @Operation(summary = "Resolve pending issues", description = "Resolves pending discrepancies and continues workflow")
    public ResponseEntity<Void> resolvePending(@PathVariable Long id) {
        receivingWorkflowService.resolveAndContinue(id);
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
    public ResponseEntity<Void> startPlacement(@PathVariable Long id) {
        placementWorkflowService.startPlacement(id);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/{id}/complete-placement")
    @Operation(summary = "Complete placement (manual)", description = "Manually completes placement workflow - transitions to STOCKED status. Note: Receipt auto-completes to STOCKED when all placement tasks are done.")
    public ResponseEntity<Void> completePlacement(@PathVariable Long id) {
        placementWorkflowService.completePlacement(id);
        return ResponseEntity.accepted().build();
    }
}

