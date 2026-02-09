package com.wmsdipl.desktop.model;

import java.util.List;

public record ShippingWaveActionResult(
    String outboundRef,
    Integer targetedReceipts,
    Integer affectedReceipts,
    Integer tasksCreated,
    List<Long> blockedReceiptIds,
    List<String> warnings
) {
}
