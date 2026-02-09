package com.wmsdipl.core.service;

import com.wmsdipl.contracts.dto.CreateReceiptRequest;
import com.wmsdipl.contracts.dto.CreateReceiptDraftRequest;
import com.wmsdipl.contracts.dto.ImportPayload;
import com.wmsdipl.contracts.dto.ReceiptDiscrepancyDto;
import com.wmsdipl.contracts.dto.ReceiptDto;
import com.wmsdipl.contracts.dto.ReceiptLineDto;
import com.wmsdipl.contracts.dto.ReceiptSummaryDto;
import com.wmsdipl.contracts.dto.UpsertReceiptLineRequest;
import com.wmsdipl.core.domain.Receipt;
import com.wmsdipl.core.domain.ReceiptLine;
import com.wmsdipl.core.domain.ReceiptStatus;
import com.wmsdipl.core.domain.Sku;
import com.wmsdipl.core.domain.SkuUnitConfig;
import com.wmsdipl.core.domain.Pallet;
import com.wmsdipl.core.domain.Task;
import com.wmsdipl.core.domain.TaskStatus;
import com.wmsdipl.core.domain.TaskType;
import com.wmsdipl.core.mapper.ReceiptMapper;
import com.wmsdipl.core.repository.DiscrepancyRepository;
import com.wmsdipl.core.repository.ReceiptRepository;
import com.wmsdipl.core.repository.PalletRepository;
import com.wmsdipl.core.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ReceiptService {

    private static final Logger log = LoggerFactory.getLogger(ReceiptService.class);

    private final ReceiptRepository receiptRepository;
    private final ReceiptMapper receiptMapper;
    private final SkuService skuService;
    private final PalletRepository palletRepository;
    private final TaskRepository taskRepository;
    private final DiscrepancyRepository discrepancyRepository;

    public ReceiptService(ReceiptRepository receiptRepository, ReceiptMapper receiptMapper, 
                         SkuService skuService, PalletRepository palletRepository, TaskRepository taskRepository,
                         DiscrepancyRepository discrepancyRepository) {
        this.receiptRepository = receiptRepository;
        this.receiptMapper = receiptMapper;
        this.skuService = skuService;
        this.palletRepository = palletRepository;
        this.taskRepository = taskRepository;
        this.discrepancyRepository = discrepancyRepository;
    }

    @Transactional(readOnly = true)
    public List<ReceiptDto> list() {
        return receiptRepository.findAll().stream().map(receiptMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public Page<ReceiptDto> listFiltered(
            String status,
            String supplier,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable
    ) {
        Specification<Receipt> spec = Specification.where(null);
        if (status != null && !status.isBlank()) {
            ReceiptStatus parsedStatus = ReceiptStatus.valueOf(status.trim().toUpperCase());
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), parsedStatus));
        }
        if (supplier != null && !supplier.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.like(
                cb.lower(root.get("supplier")),
                "%" + supplier.toLowerCase() + "%"
            ));
        }
        if (fromDate != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("docDate"), fromDate));
        }
        if (toDate != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("docDate"), toDate));
        }
        return receiptRepository.findAll(spec, pageable).map(receiptMapper::toDto);
    }

    @Transactional(readOnly = true)
    public ReceiptDto get(Long id) {
        return receiptRepository.findById(id).map(receiptMapper::toDto)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Receipt not found"));
    }

    @Transactional(readOnly = true)
    public List<ReceiptLineDto> listLines(Long id) {
        Receipt receipt = receiptRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Receipt not found"));
        return receipt.getLines().stream().map(receiptMapper::toLineDto).toList();
    }

    @Transactional
    public ReceiptDto createManual(CreateReceiptRequest request) {
        validateManual(request);
        Receipt receipt = new Receipt();
        receipt.setDocNo(request.docNo().trim());
        receipt.setDocDate(request.docDate());
        receipt.setSupplier(normalizeNullableText(request.supplier()));
        receipt.setCrossDock(request.crossDock() != null ? request.crossDock() : false);
        receipt.setOutboundRef(normalizeNullableText(request.outboundRef()));
        receipt.setStatus(ReceiptStatus.DRAFT);
        request.lines().forEach(lineReq -> {
            ReceiptLine line = receiptMapper.toLine(lineReq);
            applyUnitSnapshots(line, lineReq.skuId(), lineReq.uom());
            receipt.addLine(line);
        });
        try {
            return receiptMapper.toDto(receiptRepository.save(receipt));
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(CONFLICT, "Duplicate document for supplier");
        }
    }

    @Transactional
    public ReceiptDto createDraft(CreateReceiptDraftRequest request) {
        validateDraftHeader(request.docNo(), request.supplier());

        Receipt receipt = new Receipt();
        receipt.setDocNo(request.docNo().trim());
        receipt.setDocDate(request.docDate());
        receipt.setSupplier(normalizeNullableText(request.supplier()));
        receipt.setCrossDock(request.crossDock() != null ? request.crossDock() : false);
        receipt.setOutboundRef(normalizeNullableText(request.outboundRef()));
        receipt.setStatus(ReceiptStatus.DRAFT);

        try {
            return receiptMapper.toDto(receiptRepository.save(receipt));
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(CONFLICT, "Duplicate document for supplier");
        }
    }

    @Transactional
    public ReceiptLineDto addDraftLine(Long receiptId, UpsertReceiptLineRequest request) {
        Receipt receipt = getDraftReceiptOrThrow(receiptId);
        validateDraftLineRequest(request);

        ReceiptLine line = new ReceiptLine();
        line.setLineNo(resolveLineNoForCreate(receipt, request.lineNo()));
        line.setSkuId(request.skuId());
        line.setPackagingId(request.packagingId());
        line.setUom(request.uom());
        line.setQtyExpected(defaultZero(request.qtyExpected()));
        line.setSsccExpected(normalizeNullableText(request.ssccExpected()));
        line.setLotNumberExpected(normalizeNullableText(request.lotNumberExpected()));
        line.setExpiryDateExpected(request.expiryDateExpected());
        applyUnitSnapshots(line, request.skuId(), request.uom());
        receipt.addLine(line);

        receiptRepository.save(receipt);
        return receiptMapper.toLineDto(line);
    }

    @Transactional
    public ReceiptLineDto updateDraftLine(Long receiptId, Long lineId, UpsertReceiptLineRequest request) {
        Receipt receipt = getDraftReceiptOrThrow(receiptId);
        validateDraftLineRequest(request);
        ReceiptLine line = findReceiptLineOrThrow(receipt, lineId);

        line.setLineNo(resolveLineNoForUpdate(request.lineNo(), line.getLineNo()));
        line.setSkuId(request.skuId());
        line.setPackagingId(request.packagingId());
        line.setUom(request.uom());
        line.setQtyExpected(defaultZero(request.qtyExpected()));
        line.setSsccExpected(normalizeNullableText(request.ssccExpected()));
        line.setLotNumberExpected(normalizeNullableText(request.lotNumberExpected()));
        line.setExpiryDateExpected(request.expiryDateExpected());
        applyUnitSnapshots(line, request.skuId(), request.uom());

        receiptRepository.save(receipt);
        return receiptMapper.toLineDto(line);
    }

    @Transactional
    public void deleteDraftLine(Long receiptId, Long lineId) {
        Receipt receipt = getDraftReceiptOrThrow(receiptId);
        ReceiptLine line = findReceiptLineOrThrow(receipt, lineId);
        receipt.getLines().remove(line);
        receiptRepository.save(receipt);
    }

    @Transactional
    public void deleteReceipt(Long id) {
        Receipt receipt = receiptRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Receipt not found"));

        if (receipt.getStatus() != ReceiptStatus.DRAFT) {
            throw new ResponseStatusException(BAD_REQUEST, "Only draft receipts can be deleted");
        }

        if (!taskRepository.findByReceiptId(id).isEmpty()) {
            throw new ResponseStatusException(CONFLICT, "Cannot delete receipt with tasks");
        }
        if (!palletRepository.findByReceipt(receipt).isEmpty()) {
            throw new ResponseStatusException(CONFLICT, "Cannot delete receipt with pallets");
        }
        if (!discrepancyRepository.findByReceipt(receipt).isEmpty()) {
            throw new ResponseStatusException(CONFLICT, "Cannot delete receipt with discrepancies");
        }

        receiptRepository.delete(receipt);
    }

    @Transactional
    public ReceiptDto createFromImport(ImportPayload payload) {
        if (payload.messageId() == null || payload.messageId().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "messageId is required for import");
        }
        Optional<Receipt> existing = receiptRepository.findByMessageId(payload.messageId());
        if (existing.isPresent()) {
            return receiptMapper.toDto(existing.get());
        }
        Receipt receipt = new Receipt();
        receipt.setDocNo(payload.docNo());
        receipt.setDocDate(payload.docDate());
        receipt.setSupplier(payload.supplier());
        receipt.setCrossDock(payload.crossDock() != null ? payload.crossDock() : false);
        receipt.setOutboundRef(payload.outboundRef());
        receipt.setStatus(ReceiptStatus.DRAFT);
        receipt.setMessageId(payload.messageId());
        payload.lines().forEach(line -> receipt.addLine(toLineFromImport(line)));
        try {
            return receiptMapper.toDto(receiptRepository.save(receipt));
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(CONFLICT, "Import duplicate constraint");
        }
    }

    @Transactional
    public void confirm(Long id) {
        Receipt receipt = receiptRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Receipt not found"));
        if (receipt.getStatus() != ReceiptStatus.DRAFT) {
            throw new ResponseStatusException(BAD_REQUEST, "Only draft can be confirmed");
        }
        ensureReceiptLinesReadyForWorkflow(receipt);
        receipt.setStatus(ReceiptStatus.CONFIRMED);
    }

    @Transactional
    public void accept(Long id) {
        Receipt receipt = receiptRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Receipt not found"));
        if (receipt.getStatus() != ReceiptStatus.CONFIRMED) {
            throw new ResponseStatusException(BAD_REQUEST, "Only confirmed can be accepted");
        }
        List<ReceiptAcceptBlockedException.TaskBlocker> blockers = taskRepository.findByReceiptId(id).stream()
            .filter(task -> task.getTaskType() == TaskType.RECEIVING
                || task.getTaskType() == TaskType.PLACEMENT
                || task.getTaskType() == TaskType.SHIPPING)
            .filter(task -> task.getStatus() != TaskStatus.COMPLETED && task.getStatus() != TaskStatus.CANCELLED)
            .map(task -> new ReceiptAcceptBlockedException.TaskBlocker(
                task.getId(),
                task.getTaskType() != null ? task.getTaskType().name() : null,
                task.getStatus() != null ? task.getStatus().name() : null
            ))
            .toList();
        if (!blockers.isEmpty()) {
            throw new ReceiptAcceptBlockedException(id, blockers);
        }

        long unresolvedDiscrepancies = discrepancyRepository.findByReceipt(receipt).stream()
            .filter(discrepancy -> !Boolean.TRUE.equals(discrepancy.getResolved()))
            .count();
        if (unresolvedDiscrepancies > 0) {
            log.warn(
                "Accepting receipt {} with {} unresolved discrepancies (soft control).",
                id,
                unresolvedDiscrepancies
            );
        }
        receipt.setStatus(ReceiptStatus.ACCEPTED);
    }

    private void validateManual(CreateReceiptRequest request) {
        validateDraftHeader(request.docNo(), request.supplier());
        if (request.lines() == null || request.lines().isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "At least one receipt line is required");
        }
        for (CreateReceiptRequest.Line line : request.lines()) {
            if (line.skuId() == null) {
                throw new ResponseStatusException(BAD_REQUEST, "skuId is required for manual receipt lines");
            }
            if (line.uom() == null || line.uom().isBlank()) {
                throw new ResponseStatusException(BAD_REQUEST, "uom is required for manual receipt lines");
            }
            if (line.qtyExpected() == null || line.qtyExpected().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(BAD_REQUEST, "qtyExpected must be greater than zero");
            }
        }
    }

    public void ensureReceiptLinesReadyForWorkflow(Receipt receipt) {
        if (receipt.getLines() == null || receipt.getLines().isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "At least one receipt line is required");
        }

        for (ReceiptLine line : receipt.getLines()) {
            if (line.getSkuId() == null) {
                throw new ResponseStatusException(BAD_REQUEST, "skuId is required for receipt lines");
            }
            if (line.getUom() == null || line.getUom().isBlank()) {
                throw new ResponseStatusException(BAD_REQUEST, "uom is required for receipt lines");
            }
            if (line.getQtyExpected() == null || line.getQtyExpected().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(BAD_REQUEST, "qtyExpected must be greater than zero");
            }
            applyUnitSnapshots(line, line.getSkuId(), line.getUom());
        }
    }

    private ReceiptLine toLineFromImport(ImportPayload.Line lineReq) {
        ReceiptLine line = new ReceiptLine();
        line.setLineNo(lineReq.lineNo());
        
        // Auto-create SKU if not exists to avoid rejecting receipts with new SKUs
        Long skuId = null;
        if (lineReq.sku() != null && !lineReq.sku().isBlank()) {
            Sku sku = skuService.findOrCreate(lineReq.sku(), lineReq.name(), lineReq.uom());
            skuId = sku.getId();
            line.setSkuId(skuId);
        } else {
            line.setSkuId(null);
        }
        
        line.setPackagingId(null);
        line.setUom(lineReq.uom());
        line.setQtyExpected(defaultZero(lineReq.qtyExpected()));
        line.setSsccExpected(normalizeNullableText(lineReq.sscc()));
        line.setLotNumberExpected(normalizeNullableText(lineReq.lotNumber()));
        line.setExpiryDateExpected(lineReq.expiryDate());
        applyUnitSnapshots(line, skuId, lineReq.uom());
        return line;
    }

    private void applyUnitSnapshots(ReceiptLine line, Long skuId, String lineUom) {
        if (skuId == null) {
            throw new ResponseStatusException(BAD_REQUEST, "skuId is required for receipt line");
        }
        if (lineUom == null || lineUom.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "uom is required for receipt line");
        }
        SkuUnitConfig unitConfig = skuService.getActiveUnitConfigOrThrow(skuId, lineUom);
        BigDecimal factor = unitConfig.getFactorToBase().setScale(6, java.math.RoundingMode.HALF_UP);
        BigDecimal unitsPerPallet = unitConfig.getUnitsPerPallet().setScale(3, java.math.RoundingMode.HALF_UP);
        BigDecimal qtyExpected = defaultZero(line.getQtyExpected());
        BigDecimal qtyExpectedBase = qtyExpected.multiply(factor).setScale(3, java.math.RoundingMode.HALF_UP);

        line.setUom(unitConfig.getUnitCode());
        line.setUnitFactorToBase(factor);
        line.setUnitsPerPalletSnapshot(unitsPerPallet);
        line.setQtyExpectedBase(qtyExpectedBase);
    }

    private Receipt getDraftReceiptOrThrow(Long receiptId) {
        Receipt receipt = receiptRepository.findById(receiptId)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Receipt not found"));
        if (receipt.getStatus() != ReceiptStatus.DRAFT) {
            throw new ResponseStatusException(BAD_REQUEST, "Only draft receipt lines can be edited");
        }
        return receipt;
    }

    private ReceiptLine findReceiptLineOrThrow(Receipt receipt, Long lineId) {
        return receipt.getLines().stream()
            .filter(line -> lineId.equals(line.getId()))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Receipt line not found"));
    }

    private void validateDraftHeader(String docNo, String supplier) {
        if (docNo == null || docNo.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "docNo is required");
        }
        if (receiptRepository.existsByDocNoAndSupplier(docNo.trim(), normalizeNullableText(supplier))) {
            throw new ResponseStatusException(CONFLICT, "Duplicate document for supplier");
        }
    }

    private void validateDraftLineRequest(UpsertReceiptLineRequest request) {
        if (request.skuId() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "skuId is required for manual receipt lines");
        }
        if (request.uom() == null || request.uom().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "uom is required for manual receipt lines");
        }
        if (request.qtyExpected() == null || request.qtyExpected().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(BAD_REQUEST, "qtyExpected must be greater than zero");
        }
    }

    private Integer resolveLineNoForCreate(Receipt receipt, Integer requestedLineNo) {
        if (requestedLineNo != null && requestedLineNo > 0) {
            return requestedLineNo;
        }
        return receipt.getLines().stream()
            .map(ReceiptLine::getLineNo)
            .filter(n -> n != null && n > 0)
            .max(Integer::compareTo)
            .orElse(0) + 1;
    }

    private Integer resolveLineNoForUpdate(Integer requestedLineNo, Integer currentLineNo) {
        if (requestedLineNo != null && requestedLineNo > 0) {
            return requestedLineNo;
        }
        if (currentLineNo != null && currentLineNo > 0) {
            return currentLineNo;
        }
        return 1;
    }

    private String normalizeNullableText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    @Transactional(readOnly = true)
    public ReceiptSummaryDto getSummary(Long id) {
        Receipt receipt = receiptRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Receipt not found"));
        
        List<Pallet> pallets = palletRepository.findByReceipt(receipt);
        
        // Group pallets by receipt line
        Map<Long, List<Pallet>> palletsByLine = pallets.stream()
            .filter(p -> p.getReceiptLine() != null)
            .collect(Collectors.groupingBy(p -> p.getReceiptLine().getId()));
        
        BigDecimal totalQtyExpected = BigDecimal.ZERO;
        BigDecimal totalQtyReceived = BigDecimal.ZERO;
        boolean hasDiscrepancies = false;
        
        List<ReceiptSummaryDto.LineSummary> linesSummary = new ArrayList<>();
        
        for (ReceiptLine line : receipt.getLines()) {
            BigDecimal qtyExpected = defaultZero(line.getQtyExpected());
            
            List<Pallet> linePallets = palletsByLine.getOrDefault(line.getId(), List.of());
            BigDecimal qtyReceived = linePallets.stream()
                .map(Pallet::getQuantity)
                .map(this::defaultZero)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            boolean hasDiscrepancy = qtyExpected.compareTo(qtyReceived) != 0;
            if (hasDiscrepancy) {
                hasDiscrepancies = true;
            }
            
            totalQtyExpected = totalQtyExpected.add(qtyExpected);
            totalQtyReceived = totalQtyReceived.add(qtyReceived);
            
            linesSummary.add(new ReceiptSummaryDto.LineSummary(
                line.getId(),
                line.getLineNo(),
                line.getSkuId(),
                line.getUom(),
                qtyExpected,
                qtyReceived,
                linePallets.size(),
                hasDiscrepancy
            ));
        }
        
        return new ReceiptSummaryDto(
            receipt.getId(),
            receipt.getDocNo(),
            receipt.getSupplier(),
            receipt.getStatus().name(),
            receipt.getLines().size(),
            pallets.size(),
            totalQtyExpected,
            totalQtyReceived,
            hasDiscrepancies,
            linesSummary
        );
    }

    @Transactional(readOnly = true)
    public ReceiptDiscrepancyDto getDiscrepancies(Long id) {
        Receipt receipt = receiptRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Receipt not found"));
        
        List<Pallet> pallets = palletRepository.findByReceipt(receipt);
        
        Map<Long, List<Pallet>> palletsByLine = pallets.stream()
            .filter(p -> p.getReceiptLine() != null)
            .collect(Collectors.groupingBy(p -> p.getReceiptLine().getId()));
        
        boolean hasDiscrepancies = false;
        List<ReceiptDiscrepancyDto.LineDiscrepancy> lineDiscrepancies = new ArrayList<>();
        
        for (ReceiptLine line : receipt.getLines()) {
            BigDecimal qtyExpected = defaultZero(line.getQtyExpected());
            
            List<Pallet> linePallets = palletsByLine.getOrDefault(line.getId(), List.of());
            BigDecimal qtyReceived = linePallets.stream()
                .map(Pallet::getQuantity)
                .map(this::defaultZero)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal difference = qtyReceived.subtract(qtyExpected);
            String discrepancyType;
            String severity;
            
            if (difference.compareTo(BigDecimal.ZERO) > 0) {
                discrepancyType = "OVER";
                severity = "WARNING";
                hasDiscrepancies = true;
            } else if (difference.compareTo(BigDecimal.ZERO) < 0) {
                discrepancyType = "UNDER";
                severity = "CRITICAL";
                hasDiscrepancies = true;
            } else {
                discrepancyType = "MATCH";
                severity = "INFO";
            }
            
            lineDiscrepancies.add(new ReceiptDiscrepancyDto.LineDiscrepancy(
                line.getId(),
                line.getLineNo(),
                line.getSkuId(),
                line.getUom(),
                qtyExpected,
                qtyReceived,
                difference,
                discrepancyType,
                severity
            ));
        }
        
        return new ReceiptDiscrepancyDto(
            receipt.getId(),
            receipt.getDocNo(),
            hasDiscrepancies,
            lineDiscrepancies
        );
    }
}
