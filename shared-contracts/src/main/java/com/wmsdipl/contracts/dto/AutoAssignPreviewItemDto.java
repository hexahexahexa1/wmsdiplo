package com.wmsdipl.contracts.dto;

public record AutoAssignPreviewItemDto(
    Long taskId,
    String currentAssignee,
    String suggestedAssignee,
    Integer suggestedAssigneeLoadBeforeAssign,
    String decision
) {
}
