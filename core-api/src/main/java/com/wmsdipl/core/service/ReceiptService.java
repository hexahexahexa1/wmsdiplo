package com.wmsdipl.core.service;

import com.wmsdipl.contracts.dto.CreateReceiptRequest;
import com.wmsdipl.contracts.dto.ImportPayload;
import com.wmsdipl.contracts.dto.ReceiptDto;
import com.wmsdipl.contracts.dto.ReceiptLineDto;
import com.wmsdipl.core.domain.Receipt;
import com.wmsdipl.core.domain.ReceiptLine;
import com.wmsdipl.core.domain.ReceiptStatus;
import com.wmsdipl.core.domain.Sku;
import com.wmsdipl.core.mapper.ReceiptMapper;
import com.wmsdipl.core.repository.ReceiptRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ReceiptService {

    private final ReceiptRepository receiptRepository;
    private final ReceiptMapper receiptMapper;
    private final SkuService skuService;

    public ReceiptService(ReceiptRepository receiptRepository, ReceiptMapper receiptMapper, 
                         SkuService skuService) {
        this.receiptRepository = receiptRepository;
        this.receiptMapper = receiptMapper;
        this.skuService = skuService;
    }

    @Transactional(readOnly = true)
    public List<ReceiptDto> list() {
        return receiptRepository.findAll().stream().map(receiptMapper::toDto).toList();
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
}

