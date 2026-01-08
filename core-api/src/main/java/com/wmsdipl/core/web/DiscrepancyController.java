package com.wmsdipl.core.web;

import com.wmsdipl.core.domain.Discrepancy;
import com.wmsdipl.core.domain.Receipt;
import com.wmsdipl.core.repository.ReceiptRepository;
import com.wmsdipl.core.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/discrepancies")
@Tag(name = "Discrepancies", description = "Discrepancy management for tracking and resolving warehouse exceptions")
public class DiscrepancyController {

    private final TaskService taskService;
    private final ReceiptRepository receiptRepository;

    public DiscrepancyController(TaskService taskService, ReceiptRepository receiptRepository) {
        this.taskService = taskService;
        this.receiptRepository = receiptRepository;
    }

    @GetMapping
    @Operation(summary = "List discrepancies", description = "Retrieves discrepancies, optionally filtered by receipt ID")
    public List<Discrepancy> list(@RequestParam(required = false) Long receiptId) {
        if (receiptId == null) {
            return taskService.findOpenDiscrepancies();
        }
        Receipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new IllegalArgumentException("Receipt not found: " + receiptId));
        return receipt.getId() == null ? List.of() : receiptRepository.findById(receiptId)
                .map(r -> taskService.findOpenDiscrepancies().stream()
                        .filter(d -> d.getReceipt() != null && d.getReceipt().getId().equals(receiptId))
                        .toList())
                .orElse(List.of());
    }

    @PostMapping("/{id}/resolve")
    @Operation(summary = "Resolve discrepancy", description = "Marks a discrepancy as resolved with a comment")
    public Discrepancy resolve(@PathVariable Long id, @RequestBody ResolveRequest req) {
        return taskService.resolveDiscrepancy(id, req.comment);
    }

    private static class ResolveRequest {
        public String comment;
    }
}
