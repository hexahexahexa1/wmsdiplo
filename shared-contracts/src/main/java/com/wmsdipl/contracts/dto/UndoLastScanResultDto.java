package com.wmsdipl.contracts.dto;

import java.math.BigDecimal;

public record UndoLastScanResultDto(
    Long taskId,
    Long scanId,
    String taskType,
    BigDecimal qtyDoneBefore,
    BigDecimal qtyDoneAfter,
    String palletCode,
    boolean movementRolledBack,
    int discrepanciesRolledBack
) {
}
