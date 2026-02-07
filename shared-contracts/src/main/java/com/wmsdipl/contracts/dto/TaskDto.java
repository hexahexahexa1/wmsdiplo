package com.wmsdipl.contracts.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TaskDto(
    Long id,
    String taskType,
    String status,
    String assignee,
    String assignedBy,
    Long palletId,
    Long sourceLocationId,
    Long targetLocationId,
    String targetLocationCode,
    Long receiptId,
    String receiptDocNo,
    Long lineId,
    Long skuId,
    BigDecimal qtyAssigned,
    BigDecimal qtyDone,
    String lineUom,
    String baseUom,
    BigDecimal unitFactorToBase,
    String skuCode,
    String palletCode,
    Integer priority,
    LocalDateTime createdAt,
    LocalDateTime startedAt,
    LocalDateTime closedAt
) {}
