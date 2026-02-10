package com.wmsdipl.desktop.model;

import java.math.BigDecimal;

public record UndoLastScanResult(
    Long taskId,
    Long scanId,
    String taskType,
    BigDecimal qtyDoneBefore,
    BigDecimal qtyDoneAfter,
    String palletCode,
    Boolean movementRolledBack,
    Integer discrepanciesRolledBack
) {
}
