package com.wmsdipl.core.service;

import com.wmsdipl.core.domain.Discrepancy;
import com.wmsdipl.core.domain.Receipt;
import com.wmsdipl.core.domain.ReceiptLine;
import com.wmsdipl.core.domain.Sku;
import com.wmsdipl.core.domain.SkuStatus;
import com.wmsdipl.core.repository.DiscrepancyRepository;
import com.wmsdipl.core.repository.SkuRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class ReceiptWorkflowBlockerService {

    private final SkuRepository skuRepository;
    private final DiscrepancyRepository discrepancyRepository;

    public ReceiptWorkflowBlockerService(
        SkuRepository skuRepository,
        DiscrepancyRepository discrepancyRepository
    ) {
        this.skuRepository = skuRepository;
        this.discrepancyRepository = discrepancyRepository;
    }

    public void assertNoSkuStatusBlockers(Receipt receipt, String operation) {
        if (receipt == null || receipt.getId() == null) {
            return;
        }

        List<ReceiptWorkflowBlockedException.Blocker> blockers = new ArrayList<>();
        if (receipt.getLines() != null) {
            for (ReceiptLine line : receipt.getLines()) {
                if (line == null || Boolean.TRUE.equals(line.getExcludedFromWorkflow()) || line.getSkuId() == null) {
                    continue;
                }
                Sku sku = skuRepository.findById(line.getSkuId()).orElse(null);
                if (isBlockingSkuStatus(sku)) {
                    blockers.add(new ReceiptWorkflowBlockedException.Blocker(
                        "LINE_SKU_NOT_ACTIVE",
                        "LINE",
                        line.getId(),
                        line.getLineNo(),
                        null,
                        sku.getId(),
                        sku.getCode(),
                        sku.getStatus().name(),
                        "Receipt line references SKU with non-active status"
                    ));
                }
            }
        }

        for (Discrepancy discrepancy : discrepancyRepository.findByReceipt(receipt)) {
            if (discrepancy == null || discrepancy.getDraftSkuId() == null) {
                continue;
            }
            Sku draftSku = skuRepository.findById(discrepancy.getDraftSkuId()).orElse(null);
            if (isBlockingSkuStatus(draftSku)) {
                blockers.add(new ReceiptWorkflowBlockedException.Blocker(
                    "DISCREPANCY_SKU_PENDING_RESOLUTION",
                    "DISCREPANCY",
                    discrepancy.getLine() != null ? discrepancy.getLine().getId() : null,
                    discrepancy.getLine() != null ? discrepancy.getLine().getLineNo() : null,
                    discrepancy.getId(),
                    draftSku.getId(),
                    draftSku.getCode(),
                    draftSku.getStatus().name(),
                    "Discrepancy references DRAFT/REJECTED SKU. Approve, reject, or remap is required"
                ));
            }
        }

        List<ReceiptWorkflowBlockedException.Blocker> uniqueBlockers = deduplicate(blockers);
        if (!uniqueBlockers.isEmpty()) {
            throw new ReceiptWorkflowBlockedException(receipt.getId(), operation, uniqueBlockers);
        }
    }

    private boolean isBlockingSkuStatus(Sku sku) {
        return sku != null && (sku.getStatus() == SkuStatus.DRAFT || sku.getStatus() == SkuStatus.REJECTED);
    }

    private List<ReceiptWorkflowBlockedException.Blocker> deduplicate(
        List<ReceiptWorkflowBlockedException.Blocker> blockers
    ) {
        Map<String, ReceiptWorkflowBlockedException.Blocker> dedup = new LinkedHashMap<>();
        for (ReceiptWorkflowBlockedException.Blocker blocker : blockers) {
            String key = String.join(":",
                Objects.toString(blocker.code(), ""),
                Objects.toString(blocker.scope(), ""),
                Objects.toString(blocker.lineId(), ""),
                Objects.toString(blocker.discrepancyId(), ""),
                Objects.toString(blocker.skuId(), "")
            );
            dedup.putIfAbsent(key, blocker);
        }
        return List.copyOf(dedup.values());
    }
}
