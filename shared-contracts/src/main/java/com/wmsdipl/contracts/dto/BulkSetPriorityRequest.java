package com.wmsdipl.contracts.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Request to bulk set priority for multiple tasks.
 */
public record BulkSetPriorityRequest(
    @NotEmpty(message = "Task IDs list cannot be empty")
    List<@NotNull Long> taskIds,
    
    @NotNull(message = "Priority is required")
    @Min(value = 1, message = "Priority must be at least 1")
    @Max(value = 999, message = "Priority must not exceed 999")
    Integer priority
) {
}
