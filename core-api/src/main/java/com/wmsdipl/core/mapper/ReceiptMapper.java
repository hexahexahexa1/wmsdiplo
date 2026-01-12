package com.wmsdipl.core.mapper;

import com.wmsdipl.contracts.dto.ReceiptDto;
import com.wmsdipl.contracts.dto.ReceiptLineDto;
import com.wmsdipl.contracts.dto.CreateReceiptRequest;
import com.wmsdipl.core.domain.Receipt;
import com.wmsdipl.core.domain.ReceiptLine;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ReceiptMapper {

    public ReceiptDto toDto(Receipt receipt) {
        return new ReceiptDto(
            receipt.getId(),
            receipt.getDocNo(),
            receipt.getDocDate(),
            receipt.getSupplier(),
            receipt.getStatus().name(),
            receipt.getMessageId(),
            receipt.getCrossDock(),
            receipt.getCreatedAt()
        );
    }

    public ReceiptLineDto toLineDto(ReceiptLine line) {
        return new ReceiptLineDto(
            line.getId(),
            line.getLineNo(),
            line.getSkuId(),
            line.getPackagingId(),
            line.getUom(),
            line.getQtyExpected(),
            line.getSsccExpected(),
            line.getLotNumberExpected(),
            line.getExpiryDateExpected()
        );
    }

    public ReceiptLine toLine(CreateReceiptRequest.Line lineReq) {
        ReceiptLine line = new ReceiptLine();
        line.setLineNo(lineReq.lineNo());
        line.setSkuId(lineReq.skuId());
        line.setPackagingId(lineReq.packagingId());
        line.setUom(lineReq.uom());
        line.setQtyExpected(defaultZero(lineReq.qtyExpected()));
        line.setSsccExpected(lineReq.ssccExpected());
        line.setLotNumberExpected(lineReq.lotNumberExpected());
        line.setExpiryDateExpected(lineReq.expiryDateExpected());
        return line;
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
