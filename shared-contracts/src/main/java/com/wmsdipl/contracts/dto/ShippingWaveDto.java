package com.wmsdipl.contracts.dto;

public record ShippingWaveDto(
    String outboundRef,
    Integer totalReceipts,
    Integer readyForShipmentCount,
    Integer shippingInProgressCount,
    Integer shippedCount,
    Integer openShippingTasks,
    Integer completedShippingTasks,
    String status
) {
}
