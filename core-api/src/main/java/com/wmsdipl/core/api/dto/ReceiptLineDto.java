package com.wmsdipl.core.api.dto;

import java.math.BigDecimal;

public record ReceiptLineDto(Long id,
                             Integer lineNo,
                             Long skuId,
                             Long packagingId,
                             String uom,
                             BigDecimal qtyExpected,
                             String ssccExpected) {
}
