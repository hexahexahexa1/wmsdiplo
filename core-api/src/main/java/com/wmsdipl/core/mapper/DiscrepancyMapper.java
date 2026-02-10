package com.wmsdipl.core.mapper;

import com.wmsdipl.contracts.dto.CreateDiscrepancyRequest;
import com.wmsdipl.contracts.dto.DiscrepancyDto;
import com.wmsdipl.contracts.dto.UpdateDiscrepancyRequest;
import com.wmsdipl.core.domain.Discrepancy;
import com.wmsdipl.core.domain.Receipt;
import com.wmsdipl.core.domain.ReceiptLine;
import org.springframework.stereotype.Component;

/**
 * Mapper для преобразования между Discrepancy entity и DTOs.
 */
@Component
public class DiscrepancyMapper {
    
    /**
     * Преобразует Discrepancy entity в DiscrepancyDto.
     *
     * @param discrepancy entity для преобразования
     * @return DTO с данными расхождения
     */
    public DiscrepancyDto toDto(Discrepancy discrepancy) {
        return toDto(discrepancy, null);
    }

    public DiscrepancyDto toDto(Discrepancy discrepancy, String operator) {
        if (discrepancy == null) {
            return null;
        }
        
        return new DiscrepancyDto(
            discrepancy.getId(),
            discrepancy.getReceipt() != null ? discrepancy.getReceipt().getId() : null,
            discrepancy.getReceipt() != null ? discrepancy.getReceipt().getDocNo() : null,
            discrepancy.getLine() != null ? discrepancy.getLine().getId() : null,
            discrepancy.getLine() != null ? discrepancy.getLine().getLineNo() : null,
            discrepancy.getTaskId(),
            discrepancy.getPalletId(),
            discrepancy.getLine() != null ? discrepancy.getLine().getSkuId() : null,
            operator,
            discrepancy.getType(),
            discrepancy.getQtyExpected(),
            discrepancy.getQtyActual(),
            discrepancy.getDescription(),
            discrepancy.getSystemCommentKey(),
            discrepancy.getSystemCommentParams(),
            discrepancy.getDraftSkuId(),
            discrepancy.getResolved(),
            discrepancy.getResolvedBy(),
            discrepancy.getResolvedAt(),
            discrepancy.getCreatedAt()
        );
    }
    
    /**
     * Создает новую Discrepancy entity из CreateDiscrepancyRequest.
     *
     * @param request запрос на создание
     * @param receipt приемка, к которой относится расхождение
     * @param line строка приемки (опционально)
     * @return новая Discrepancy entity
     */
    public Discrepancy toEntity(CreateDiscrepancyRequest request, Receipt receipt, ReceiptLine line) {
        if (request == null) {
            return null;
        }
        
        Discrepancy discrepancy = new Discrepancy();
        discrepancy.setReceipt(receipt);
        discrepancy.setLine(line);
        discrepancy.setType(request.type());
        discrepancy.setQtyExpected(request.qtyExpected());
        discrepancy.setQtyActual(request.qtyActual());
        discrepancy.setDescription(request.comment());
        
        return discrepancy;
    }
    
    /**
     * Обновляет существующую Discrepancy entity данными из UpdateDiscrepancyRequest.
     *
     * @param discrepancy существующая entity для обновления
     * @param request запрос с новыми данными
     */
    public void updateEntity(Discrepancy discrepancy, UpdateDiscrepancyRequest request) {
        if (discrepancy == null || request == null) {
            return;
        }
        
        if (request.type() != null) {
            discrepancy.setType(request.type());
        }
        
        if (request.qtyExpected() != null) {
            discrepancy.setQtyExpected(request.qtyExpected());
        }
        
        if (request.qtyActual() != null) {
            discrepancy.setQtyActual(request.qtyActual());
        }
        
        if (request.comment() != null) {
            discrepancy.setDescription(request.comment());
        }
        
        if (request.resolved() != null) {
            discrepancy.setResolved(request.resolved());
        }
    }
}
