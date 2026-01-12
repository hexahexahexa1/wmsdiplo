package com.wmsdipl.desktop.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record Receipt(Long id,
                      String docNo,
                      LocalDate docDate,
                      String supplier,
                      String status,
                      String messageId,
                      LocalDateTime createdAt,
                      Boolean crossDock) {
    @Override
    public String toString() {
        return docNo + " | " + status + (supplier != null ? " | " + supplier : "");
    }
}
