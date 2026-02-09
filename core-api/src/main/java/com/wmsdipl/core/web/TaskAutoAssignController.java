package com.wmsdipl.core.web;

import com.wmsdipl.contracts.dto.AutoAssignRequest;
import com.wmsdipl.contracts.dto.AutoAssignResultDto;
import com.wmsdipl.core.service.TaskAutoAssignService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks/auto-assign")
@Tag(name = "Task Auto-Assign", description = "Automatic task distribution by current operator load")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
public class TaskAutoAssignController {

    private final TaskAutoAssignService taskAutoAssignService;

    public TaskAutoAssignController(TaskAutoAssignService taskAutoAssignService) {
        this.taskAutoAssignService = taskAutoAssignService;
    }

    @PostMapping("/dry-run")
    @Operation(summary = "Auto-assign dry-run", description = "Returns suggested assignees without persisting changes")
    public ResponseEntity<AutoAssignResultDto> dryRun(@Valid @RequestBody AutoAssignRequest request) {
        return ResponseEntity.ok(taskAutoAssignService.dryRun(request));
    }

    @PostMapping("/apply")
    @Operation(summary = "Auto-assign apply", description = "Applies automatic assignment suggestions")
    public ResponseEntity<AutoAssignResultDto> apply(@Valid @RequestBody AutoAssignRequest request) {
        return ResponseEntity.ok(taskAutoAssignService.apply(request));
    }
}
