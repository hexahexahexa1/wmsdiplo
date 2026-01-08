package com.wmsdipl.desktop.model;

import java.math.BigDecimal;

public record Pallet(Long id,
                     String code,
                     String status,
                     Location location,
                     Long skuId,
                     BigDecimal quantity,
                     ReceiptRef receipt) {
}
