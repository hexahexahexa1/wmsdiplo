package com.wmsdipl.core.domain;

public enum ReceiptStatus {
    DRAFT,              // Импортирован, черновик
    CONFIRMED,          // Подтверждён к приёмке
    IN_PROGRESS,        // Приёмка выполняется
    PENDING_RESOLUTION, // Есть нерешённые расхождения
    ACCEPTED,           // Приёмка завершена
    PLACING,            // Размещение в процессе
    STOCKED,            // Размещён на складе (финал)
    CANCELLED           // Отменён
}
