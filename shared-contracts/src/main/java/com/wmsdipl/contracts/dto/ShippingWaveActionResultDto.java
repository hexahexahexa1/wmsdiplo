package com.wmsdipl.contracts.dto;

import java.util.List;

public record ShippingWaveActionResultDto(
    String outboundRef,
    Integer targetedReceipts,
    Integer affectedReceipts,
    Integer tasksCreated,
    List<Long> blockedReceiptIds,
    List<String> warnings
) {
}
