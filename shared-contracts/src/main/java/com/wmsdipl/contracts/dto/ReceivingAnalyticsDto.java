package com.wmsdipl.contracts.dto;

import java.time.LocalDate;
import java.util.Map;

/**
 * Analytics data for receiving performance.
 * Contains metrics for a specified date range.
 */
public record ReceivingAnalyticsDto(
    LocalDate fromDate,
    LocalDate toDate,
    
    // Receiving Performance Metrics
    Double avgReceivingTimeHours,           // Average time from start to completion
    Map<String, Integer> receiptsByStatus,  // Count by status: {DRAFT: 5, CONFIRMED: 10, ...}
    
    // Discrepancy Analytics
    Map<String, Integer> discrepanciesByType,  // Count by type: {UNDER_QTY: 15, DAMAGE: 8, ...}
    Double discrepancyRate,                     // % of receipts with discrepancies
    
    // Pallet Analytics
    Map<String, Integer> palletsByStatus,   // Count by status: {PLACED: 450, DAMAGED: 12, ...}
    Double damagedPalletsRate               // % of pallets that are damaged
) {
}
