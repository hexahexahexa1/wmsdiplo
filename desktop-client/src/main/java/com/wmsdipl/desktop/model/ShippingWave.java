package com.wmsdipl.desktop.model;

public record ShippingWave(
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
