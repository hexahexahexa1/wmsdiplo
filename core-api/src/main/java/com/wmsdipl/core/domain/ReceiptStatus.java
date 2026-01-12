package com.wmsdipl.core.domain;

public enum ReceiptStatus {
    DRAFT,              // Импортирован, черновик
    CONFIRMED,          // Подтверждён к приёмке
    IN_PROGRESS,        // Приёмка выполняется
    ACCEPTED,           // Приёмка завершена (оператор принял решение по всем расхождениям)
    READY_FOR_SHIPMENT, // Готов к отгрузке (cross-dock)
    PLACING,            // Размещение в процессе
    STOCKED,            // Размещён на складе (финал)
    CANCELLED           // Отменён
}
