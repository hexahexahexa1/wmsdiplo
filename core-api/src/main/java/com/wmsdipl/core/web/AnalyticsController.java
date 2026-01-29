package com.wmsdipl.core.web;

import com.wmsdipl.contracts.dto.ReceivingAnalyticsDto;
import com.wmsdipl.core.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * REST controller for receiving analytics and performance metrics.
 */
@RestController
@RequestMapping("/api/analytics")
@Tag(name = "Analytics", description = "Analytics and performance metrics for receiving operations")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'PC_OPERATOR')")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /**
     * Get receiving analytics for a date range.
     * 
     * GET /api/analytics/receiving?fromDate=2026-01-01&toDate=2026-01-31
     * 
     * Returns:
     * - Receipt counts by status
     * - Discrepancy counts by type and rate
     * - Pallet counts by status
     * - Damaged pallets rate
     * - Average receiving time in hours
     */
    @GetMapping("/receiving")
    @Operation(
        summary = "Get receiving analytics", 
        description = "Calculate performance metrics for receiving operations within a date range"
    )
    public ResponseEntity<ReceivingAnalyticsDto> getReceivingAnalytics(
        @Parameter(description = "Start date (inclusive)", example = "2026-01-01")
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
        
        @Parameter(description = "End date (inclusive)", example = "2026-01-31")
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        LocalDateTime startDateTime = fromDate.atStartOfDay();
        LocalDateTime endDateTime = toDate.atTime(23, 59, 59);
        
        ReceivingAnalyticsDto analytics = analyticsService.calculateAnalytics(startDateTime, endDateTime);
        return ResponseEntity.ok(analytics);
    }

    /**
     * Get receiving analytics for today.
     * 
     * GET /api/analytics/receiving/today
     */
    @GetMapping("/receiving/today")
    @Operation(summary = "Get today's receiving analytics", description = "Calculate metrics for today")
    public ResponseEntity<ReceivingAnalyticsDto> getTodayAnalytics() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(23, 59, 59);
        
        ReceivingAnalyticsDto analytics = analyticsService.calculateAnalytics(startOfDay, endOfDay);
        return ResponseEntity.ok(analytics);
    }

    /**
     * Get receiving analytics for current week.
     * 
     * GET /api/analytics/receiving/week
     */
    @GetMapping("/receiving/week")
    @Operation(summary = "Get current week's receiving analytics", description = "Calculate metrics for current week (Monday-Sunday)")
    public ResponseEntity<ReceivingAnalyticsDto> getWeekAnalytics() {
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.minusDays(today.getDayOfWeek().getValue() - 1);
        LocalDate endOfWeek = startOfWeek.plusDays(6);
        
        LocalDateTime startDateTime = startOfWeek.atStartOfDay();
        LocalDateTime endDateTime = endOfWeek.atTime(23, 59, 59);
        
        ReceivingAnalyticsDto analytics = analyticsService.calculateAnalytics(startDateTime, endDateTime);
        return ResponseEntity.ok(analytics);
    }

    /**
     * Get receiving analytics for current month.
     * 
     * GET /api/analytics/receiving/month
     */
    @GetMapping("/receiving/month")
    @Operation(summary = "Get current month's receiving analytics", description = "Calculate metrics for current month")
    public ResponseEntity<ReceivingAnalyticsDto> getMonthAnalytics() {
        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);
        LocalDate endOfMonth = today.withDayOfMonth(today.lengthOfMonth());
        
        LocalDateTime startDateTime = startOfMonth.atStartOfDay();
        LocalDateTime endDateTime = endOfMonth.atTime(23, 59, 59);
        
        ReceivingAnalyticsDto analytics = analyticsService.calculateAnalytics(startDateTime, endDateTime);
        return ResponseEntity.ok(analytics);
    }
}
