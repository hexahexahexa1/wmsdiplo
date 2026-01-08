package com.wmsdipl.contracts.dto;

import java.math.BigDecimal;

public record ReceiptLineDto(Long id,
                             Integer lineNo,
                             Long skuId,
                             Long packagingId,
                             String uom,
                             BigDecimal qtyExpected,
                             String ssccExpected) {
}
