package com.wmsdipl.core.service;

import com.wmsdipl.contracts.dto.ReceivingAnalyticsDto;
import com.wmsdipl.contracts.dto.ReceivingHealthDto;
import com.wmsdipl.core.domain.Discrepancy;
import com.wmsdipl.core.domain.Pallet;
import com.wmsdipl.core.domain.PalletStatus;
import com.wmsdipl.core.domain.Receipt;
import com.wmsdipl.core.domain.ReceiptStatus;
import com.wmsdipl.core.domain.Task;
import com.wmsdipl.core.domain.TaskStatus;
import com.wmsdipl.core.domain.TaskType;
import com.wmsdipl.core.repository.DiscrepancyRepository;
import com.wmsdipl.core.repository.PalletRepository;
import com.wmsdipl.core.repository.ReceiptRepository;
import com.wmsdipl.core.repository.ScanRepository;
import com.wmsdipl.core.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service for calculating receiving workflow analytics.
 * Provides dashboard metrics for performance monitoring.
 */
@Service
public class AnalyticsService {
    private static final Set<String> DAMAGE_DISCREPANCY_TYPES = Set.of("DAMAGE", "EXPIRED_PRODUCT", "EXPIRED");

    private final ReceiptRepository receiptRepository;
    private final TaskRepository taskRepository;
    private final PalletRepository palletRepository;
    private final DiscrepancyRepository discrepancyRepository;
    private final ScanRepository scanRepository;
    private final CsvExportService csvExportService;

    public AnalyticsService(
            ReceiptRepository receiptRepository,
            TaskRepository taskRepository,
            PalletRepository palletRepository,
            DiscrepancyRepository discrepancyRepository,
            ScanRepository scanRepository,
            CsvExportService csvExportService
    ) {
        this.receiptRepository = receiptRepository;
        this.taskRepository = taskRepository;
        this.palletRepository = palletRepository;
        this.discrepancyRepository = discrepancyRepository;
        this.scanRepository = scanRepository;
        this.csvExportService = csvExportService;
    }

    /**
     * Calculates receiving analytics for a given time period.
     * Metrics include:
     * - Receipt counts by status
     * - Discrepancy counts by type and rate
     * - Pallet counts by status
     * - Damage rate (damage discrepancies share, with fallback to damaged pallets share)
     * - Average receiving time
     * 
     * @param startDate start of period (inclusive)
     * @param endDate end of period (inclusive)
     * @return analytics DTO with calculated metrics
     */
    @Transactional(readOnly = true)
    public ReceivingAnalyticsDto calculateAnalytics(LocalDateTime startDate, LocalDateTime endDate) {
        validatePeriod(startDate, endDate);

        List<Task> receivingTasks = taskRepository.findByTaskTypeAndClosedAtBetween(
            TaskType.RECEIVING,
            startDate,
            endDate
        );
        List<Task> placementTasks = taskRepository.findByTaskTypeAndClosedAtBetween(
            TaskType.PLACEMENT,
            startDate,
            endDate
        );

        List<Discrepancy> discrepancies = discrepancyRepository.findByCreatedAtBetween(startDate, endDate);
        Map<String, Integer> discrepanciesByType = countByKey(
            discrepancies,
            discrepancy -> discrepancy.getType() != null ? discrepancy.getType() : "UNKNOWN"
        );

        List<Pallet> pallets = palletRepository.findByCreatedAtBetweenAndReceiptIsNotNull(startDate, endDate);
        Map<String, Integer> palletsByStatus = countByKey(
            pallets,
            pallet -> pallet.getStatus() != null ? pallet.getStatus().name() : "UNKNOWN"
        );

        List<Receipt> receiptsCreatedInPeriod = receiptRepository.findByCreatedAtBetween(startDate, endDate);
        Set<Long> activeReceiptIds = collectActiveReceiptIds(
            receiptsCreatedInPeriod,
            discrepancies,
            pallets,
            receivingTasks,
            placementTasks
        );
        List<Receipt> activeReceipts = activeReceiptIds.isEmpty()
            ? List.of()
            : receiptRepository.findAllById(activeReceiptIds);
        Map<String, Integer> receiptsByStatus = countByKey(
            activeReceipts,
            receipt -> receipt.getStatus() != null ? receipt.getStatus().name() : "UNKNOWN"
        );

        long receiptsWithDiscrepancies = discrepancies.stream()
            .map(Discrepancy::getReceipt)
            .filter(Objects::nonNull)
            .map(Receipt::getId)
            .filter(Objects::nonNull)
            .filter(activeReceiptIds::contains)
            .distinct()
            .count();
        double discrepancyRate = activeReceipts.isEmpty()
            ? 0.0
            : ((double) receiptsWithDiscrepancies / activeReceipts.size()) * 100.0;

        double damagedPalletsRate = calculateDamageRate(activeReceipts, activeReceiptIds, discrepancies, pallets);
        double avgReceivingTimeHours = calculateAverageTaskTimeHours(receivingTasks);
        double avgPlacingTimeHours = calculateAverageTaskTimeHours(placementTasks);

        return new ReceivingAnalyticsDto(
            startDate.toLocalDate(),
            endDate.toLocalDate(),
            avgReceivingTimeHours,
            avgPlacingTimeHours,
            receiptsByStatus,
            discrepanciesByType,
            discrepancyRate,
            palletsByStatus,
            damagedPalletsRate
        );
    }

    @Transactional(readOnly = true)
    public byte[] exportAnalyticsCsv(LocalDateTime startDate, LocalDateTime endDate) {
        ReceivingAnalyticsDto analytics = calculateAnalytics(startDate, endDate);
        List<String> headers = List.of("metric", "value");
        List<List<String>> rows = new ArrayList<>();

        rows.add(List.of("fromDate", analytics.fromDate().toString()));
        rows.add(List.of("toDate", analytics.toDate().toString()));
        rows.add(List.of("avgReceivingTimeHours", String.format("%.2f", analytics.avgReceivingTimeHours())));
        rows.add(List.of("avgPlacingTimeHours", String.format("%.2f", analytics.avgPlacingTimeHours())));
        rows.add(List.of("discrepancyRate", String.format("%.2f", analytics.discrepancyRate())));
        rows.add(List.of("damagedPalletsRate", String.format("%.2f", analytics.damagedPalletsRate())));

        analytics.receiptsByStatus().entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> rows.add(List.of("receiptsByStatus." + entry.getKey(), String.valueOf(entry.getValue()))));

        analytics.discrepanciesByType().entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> rows.add(List.of("discrepanciesByType." + entry.getKey(), String.valueOf(entry.getValue()))));

        analytics.palletsByStatus().entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> rows.add(List.of("palletsByStatus." + entry.getKey(), String.valueOf(entry.getValue()))));

        return csvExportService.generateCsv(headers, rows);
    }

    @Transactional(readOnly = true)
    public ReceivingHealthDto calculateReceivingHealth(LocalDateTime startDate, LocalDateTime endDate, int thresholdHours) {
        validatePeriod(startDate, endDate);
        if (thresholdHours <= 0) {
            throw new IllegalArgumentException("thresholdHours must be greater than 0");
        }

        LocalDateTime staleThreshold = LocalDateTime.now().minusHours(thresholdHours);

        long stuckReceivingReceipts = receiptRepository
            .findByStatusAndUpdatedAtBefore(ReceiptStatus.IN_PROGRESS, staleThreshold)
            .stream()
            .filter(receipt -> isWithinPeriod(receipt.getCreatedAt(), startDate, endDate))
            .count();

        long stuckPlacingReceipts = receiptRepository
            .findByStatusAndUpdatedAtBefore(ReceiptStatus.PLACING, staleThreshold)
            .stream()
            .filter(receipt -> isWithinPeriod(receipt.getCreatedAt(), startDate, endDate))
            .count();

        long stuckReadyForShipmentReceipts = receiptRepository
            .findByStatusAndUpdatedAtBefore(ReceiptStatus.READY_FOR_SHIPMENT, staleThreshold)
            .stream()
            .filter(receipt -> isWithinPeriod(receipt.getCreatedAt(), startDate, endDate))
            .count();

        long stuckShippingInProgressReceipts = receiptRepository
            .findByStatusAndUpdatedAtBefore(ReceiptStatus.SHIPPING_IN_PROGRESS, staleThreshold)
            .stream()
            .filter(receipt -> isWithinPeriod(receipt.getCreatedAt(), startDate, endDate))
            .count();

        long staleTasks = taskRepository.findAll().stream()
            .filter(task -> task.getTaskType() == TaskType.RECEIVING
                || task.getTaskType() == TaskType.PLACEMENT
                || task.getTaskType() == TaskType.SHIPPING)
            .filter(task -> task.getStatus() == TaskStatus.ASSIGNED || task.getStatus() == TaskStatus.IN_PROGRESS)
            .filter(task -> {
                LocalDateTime activityReference = task.getStartedAt() != null ? task.getStartedAt() : task.getCreatedAt();
                return activityReference != null
                    && activityReference.isBefore(staleThreshold)
                    && isWithinPeriod(task.getCreatedAt(), startDate, endDate)
                    && !scanRepository.existsByTaskIdAndScannedAtAfter(task.getId(), staleThreshold);
            })
            .count();

        List<Discrepancy> periodDiscrepancies = discrepancyRepository.findByCreatedAtBetween(startDate, endDate);
        long autoResolvedDiscrepancies = periodDiscrepancies.stream()
            .filter(discrepancy -> Boolean.TRUE.equals(discrepancy.getResolved()))
            .count();
        long criticalDiscrepancies = periodDiscrepancies.stream()
            .filter(discrepancy -> discrepancy.getType() != null)
            .filter(discrepancy -> List.of(
                "UNDER_QTY",
                "OVER_QTY",
                "BARCODE_MISMATCH",
                "SSCC_MISMATCH"
            ).contains(discrepancy.getType()))
            .count();

        return new ReceivingHealthDto(
            startDate.toLocalDate(),
            endDate.toLocalDate(),
            thresholdHours,
            stuckReceivingReceipts,
            stuckPlacingReceipts,
            stuckReadyForShipmentReceipts,
            stuckShippingInProgressReceipts,
            staleTasks,
            autoResolvedDiscrepancies,
            criticalDiscrepancies
        );
    }

    private void validatePeriod(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Date range is required");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before or equal to end date");
        }
    }

    private Set<Long> collectActiveReceiptIds(
        List<Receipt> receiptsCreatedInPeriod,
        List<Discrepancy> discrepancies,
        List<Pallet> pallets,
        List<Task> receivingTasks,
        List<Task> placementTasks
    ) {
        Set<Long> ids = new HashSet<>();
        receiptsCreatedInPeriod.stream()
            .map(Receipt::getId)
            .filter(Objects::nonNull)
            .forEach(ids::add);

        discrepancies.stream()
            .map(Discrepancy::getReceipt)
            .filter(Objects::nonNull)
            .map(Receipt::getId)
            .filter(Objects::nonNull)
            .forEach(ids::add);

        pallets.stream()
            .map(Pallet::getReceipt)
            .filter(Objects::nonNull)
            .map(Receipt::getId)
            .filter(Objects::nonNull)
            .forEach(ids::add);

        Stream.concat(receivingTasks.stream(), placementTasks.stream())
            .map(Task::getReceipt)
            .filter(Objects::nonNull)
            .map(Receipt::getId)
            .filter(Objects::nonNull)
            .forEach(ids::add);

        return ids;
    }

    private double calculateAverageTaskTimeHours(List<Task> tasks) {
        if (tasks.isEmpty()) {
            return 0.0;
        }

        List<Double> durationsHours = tasks.stream()
            .filter(task -> task.getStatus() == TaskStatus.COMPLETED)
            .map(task -> {
                LocalDateTime startedAt = task.getStartedAt();
                LocalDateTime closedAt = task.getClosedAt();
                if (startedAt == null || closedAt == null || closedAt.isBefore(startedAt)) {
                    return null;
                }
                return Duration.between(startedAt, closedAt).toSeconds() / 3600.0;
            })
            .filter(Objects::nonNull)
            .toList();

        if (durationsHours.isEmpty()) {
            return 0.0;
        }

        return durationsHours.stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);
    }

    private double calculateDamageRate(
        List<Receipt> activeReceipts,
        Set<Long> activeReceiptIds,
        List<Discrepancy> discrepancies,
        List<Pallet> pallets
    ) {
        if (activeReceipts == null || activeReceipts.isEmpty()) {
            return 0.0;
        }

        Set<Long> damagedReceiptIds = discrepancies.stream()
            .filter(discrepancy -> discrepancy.getType() != null)
            .filter(discrepancy -> DAMAGE_DISCREPANCY_TYPES.contains(discrepancy.getType().trim().toUpperCase()))
            .map(Discrepancy::getReceipt)
            .filter(Objects::nonNull)
            .map(Receipt::getId)
            .filter(Objects::nonNull)
            .filter(activeReceiptIds::contains)
            .collect(Collectors.toSet());

        if (damagedReceiptIds.isEmpty()) {
            damagedReceiptIds = pallets.stream()
                .filter(pallet -> pallet.getStatus() == PalletStatus.DAMAGED)
                .map(Pallet::getReceipt)
                .filter(Objects::nonNull)
                .map(Receipt::getId)
                .filter(Objects::nonNull)
                .filter(activeReceiptIds::contains)
                .collect(Collectors.toSet());
        }

        return ((double) damagedReceiptIds.size() / activeReceipts.size()) * 100.0;
    }

    private <T> Map<String, Integer> countByKey(List<T> items, Function<T, String> keyExtractor) {
        return items.stream()
            .collect(Collectors.groupingBy(
                keyExtractor,
                Collectors.counting()
            ))
            .entrySet()
            .stream()
            .sorted(
                Comparator
                    .<Map.Entry<String, Long>>comparingLong(Map.Entry::getValue)
                    .reversed()
                    .thenComparing(Map.Entry::getKey)
            )
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().intValue(),
                (left, right) -> left,
                LinkedHashMap::new
            ));
    }

    private boolean isWithinPeriod(LocalDateTime value, LocalDateTime startDate, LocalDateTime endDate) {
        return value != null && !value.isBefore(startDate) && !value.isAfter(endDate);
    }
}
