package com.wmsdipl.core.service;

import com.wmsdipl.contracts.dto.CreateReceiptRequest;
import com.wmsdipl.contracts.dto.ImportPayload;
import com.wmsdipl.contracts.dto.ReceiptDiscrepancyDto;
import com.wmsdipl.contracts.dto.ReceiptDto;
import com.wmsdipl.contracts.dto.ReceiptLineDto;
import com.wmsdipl.contracts.dto.ReceiptSummaryDto;
import com.wmsdipl.core.domain.Receipt;
import com.wmsdipl.core.domain.ReceiptLine;
import com.wmsdipl.core.domain.ReceiptStatus;
import com.wmsdipl.core.domain.Sku;
import com.wmsdipl.core.domain.Pallet;
import com.wmsdipl.core.mapper.ReceiptMapper;
import com.wmsdipl.core.repository.ReceiptRepository;
import com.wmsdipl.core.repository.PalletRepository;
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

    private final ReceiptRepository receiptRepository;
    private final ReceiptMapper receiptMapper;
    private final SkuService skuService;
    private final PalletRepository palletRepository;

    public ReceiptService(ReceiptRepository receiptRepository, ReceiptMapper receiptMapper, 
                         SkuService skuService, PalletRepository palletRepository) {
        this.receiptRepository = receiptRepository;
        this.receiptMapper = receiptMapper;
        this.skuService = skuService;
        this.palletRepository = palletRepository;
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
        receipt.setDocNo(request.docNo());
        receipt.setDocDate(request.docDate());
        receipt.setSupplier(request.supplier());
        receipt.setCrossDock(request.crossDock() != null ? request.crossDock() : false);
        receipt.setStatus(ReceiptStatus.DRAFT);
        request.lines().forEach(lineReq -> receipt.addLine(receiptMapper.toLine(lineReq)));
        try {
            return receiptMapper.toDto(receiptRepository.save(receipt));
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(CONFLICT, "Duplicate document for supplier");
        }
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
        receipt.setStatus(ReceiptStatus.CONFIRMED);
    }

    @Transactional
    public void accept(Long id) {
        Receipt receipt = receiptRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Receipt not found"));
        if (receipt.getStatus() != ReceiptStatus.CONFIRMED) {
            throw new ResponseStatusException(BAD_REQUEST, "Only confirmed can be accepted");
        }
        // TODO: validate tasks closed and discrepancies resolved
        receipt.setStatus(ReceiptStatus.ACCEPTED);
    }

    private void validateManual(CreateReceiptRequest request) {
        if (request.docNo() == null || request.docNo().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "docNo is required");
        }
        if (request.lines() == null || request.lines().isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "At least one receipt line is required");
        }
        for (CreateReceiptRequest.Line line : request.lines()) {
            if (line.skuId() == null) {
                throw new ResponseStatusException(BAD_REQUEST, "skuId is required for manual receipt lines");
            }
            if (line.qtyExpected() == null || line.qtyExpected().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(BAD_REQUEST, "qtyExpected must be greater than zero");
            }
        }
        if (receiptRepository.existsByDocNoAndSupplier(request.docNo(), request.supplier())) {
            throw new ResponseStatusException(CONFLICT, "Duplicate document for supplier");
        }
    }

    private ReceiptLine toLineFromImport(ImportPayload.Line lineReq) {
        ReceiptLine line = new ReceiptLine();
        line.setLineNo(lineReq.lineNo());
        
        // Auto-create SKU if not exists to avoid rejecting receipts with new SKUs
        if (lineReq.sku() != null && !lineReq.sku().isBlank()) {
            Sku sku = skuService.findOrCreate(lineReq.sku(), lineReq.name(), lineReq.uom());
            line.setSkuId(sku.getId());
        } else {
            line.setSkuId(null);
        }
        
        line.setPackagingId(null);
        line.setUom(lineReq.uom());
        line.setQtyExpected(defaultZero(lineReq.qtyExpected()));
        line.setSsccExpected(lineReq.sscc());
        return line;
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
