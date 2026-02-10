package com.wmsdipl.core.service;

import java.util.List;

public class ReceiptWorkflowBlockedException extends RuntimeException {

    private final Long receiptId;
    private final String operation;
    private final List<Blocker> blockers;

    public ReceiptWorkflowBlockedException(Long receiptId, String operation, List<Blocker> blockers) {
        super("Receipt workflow is blocked for operation: " + operation);
        this.receiptId = receiptId;
        this.operation = operation;
        this.blockers = blockers;
    }

    public Long getReceiptId() {
        return receiptId;
    }

    public String getOperation() {
        return operation;
    }

    public List<Blocker> getBlockers() {
        return blockers;
    }

    public record Blocker(
        String code,
        String scope,
        Long lineId,
        Integer lineNo,
        Long discrepancyId,
        Long skuId,
        String skuCode,
        String skuStatus,
        String message
    ) {
    }
}
