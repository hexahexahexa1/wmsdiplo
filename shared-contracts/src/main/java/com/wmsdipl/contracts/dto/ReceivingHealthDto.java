package com.wmsdipl.contracts.dto;

import java.time.LocalDate;

public record ReceivingHealthDto(
    LocalDate fromDate,
    LocalDate toDate,
    Integer thresholdHours,
    Long stuckReceivingReceipts,
    Long stuckPlacingReceipts,
    Long staleTasks,
    Long autoResolvedDiscrepancies,
    Long criticalDiscrepancies
) {
}
