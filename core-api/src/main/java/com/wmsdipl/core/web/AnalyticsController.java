package com.wmsdipl.core.web;

import com.wmsdipl.contracts.dto.ReceivingAnalyticsDto;
import com.wmsdipl.contracts.dto.ReceivingHealthDto;
import com.wmsdipl.core.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * REST controller for receiving analytics and performance metrics.
 */
@RestController
@RequestMapping("/api/analytics")
@Tag(name = "Analytics", description = "Analytics and performance metrics for receiving operations")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
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
        validateDateRange(fromDate, toDate);

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

    @GetMapping("/receiving-health")
    @Operation(summary = "Get receiving health metrics", description = "Returns counts of stuck receipts/tasks and discrepancy severity counters for operations control")
    public ResponseEntity<ReceivingHealthDto> getReceivingHealth(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false, defaultValue = "4") Integer thresholdHours
    ) {
        validateDateRange(fromDate, toDate);
        if (thresholdHours == null || thresholdHours <= 0) {
            throw new ResponseStatusException(BAD_REQUEST, "thresholdHours must be greater than 0");
        }

        LocalDateTime startDateTime = fromDate.atStartOfDay();
        LocalDateTime endDateTime = toDate.atTime(23, 59, 59);
        ReceivingHealthDto health = analyticsService.calculateReceivingHealth(startDateTime, endDateTime, thresholdHours);
        return ResponseEntity.ok(health);
    }

    /**
     * Export receiving analytics for a date range as CSV.
     *
     * GET /api/analytics/export-csv?fromDate=2026-01-01&toDate=2026-01-31
     */
    @GetMapping("/export-csv")
    @Operation(summary = "Export receiving analytics to CSV", description = "Download analytics metrics for a date range as CSV")
    public ResponseEntity<byte[]> exportAnalyticsCsv(
        @Parameter(description = "Start date (inclusive)", example = "2026-01-01")
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,

        @Parameter(description = "End date (inclusive)", example = "2026-01-31")
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        validateDateRange(fromDate, toDate);

        LocalDateTime startDateTime = fromDate.atStartOfDay();
        LocalDateTime endDateTime = toDate.atTime(23, 59, 59);
        byte[] csv = analyticsService.exportAnalyticsCsv(startDateTime, endDateTime);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData(
            "attachment",
            "receiving-analytics-" + fromDate.format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-" + toDate.format(DateTimeFormatter.BASIC_ISO_DATE) + ".csv"
        );

        return ResponseEntity.ok()
            .headers(headers)
            .body(csv);
    }

    private void validateDateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate.isAfter(toDate)) {
            throw new ResponseStatusException(BAD_REQUEST, "fromDate must be before or equal to toDate");
        }
    }
}
