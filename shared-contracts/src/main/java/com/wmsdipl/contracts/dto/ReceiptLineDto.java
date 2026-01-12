package com.wmsdipl.contracts.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReceiptLineDto(Long id,
                             Integer lineNo,
                             Long skuId,
                             Long packagingId,
                             String uom,
                             BigDecimal qtyExpected,
                             String ssccExpected,
                             String lotNumberExpected,
                             LocalDate expiryDateExpected) {
}
