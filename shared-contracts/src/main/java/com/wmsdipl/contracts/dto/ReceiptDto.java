package com.wmsdipl.contracts.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReceiptDto(Long id,
						 String docNo,
						 LocalDate docDate,
						 String supplier,
						 String status,
						 String messageId,
						 Boolean crossDock,
                         String outboundRef,
						 LocalDateTime createdAt) {
}
