package com.wmsdipl.contracts.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record CreateReceiptDraftRequest(
    @NotBlank String docNo,
    LocalDate docDate,
    String supplier,
    Boolean crossDock,
    String outboundRef
) {
}
