package com.wmsdipl.core.web;

import com.wmsdipl.contracts.dto.RecordScanRequest;
import com.wmsdipl.contracts.dto.ScanDto;
import com.wmsdipl.contracts.dto.TaskDto;
import com.wmsdipl.core.domain.Discrepancy;
import com.wmsdipl.core.domain.Scan;
import com.wmsdipl.core.domain.Task;
import com.wmsdipl.core.domain.TaskType;
import com.wmsdipl.core.mapper.ScanMapper;
import com.wmsdipl.core.mapper.TaskMapper;
import com.wmsdipl.core.repository.ScanRepository;
import com.wmsdipl.core.service.TaskService;
import com.wmsdipl.core.service.workflow.PlacementWorkflowService;
import com.wmsdipl.core.service.workflow.ReceivingWorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tasks")
@Tag(name = "Tasks", description = "Warehouse task management operations including receiving, putaway, and discrepancy handling")
public class TaskController {

    private final TaskService taskService;
    private final ReceivingWorkflowService receivingWorkflowService;
    private final PlacementWorkflowService placementWorkflowService;
    private final ScanRepository scanRepository;
    private final ScanMapper scanMapper;
    private final TaskMapper taskMapper;

    public TaskController(
            TaskService taskService, 
            ReceivingWorkflowService receivingWorkflowService,
            PlacementWorkflowService placementWorkflowService,
            ScanRepository scanRepository,
            ScanMapper scanMapper,
            TaskMapper taskMapper
    ) {
        this.taskService = taskService;
        this.receivingWorkflowService = receivingWorkflowService;
        this.placementWorkflowService = placementWorkflowService;
        this.scanRepository = scanRepository;
        this.scanMapper = scanMapper;
        this.taskMapper = taskMapper;
    }

    @GetMapping
    @Operation(summary = "List all tasks", description = "Retrieves all warehouse tasks across all receipts and types")
    public List<TaskDto> all() {
        return taskService.findAll().stream()
            .map(taskMapper::toDto)
            .collect(Collectors.toList());
    }

    @GetMapping(params = "receiptId")
    @Operation(summary = "List tasks by receipt", description = "Retrieves all tasks associated with a specific receipt")
    public List<TaskDto> byReceipt(@RequestParam Long receiptId) {
        return taskService.findByReceipt(receiptId).stream()
            .map(taskMapper::toDto)
            .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get task by ID", description = "Retrieves a single task by its unique identifier")
    public TaskDto one(@PathVariable Long id) {
        return taskMapper.toDto(taskService.get(id));
    }

    @PostMapping
    @Operation(summary = "Create task", description = "Creates a new warehouse task")
    public ResponseEntity<TaskDto> create(@RequestBody Task task) {
        Task created = taskService.create(task);
        return ResponseEntity.created(URI.create("/api/tasks/" + created.getId()))
            .body(taskMapper.toDto(created));
    }

    @PostMapping("/{id}/assign")
    @Operation(summary = "Assign task", description = "Assigns a task to a warehouse worker")
    public TaskDto assign(@PathVariable Long id, @RequestBody AssignRequest req) {
        return taskMapper.toDto(taskService.assign(id, req.assignee, req.assignedBy));
    }

    @PostMapping("/{id}/start")
    @Operation(summary = "Start task", description = "Marks a task as started by the assigned worker")
    public TaskDto start(@PathVariable Long id) {
        return taskMapper.toDto(taskService.start(id));
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Complete task", description = "Marks a task as completed")
    public TaskDto complete(@PathVariable Long id) {
        return taskMapper.toDto(taskService.complete(id));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel task", description = "Cancels a task that is no longer needed")
    public TaskDto cancel(@PathVariable Long id) {
        return taskMapper.toDto(taskService.cancel(id));
    }

    @PostMapping("/{id}/release")
    @Operation(
        summary = "Release task", 
        description = "Releases an assigned or in-progress task back to NEW status, making it available for other operators. Work already done (qtyDone, scans) is preserved."
    )
    @ApiResponse(responseCode = "200", description = "Task released successfully")
    @ApiResponse(responseCode = "400", description = "Task cannot be released (invalid status)")
    @ApiResponse(responseCode = "404", description = "Task not found")
    public TaskDto release(@PathVariable Long id) {
        return taskMapper.toDto(taskService.release(id));
    }

    @GetMapping("/{id}/scans")
    @Operation(
        summary = "Get task scans", 
        description = "Returns all scans for a task, ordered by scan time (newest first). Used in terminal to display scan history."
    )
    @ApiResponse(responseCode = "200", description = "Scans retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Task not found")
    public List<ScanDto> getTaskScans(@PathVariable Long id) {
        Task task = taskService.get(id);
        List<Scan> scans = scanRepository.findByTaskOrderByScannedAtDesc(task);
        return scans.stream()
            .map(scanMapper::toDto)
            .collect(Collectors.toList());
    }

    @PostMapping("/{id}/scans")
    @Operation(summary = "Record scan", description = "Records a barcode scan during task execution (RECEIVING or PLACEMENT)")
    public ResponseEntity<ScanDto> scan(@PathVariable Long id, @RequestBody @Valid RecordScanRequest request) {
        // Route to appropriate workflow service based on task type
        Task task = taskService.get(id);
        Scan scan;
        
        if (task.getTaskType() == TaskType.RECEIVING) {
            scan = receivingWorkflowService.recordScan(id, request);
        } else if (task.getTaskType() == TaskType.PLACEMENT) {
            scan = placementWorkflowService.recordPlacement(id, request);
        } else {
            throw new ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST,
                "Unsupported task type for scan recording: " + task.getTaskType()
            );
        }
        
        ScanDto dto = scanMapper.toDto(scan);
        return ResponseEntity.created(URI.create("/api/tasks/" + id + "/scans/" + scan.getId())).body(dto);
    }

    @PostMapping("/generate")
    @Operation(summary = "Generate tasks", description = "Generates multiple tasks for a receipt (e.g., receiving or putaway tasks)")
    @ApiResponse(responseCode = "202", description = "Tasks generation accepted")
    @ApiResponse(responseCode = "400", description = "Invalid request or receipt lines missing SKU")
    @ApiResponse(responseCode = "404", description = "Receipt not found")
    public ResponseEntity<Void> generate(@RequestBody GenerateRequest req) {
        if (req.receiptId == null || req.count == null || req.count <= 0) {
            return ResponseEntity.badRequest().build();
        }
        try {
            TaskType type = req.taskType == null ? TaskType.RECEIVING : req.taskType;
            taskService.createReceivingTasks(req.receiptId, type, req.count);
            return ResponseEntity.accepted().build();
        } catch (IllegalStateException e) {
            // Validation failed - receipt lines missing SKU
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/discrepancies/open")
    @Operation(summary = "List open discrepancies", description = "Retrieves all unresolved discrepancies from warehouse tasks")
    public List<Discrepancy> openDiscrepancies() {
        return taskService.findOpenDiscrepancies();
    }

    @PostMapping("/discrepancies/{id}/resolve")
    @Operation(summary = "Resolve discrepancy", description = "Marks a discrepancy as resolved with a comment")
    public Discrepancy resolve(@PathVariable Long id, @RequestBody ResolveRequest req) {
        return taskService.resolveDiscrepancy(id, req.comment);
    }

    @GetMapping("/{id}/has-discrepancies")
    @Operation(summary = "Check task discrepancies", description = "Returns true if the task has any scans with discrepancies")
    public boolean hasDiscrepancies(@PathVariable Long id) {
        return taskService.hasDiscrepancies(id);
    }

    private static class AssignRequest {
        public String assignee;
        public String assignedBy;
    }

    private static class GenerateRequest {
        public Long receiptId;
        public Integer count;
        public TaskType taskType;
    }

    private static class ResolveRequest {
        public String comment;
    }
}
