package com.wmsdipl.contracts.dto;

import java.util.List;

public record AutoAssignResultDto(
    Integer totalCandidates,
    Integer assignedCount,
    Integer skippedCount,
    List<AutoAssignPreviewItemDto> items
) {
}
