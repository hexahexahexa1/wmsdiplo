package com.wmsdipl.core.web;

import com.wmsdipl.contracts.dto.BulkAssignRequest;
import com.wmsdipl.contracts.dto.BulkCreatePalletsRequest;
import com.wmsdipl.contracts.dto.BulkOperationResult;
import com.wmsdipl.contracts.dto.BulkSetPriorityRequest;
import com.wmsdipl.contracts.dto.PalletCreationResult;
import com.wmsdipl.core.service.BulkOperationsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for bulk operations on tasks and pallets.
 * Provides endpoints for mass assignment, priority changes, and pallet creation.
 */
@RestController
@RequestMapping("/api/bulk")
@Tag(name = "Bulk Operations", description = "Bulk operations for tasks and pallets")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
public class BulkOperationsController {

    private final BulkOperationsService bulkOperationsService;

    public BulkOperationsController(BulkOperationsService bulkOperationsService) {
        this.bulkOperationsService = bulkOperationsService;
    }

    /**
     * Bulk assign tasks to an operator.
     * 
     * POST /api/bulk/assign
     * {
     *   "taskIds": [1, 2, 3],
     *   "assignee": "operator1"
     * }
     */
    @PostMapping("/assign")
    @Operation(summary = "Bulk assign tasks", description = "Assign multiple tasks to an operator")
    public ResponseEntity<BulkOperationResult<Long>> bulkAssign(@Valid @RequestBody BulkAssignRequest request) {
        BulkOperationResult<Long> result = bulkOperationsService.bulkAssignTasks(request);
        return ResponseEntity.ok(result);
    }

    /**
     * Bulk set priority for tasks.
     * 
     * POST /api/bulk/priority
     * {
     *   "taskIds": [1, 2, 3],
     *   "priority": 200
     * }
     */
    @PostMapping("/priority")
    @Operation(summary = "Bulk set task priority", description = "Set priority for multiple tasks")
    public ResponseEntity<BulkOperationResult<Long>> bulkSetPriority(@Valid @RequestBody BulkSetPriorityRequest request) {
        BulkOperationResult<Long> result = bulkOperationsService.bulkSetPriority(request);
        return ResponseEntity.ok(result);
    }

    /**
     * Bulk create pallets with sequential numbering.
     * 
     * POST /api/bulk/pallets
     * {
     *   "startNumber": 100,
     *   "count": 50
     * }
     * 
     * Creates pallets: PLT-00100, PLT-00101, ..., PLT-00149
     */
    @PostMapping("/pallets")
    @Operation(summary = "Bulk create pallets", description = "Create multiple pallets with auto-generated codes")
    public ResponseEntity<PalletCreationResult> bulkCreatePallets(@Valid @RequestBody BulkCreatePalletsRequest request) {
        PalletCreationResult result = bulkOperationsService.bulkCreatePallets(request);
        return ResponseEntity.ok(result);
    }

    /**
     * Bulk cancel tasks.
     * 
     * POST /api/bulk/cancel
     * [1, 2, 3]
     */
    @PostMapping("/cancel")
    @Operation(summary = "Bulk cancel tasks", description = "Cancel multiple tasks")
    public ResponseEntity<BulkOperationResult<Long>> bulkCancel(@RequestBody List<Long> taskIds) {
        BulkOperationResult<Long> result = bulkOperationsService.bulkCancelTasks(taskIds);
        return ResponseEntity.ok(result);
    }
}
