package com.wmsdipl.desktop.model;

import java.math.BigDecimal;

public record ReceiptLine(Long id,
                          Integer lineNo,
                          Long skuId,
                          Long packagingId,
                          String uom,
                          BigDecimal qtyExpected,
                          String ssccExpected) {
}
