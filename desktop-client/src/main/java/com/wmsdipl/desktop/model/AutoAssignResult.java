package com.wmsdipl.desktop.model;

import java.util.List;

public record AutoAssignResult(
    Integer totalCandidates,
    Integer assignedCount,
    Integer skippedCount,
    List<AutoAssignPreviewItem> items
) {
}
