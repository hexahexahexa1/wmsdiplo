package com.wmsdipl.contracts.dto;

import java.util.List;

public record AutoAssignRequest(
    List<Long> taskIds,
    Boolean reassignAssigned
) {
}
