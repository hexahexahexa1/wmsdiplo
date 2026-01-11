package com.wmsdipl.contracts.dto;

import java.math.BigDecimal;

/**
 * Запрос на обновление расхождения при приемке.
 */
public record UpdateDiscrepancyRequest(
    String type,
    BigDecimal qtyExpected,
    BigDecimal qtyActual,
    String comment,
    Boolean resolved
) {
}
