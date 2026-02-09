package com.wmsdipl.desktop.model;

public record AutoAssignPreviewItem(
    Long taskId,
    String currentAssignee,
    String suggestedAssignee,
    Integer suggestedAssigneeLoadBeforeAssign,
    String decision
) {
}
