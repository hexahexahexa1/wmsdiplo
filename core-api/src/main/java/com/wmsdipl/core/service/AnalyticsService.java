package com.wmsdipl.core.service;

import com.wmsdipl.contracts.dto.ReceivingAnalyticsDto;
import com.wmsdipl.core.domain.Pallet;
import com.wmsdipl.core.domain.PalletStatus;
import com.wmsdipl.core.domain.Receipt;
import com.wmsdipl.core.domain.ReceiptStatus;
import com.wmsdipl.core.domain.TaskStatus;
import com.wmsdipl.core.domain.TaskType;
import com.wmsdipl.core.repository.DiscrepancyRepository;
import com.wmsdipl.core.repository.PalletRepository;
import com.wmsdipl.core.repository.ReceiptRepository;
import com.wmsdipl.core.repository.ScanRepository;
import com.wmsdipl.core.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for calculating receiving workflow analytics.
 * Provides dashboard metrics for performance monitoring.
 */
@Service
public class AnalyticsService {

    private final ReceiptRepository receiptRepository;
    private final TaskRepository taskRepository;
    private final PalletRepository palletRepository;
    private final ScanRepository scanRepository;
    private final DiscrepancyRepository discrepancyRepository;

    public AnalyticsService(
            ReceiptRepository receiptRepository,
            TaskRepository taskRepository,
            PalletRepository palletRepository,
            ScanRepository scanRepository,
            DiscrepancyRepository discrepancyRepository
    ) {
        this.receiptRepository = receiptRepository;
        this.taskRepository = taskRepository;
        this.palletRepository = palletRepository;
        this.scanRepository = scanRepository;
        this.discrepancyRepository = discrepancyRepository;
    }

    /**
     * Calculates receiving analytics for a given time period.
     * Metrics include:
     * - Receipt counts by status
     * - Discrepancy counts by type and rate
     * - Pallet counts by status and damaged rate
     * - Average receiving time
     * 
     * @param startDate start of period (inclusive)
     * @param endDate end of period (inclusive)
     * @return analytics DTO with calculated metrics
     */
    @Transactional(readOnly = true)
    public ReceivingAnalyticsDto calculateAnalytics(LocalDateTime startDate, LocalDateTime endDate) {
        // Receipt metrics by status
        List<Receipt> allReceipts = receiptRepository.findAll().stream()
            .filter(r -> isInPeriod(r.getCreatedAt(), startDate, endDate))
            .toList();
        
        java.util.Map<String, Integer> receiptsByStatus = new java.util.HashMap<>();
        for (Receipt r : allReceipts) {
            String status = r.getStatus().name();
            receiptsByStatus.put(status, receiptsByStatus.getOrDefault(status, 0) + 1);
        }

        // Discrepancy metrics by type
        List<com.wmsdipl.core.domain.Discrepancy> allDiscrepancies = discrepancyRepository.findAll().stream()
            .filter(d -> isInPeriod(d.getCreatedAt(), startDate, endDate))
            .toList();
        
        java.util.Map<String, Integer> discrepanciesByType = new java.util.HashMap<>();
        for (com.wmsdipl.core.domain.Discrepancy d : allDiscrepancies) {
            String type = d.getType() != null ? d.getType() : "UNKNOWN";
            discrepanciesByType.put(type, discrepanciesByType.getOrDefault(type, 0) + 1);
        }
        
        // Discrepancy rate
        long receiptsWithDiscrepancies = allReceipts.stream()
            .filter(r -> !discrepancyRepository.findByReceipt(r).isEmpty())
            .count();
        double discrepancyRate = allReceipts.isEmpty() ? 0.0 : 
            (double) receiptsWithDiscrepancies / allReceipts.size() * 100.0;

        // Pallet metrics by status
        List<Pallet> allPallets = palletRepository.findAll().stream()
            .filter(p -> p.getReceipt() != null && isInPeriod(p.getReceipt().getCreatedAt(), startDate, endDate))
            .toList();
        
        java.util.Map<String, Integer> palletsByStatus = new java.util.HashMap<>();
        for (Pallet p : allPallets) {
            String status = p.getStatus().name();
            palletsByStatus.put(status, palletsByStatus.getOrDefault(status, 0) + 1);
        }
        
        // Damaged pallets rate
        long damagedPalletsCount = allPallets.stream()
            .filter(p -> p.getStatus() == PalletStatus.DAMAGED)
            .count();
        double damagedPalletsRate = allPallets.isEmpty() ? 0.0 : 
            (double) damagedPalletsCount / allPallets.size() * 100.0;

        // Average receiving time in hours
        double avgReceivingTimeHours = calculateAverageReceivingTimeHours(startDate, endDate);

        return new ReceivingAnalyticsDto(
            startDate.toLocalDate(),
            endDate.toLocalDate(),
            avgReceivingTimeHours,
            receiptsByStatus,
            discrepanciesByType,
            discrepancyRate,
            palletsByStatus,
            damagedPalletsRate
        );
    }

    /**
     * Checks if a timestamp falls within the given period.
     */
    private boolean isInPeriod(LocalDateTime timestamp, LocalDateTime start, LocalDateTime end) {
        if (timestamp == null) {
            return false;
        }
        return !timestamp.isBefore(start) && !timestamp.isAfter(end);
    }

    /**
     * Calculates average receiving time (from DRAFT to ACCEPTED) in hours.
     */
    private double calculateAverageReceivingTimeHours(LocalDateTime startDate, LocalDateTime endDate) {
        List<Receipt> completedReceipts = receiptRepository.findAll().stream()
            .filter(r -> r.getStatus() == ReceiptStatus.ACCEPTED || r.getStatus() == ReceiptStatus.PLACING || r.getStatus() == ReceiptStatus.STOCKED)
            .filter(r -> isInPeriod(r.getCreatedAt(), startDate, endDate))
            .toList();

        if (completedReceipts.isEmpty()) {
            return 0.0;
        }

        long totalHours = completedReceipts.stream()
            .filter(r -> r.getCreatedAt() != null && r.getUpdatedAt() != null)
            .mapToLong(r -> {
                return java.time.Duration.between(r.getCreatedAt(), r.getUpdatedAt()).toHours();
            })
            .sum();

        return (double) totalHours / completedReceipts.size();
    }
}
