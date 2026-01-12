package com.wmsdipl.core.domain;

public enum PalletStatus {
    EMPTY,
    RECEIVING,
    RECEIVED,
    STORED,
    IN_TRANSIT,
    PLACED,
    PICKING,
    SHIPPED,
    DAMAGED,      // Товар с повреждением
    QUARANTINE    // Товар на карантине
}
