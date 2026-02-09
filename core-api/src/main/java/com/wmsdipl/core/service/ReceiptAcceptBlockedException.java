package com.wmsdipl.core.service;

import java.util.List;

public class ReceiptAcceptBlockedException extends RuntimeException {

    private final Long receiptId;
    private final List<TaskBlocker> blockers;

    public ReceiptAcceptBlockedException(Long receiptId, List<TaskBlocker> blockers) {
        super("Cannot accept receipt: open tasks exist");
        this.receiptId = receiptId;
        this.blockers = blockers;
    }

    public Long getReceiptId() {
        return receiptId;
    }

    public List<TaskBlocker> getBlockers() {
        return blockers;
    }

    public record TaskBlocker(
        Long taskId,
        String taskType,
        String status
    ) {
    }
}
