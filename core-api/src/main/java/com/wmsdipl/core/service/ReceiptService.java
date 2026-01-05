package com.wmsdipl.core.service;

import com.wmsdipl.core.api.dto.CreateReceiptRequest;
import com.wmsdipl.core.api.dto.ImportPayload;
import com.wmsdipl.core.api.dto.ReceiptDto;
import com.wmsdipl.core.api.dto.ReceiptLineDto;
import com.wmsdipl.core.domain.Receipt;
import com.wmsdipl.core.domain.ReceiptLine;
import com.wmsdipl.core.domain.ReceiptStatus;
import com.wmsdipl.core.repository.ReceiptRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ReceiptService {

    private final ReceiptRepository receiptRepository;

    public ReceiptService(ReceiptRepository receiptRepository) {
        this.receiptRepository = receiptRepository;
    }

    @Transactional(readOnly = true)
    public List<ReceiptDto> list() {
        return receiptRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public ReceiptDto get(Long id) {
        return receiptRepository.findById(id).map(this::toDto)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Receipt not found"));
    }

    @Transactional(readOnly = true)
    public List<ReceiptLineDto> listLines(Long id) {
        Receipt receipt = receiptRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Receipt not found"));
        return receipt.getLines().stream().map(this::toLineDto).toList();
    }

    @Transactional
    public ReceiptDto createManual(CreateReceiptRequest request) {
        validateManual(request);
        Receipt receipt = new Receipt();
        receipt.setDocNo(request.docNo());
        receipt.setDocDate(request.docDate());
        receipt.setSupplier(request.supplier());
        receipt.setStatus(ReceiptStatus.DRAFT);
        request.lines().forEach(lineReq -> receipt.addLine(toLine(lineReq)));
        try {
            return toDto(receiptRepository.save(receipt));
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
            return toDto(existing.get());
        }
        Receipt receipt = new Receipt();
        receipt.setDocNo(payload.docNo());
        receipt.setDocDate(payload.docDate());
        receipt.setSupplier(payload.supplier());
        receipt.setStatus(ReceiptStatus.DRAFT);
        receipt.setMessageId(payload.messageId());
        payload.lines().forEach(line -> receipt.addLine(toLine(line)));
        try {
            return toDto(receiptRepository.save(receipt));
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

    private ReceiptLine toLine(CreateReceiptRequest.Line lineReq) {
        ReceiptLine line = new ReceiptLine();
        line.setLineNo(lineReq.lineNo());
        line.setSkuId(lineReq.skuId());
        line.setPackagingId(lineReq.packagingId());
        line.setUom(lineReq.uom());
        line.setQtyExpected(defaultZero(lineReq.qtyExpected()));
        line.setSsccExpected(lineReq.ssccExpected());
        return line;
    }

    private ReceiptLine toLine(ImportPayload.Line lineReq) {
        ReceiptLine line = new ReceiptLine();
        line.setLineNo(lineReq.lineNo());
        line.setSkuId(null); // импорт по коду; связывание с каталогом позже
        line.setPackagingId(null);
        line.setUom(lineReq.uom());
        line.setQtyExpected(defaultZero(lineReq.qtyExpected()));
        line.setSsccExpected(lineReq.sscc());
        return line;
    }

    private ReceiptDto toDto(Receipt receipt) {
        return new ReceiptDto(
            receipt.getId(),
            receipt.getDocNo(),
            receipt.getDocDate(),
            receipt.getSupplier(),
            receipt.getStatus().name(),
            receipt.getMessageId(),
            receipt.getCreatedAt()
        );
    }

    private ReceiptLineDto toLineDto(ReceiptLine line) {
        return new ReceiptLineDto(
            line.getId(),
            line.getLineNo(),
            line.getSkuId(),
            line.getPackagingId(),
            line.getUom(),
            line.getQtyExpected(),
            line.getSsccExpected()
        );
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
