package com.wmsdipl.imports.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ImportPayload(String messageId,
                            String docNo,
                            LocalDate docDate,
                            String supplier,
                            List<Line> lines) {
    public record Line(Integer lineNo,
                       String sku,
                       String name,
                       String uom,
                       BigDecimal qtyExpected,
                       String packaging,
                       String sscc) {}
}
