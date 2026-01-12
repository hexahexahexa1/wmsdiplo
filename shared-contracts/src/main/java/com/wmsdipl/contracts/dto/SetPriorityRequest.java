package com.wmsdipl.contracts.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request to set task priority.
 */
public record SetPriorityRequest(
    @NotNull(message = "Priority is required")
    @Min(value = 1, message = "Priority must be at least 1")
    @Max(value = 999, message = "Priority must not exceed 999")
    Integer priority
) {
}
