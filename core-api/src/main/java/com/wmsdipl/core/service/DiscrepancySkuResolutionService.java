package com.wmsdipl.core.service;

import com.wmsdipl.core.domain.Discrepancy;
import com.wmsdipl.core.domain.Pallet;
import com.wmsdipl.core.domain.ReceiptLine;
import com.wmsdipl.core.domain.Sku;
import com.wmsdipl.core.domain.SkuStatus;
import com.wmsdipl.core.domain.SkuUnitConfig;
import com.wmsdipl.core.repository.DiscrepancyRepository;
import com.wmsdipl.core.repository.PalletRepository;
import com.wmsdipl.core.repository.ReceiptLineRepository;
import com.wmsdipl.core.repository.SkuRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class DiscrepancySkuResolutionService {

    private final DiscrepancyRepository discrepancyRepository;
    private final SkuRepository skuRepository;
    private final ReceiptLineRepository receiptLineRepository;
    private final PalletRepository palletRepository;
    private final SkuService skuService;
    private final AuditLogService auditLogService;

    public DiscrepancySkuResolutionService(
        DiscrepancyRepository discrepancyRepository,
        SkuRepository skuRepository,
        ReceiptLineRepository receiptLineRepository,
        PalletRepository palletRepository,
        SkuService skuService,
        AuditLogService auditLogService
    ) {
        this.discrepancyRepository = discrepancyRepository;
        this.skuRepository = skuRepository;
        this.receiptLineRepository = receiptLineRepository;
        this.palletRepository = palletRepository;
        this.skuService = skuService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public Discrepancy remapDiscrepancySku(Long discrepancyId, Long targetSkuId) {
        if (targetSkuId == null) {
            throw new ResponseStatusException(BAD_REQUEST, "targetSkuId is required");
        }

        Discrepancy discrepancy = discrepancyRepository.findById(discrepancyId)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Discrepancy not found: " + discrepancyId));
        if (discrepancy.getReceipt() == null || discrepancy.getReceipt().getId() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Discrepancy is not linked to a receipt");
        }
        Long receiptId = discrepancy.getReceipt().getId();

        Long sourceSkuId = resolveSourceSkuId(discrepancy);
        if (sourceSkuId == null) {
            throw new ResponseStatusException(BAD_REQUEST, "No source DRAFT/REJECTED SKU linked to discrepancy");
        }

        Sku sourceSku = skuRepository.findById(sourceSkuId)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Source SKU not found: " + sourceSkuId));
        if (sourceSku.getStatus() != SkuStatus.DRAFT && sourceSku.getStatus() != SkuStatus.REJECTED) {
            throw new ResponseStatusException(BAD_REQUEST, "Source SKU must be DRAFT or REJECTED");
        }

        Sku targetSku = skuRepository.findById(targetSkuId)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Target SKU not found: " + targetSkuId));
        if (targetSku.getStatus() != SkuStatus.ACTIVE) {
            throw new ResponseStatusException(BAD_REQUEST, "Target SKU must be ACTIVE");
        }
        if (sourceSku.getId().equals(targetSku.getId())) {
            throw new ResponseStatusException(BAD_REQUEST, "Target SKU must differ from source SKU");
        }

        Map<Long, ReceiptLine> affectedLinesById = collectAffectedLines(receiptId, sourceSkuId, discrepancy);
        List<ReceiptLine> affectedLines = affectedLinesById.values().stream().toList();
        for (ReceiptLine line : affectedLines) {
            Long oldSkuId = line.getSkuId();
            line.setSkuId(targetSku.getId());
            line.setExcludedFromWorkflow(false);
            line.setExclusionReason(null);
            applySnapshots(line, targetSku);
            if (oldSkuId != null && !oldSkuId.equals(targetSku.getId())) {
                auditLogService.logUpdate(
                    "RECEIPT_LINE",
                    line.getId(),
                    resolveCurrentUsername(),
                    "skuId",
                    oldSkuId.toString(),
                    targetSku.getId().toString()
                );
            }
        }
        if (!affectedLines.isEmpty()) {
            receiptLineRepository.saveAll(affectedLines);
        }

        List<Pallet> pallets = collectAffectedPallets(receiptId, sourceSkuId, affectedLines);
        for (Pallet pallet : pallets) {
            Long oldSkuId = pallet.getSkuId();
            if (targetSku.getId().equals(oldSkuId)) {
                continue;
            }
            pallet.setSkuId(targetSku.getId());
            auditLogService.logUpdate(
                "PALLET",
                pallet.getId(),
                resolveCurrentUsername(),
                "skuId",
                oldSkuId != null ? oldSkuId.toString() : null,
                targetSku.getId().toString()
            );
        }
        if (!pallets.isEmpty()) {
            palletRepository.saveAll(pallets);
        }

        List<Discrepancy> relatedDiscrepancies = discrepancyRepository.findByReceipt_IdAndDraftSkuId(receiptId, sourceSkuId);
        for (Discrepancy item : relatedDiscrepancies) {
            item.setDraftSkuId(null);
            auditLogService.logUpdate(
                "DISCREPANCY",
                item.getId(),
                resolveCurrentUsername(),
                "draftSkuId",
                sourceSkuId.toString(),
                null
            );
        }
        if (!relatedDiscrepancies.isEmpty()) {
            discrepancyRepository.saveAll(relatedDiscrepancies);
        }

        return discrepancyRepository.findById(discrepancyId)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Discrepancy not found after remap: " + discrepancyId));
    }

    private Long resolveSourceSkuId(Discrepancy discrepancy) {
        if (discrepancy.getDraftSkuId() != null) {
            return discrepancy.getDraftSkuId();
        }
        if (discrepancy.getLine() == null || discrepancy.getLine().getSkuId() == null) {
            return null;
        }
        Sku lineSku = skuRepository.findById(discrepancy.getLine().getSkuId()).orElse(null);
        if (lineSku == null) {
            return null;
        }
        if (lineSku.getStatus() == SkuStatus.DRAFT || lineSku.getStatus() == SkuStatus.REJECTED) {
            return lineSku.getId();
        }
        return null;
    }

    private Map<Long, ReceiptLine> collectAffectedLines(Long receiptId, Long sourceSkuId, Discrepancy discrepancy) {
        Map<Long, ReceiptLine> lines = new LinkedHashMap<>();
        if (discrepancy.getLine() != null && discrepancy.getLine().getId() != null) {
            lines.put(discrepancy.getLine().getId(), discrepancy.getLine());
        }
        receiptLineRepository.findByReceipt_IdAndSkuId(receiptId, sourceSkuId)
            .forEach(line -> {
                if (line.getId() != null) {
                    lines.putIfAbsent(line.getId(), line);
                }
            });
        discrepancyRepository.findByReceipt_IdAndDraftSkuId(receiptId, sourceSkuId).stream()
            .map(Discrepancy::getLine)
            .filter(line -> line != null && line.getId() != null)
            .forEach(line -> lines.putIfAbsent(line.getId(), line));
        return lines;
    }

    private List<Pallet> collectAffectedPallets(Long receiptId, Long sourceSkuId, List<ReceiptLine> affectedLines) {
        Map<Long, Pallet> palletsById = new LinkedHashMap<>();

        palletRepository.findByReceipt_IdAndSkuId(receiptId, sourceSkuId)
            .forEach(pallet -> {
                if (pallet.getId() != null) {
                    palletsById.putIfAbsent(pallet.getId(), pallet);
                }
            });

        Set<Long> lineIds = new HashSet<>();
        for (ReceiptLine line : affectedLines) {
            if (line != null && line.getId() != null) {
                lineIds.add(line.getId());
            }
        }
        if (!lineIds.isEmpty()) {
            palletRepository.findByReceipt_IdAndReceiptLine_IdIn(receiptId, lineIds)
                .forEach(pallet -> {
                    if (pallet.getId() != null) {
                        palletsById.putIfAbsent(pallet.getId(), pallet);
                    }
                });
        }

        return new ArrayList<>(palletsById.values());
    }

    private void applySnapshots(ReceiptLine line, Sku targetSku) {
        if (line == null || targetSku == null || targetSku.getId() == null) {
            return;
        }
        String preferredUom = (line.getUom() != null && !line.getUom().isBlank()) ? line.getUom() : targetSku.getUom();
        SkuUnitConfig config;
        try {
            config = skuService.getActiveUnitConfigOrThrow(targetSku.getId(), preferredUom);
        } catch (ResponseStatusException ex) {
            config = skuService.getActiveUnitConfigOrThrow(targetSku.getId(), targetSku.getUom());
        }

        line.setUom(config.getUnitCode());
        line.setUnitFactorToBase(config.getFactorToBase().setScale(6, RoundingMode.HALF_UP));
        line.setUnitsPerPalletSnapshot(config.getUnitsPerPallet().setScale(3, RoundingMode.HALF_UP));
        BigDecimal qtyExpected = line.getQtyExpected() != null ? line.getQtyExpected() : BigDecimal.ZERO;
        line.setQtyExpectedBase(qtyExpected.multiply(line.getUnitFactorToBase()).setScale(3, RoundingMode.HALF_UP));
    }

    private String resolveCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()
            || "anonymousUser".equalsIgnoreCase(authentication.getName())) {
            return "system";
        }
        return authentication.getName();
    }
}
