package com.wmsdipl.contracts.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Request to bulk assign tasks to an operator.
 */
public record BulkAssignRequest(
    @NotEmpty(message = "Task IDs list cannot be empty")
    List<@NotNull Long> taskIds,
    
    @NotBlank(message = "Assignee is required")
    String assignee
) {
}
