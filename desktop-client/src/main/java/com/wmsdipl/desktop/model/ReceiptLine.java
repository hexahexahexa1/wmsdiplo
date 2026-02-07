package com.wmsdipl.desktop.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReceiptLine(Long id,
                          Integer lineNo,
                          Long skuId,
                          Long packagingId,
                          String uom,
                          BigDecimal qtyExpected,
                          BigDecimal qtyExpectedBase,
                          BigDecimal unitFactorToBase,
                          BigDecimal unitsPerPalletSnapshot,
                          String ssccExpected,
                          String lotNumberExpected,
                          LocalDate expiryDateExpected) {
}
