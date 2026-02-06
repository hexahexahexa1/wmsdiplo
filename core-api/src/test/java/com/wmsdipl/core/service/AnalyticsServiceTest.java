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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private ReceiptRepository receiptRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private PalletRepository palletRepository;

    @Mock
    private DiscrepancyRepository discrepancyRepository;

    @Mock
    private ScanRepository scanRepository;

    @Mock
    private CsvExportService csvExportService;

    @InjectMocks
    private AnalyticsService analyticsService;

    @Test
    void shouldCalculateMetricsAccurately_WhenDataExists() {
        LocalDateTime from = LocalDateTime.of(2026, 2, 1, 0, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 2, 7, 23, 59, 59);

        Receipt receipt1 = new Receipt();
        receipt1.setId(1L);
        receipt1.setStatus(ReceiptStatus.ACCEPTED);
        receipt1.setCreatedAt(from.plusHours(1));

        Receipt receipt2 = new Receipt();
        receipt2.setId(2L);
        receipt2.setStatus(ReceiptStatus.IN_PROGRESS);
        receipt2.setCreatedAt(from.plusHours(2));

        Discrepancy discrepancy1 = new Discrepancy();
        discrepancy1.setReceipt(receipt1);
        discrepancy1.setType("DAMAGE");
        discrepancy1.setCreatedAt(from.plusHours(3));

        Discrepancy discrepancy2 = new Discrepancy();
        discrepancy2.setReceipt(receipt1);
        discrepancy2.setType("UNDER_QTY");
        discrepancy2.setCreatedAt(from.plusHours(4));

        Pallet pallet1 = new Pallet();
        pallet1.setStatus(PalletStatus.DAMAGED);
        pallet1.setCreatedAt(from.plusHours(2));
        pallet1.setReceipt(receipt1);

        Pallet pallet2 = new Pallet();
        pallet2.setStatus(PalletStatus.RECEIVED);
        pallet2.setCreatedAt(from.plusHours(5));
        pallet2.setReceipt(receipt2);

        Task task1 = new Task();
        task1.setReceipt(receipt1);
        task1.setTaskType(TaskType.RECEIVING);
        task1.setStatus(TaskStatus.COMPLETED);
        task1.setStartedAt(receipt1.getCreatedAt().plusHours(1));
        task1.setClosedAt(receipt1.getCreatedAt().plusHours(3));

        Task task2 = new Task();
        task2.setReceipt(receipt2);
        task2.setTaskType(TaskType.RECEIVING);
        task2.setStatus(TaskStatus.COMPLETED);
        task2.setStartedAt(receipt2.getCreatedAt().plusHours(1));
        task2.setClosedAt(receipt2.getCreatedAt().plusHours(2));

        Task placementTask = new Task();
        placementTask.setReceipt(receipt1);
        placementTask.setTaskType(TaskType.PLACEMENT);
        placementTask.setStatus(TaskStatus.COMPLETED);
        placementTask.setStartedAt(receipt1.getCreatedAt().plusHours(3));
        placementTask.setClosedAt(receipt1.getCreatedAt().plusHours(4));

        when(receiptRepository.findByCreatedAtBetween(from, to)).thenReturn(List.of(receipt1, receipt2));
        when(receiptRepository.findAllById(any())).thenReturn(List.of(receipt1, receipt2));
        when(discrepancyRepository.findByCreatedAtBetween(from, to)).thenReturn(List.of(discrepancy1, discrepancy2));
        when(palletRepository.findByCreatedAtBetweenAndReceiptIsNotNull(from, to)).thenReturn(List.of(pallet1, pallet2));
        when(taskRepository.findByTaskTypeAndClosedAtBetween(TaskType.RECEIVING, from, to)).thenReturn(List.of(task1, task2));
        when(taskRepository.findByTaskTypeAndClosedAtBetween(TaskType.PLACEMENT, from, to)).thenReturn(List.of(placementTask));

        ReceivingAnalyticsDto result = analyticsService.calculateAnalytics(from, to);

        assertEquals(2, result.receiptsByStatus().values().stream().mapToInt(Integer::intValue).sum());
        assertEquals(1, result.receiptsByStatus().get("ACCEPTED"));
        assertEquals(1, result.receiptsByStatus().get("IN_PROGRESS"));
        assertEquals(1, result.discrepanciesByType().get("DAMAGE"));
        assertEquals(1, result.discrepanciesByType().get("UNDER_QTY"));
        assertEquals(50.0, result.discrepancyRate(), 0.0001);
        assertEquals(50.0, result.damagedPalletsRate(), 0.0001);
        assertEquals(1.5, result.avgReceivingTimeHours(), 0.0001);
        assertEquals(1.0, result.avgPlacingTimeHours(), 0.0001);
    }

    @Test
    void shouldReturnNonZeroDiscrepancyRate_WhenReceiptCreatedOutsidePeriod() {
        LocalDateTime from = LocalDateTime.of(2026, 2, 1, 0, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 2, 7, 23, 59, 59);

        Receipt oldReceipt = new Receipt();
        oldReceipt.setId(10L);
        oldReceipt.setStatus(ReceiptStatus.PLACING);
        oldReceipt.setCreatedAt(from.minusMonths(1));

        Discrepancy discrepancy = new Discrepancy();
        discrepancy.setReceipt(oldReceipt);
        discrepancy.setType("UNDER_QTY");
        discrepancy.setCreatedAt(from.plusDays(1));

        when(receiptRepository.findByCreatedAtBetween(from, to)).thenReturn(List.of());
        when(receiptRepository.findAllById(any())).thenReturn(List.of(oldReceipt));
        when(discrepancyRepository.findByCreatedAtBetween(from, to)).thenReturn(List.of(discrepancy));
        when(palletRepository.findByCreatedAtBetweenAndReceiptIsNotNull(from, to)).thenReturn(List.of());
        when(taskRepository.findByTaskTypeAndClosedAtBetween(TaskType.RECEIVING, from, to)).thenReturn(List.of());
        when(taskRepository.findByTaskTypeAndClosedAtBetween(TaskType.PLACEMENT, from, to)).thenReturn(List.of());

        ReceivingAnalyticsDto result = analyticsService.calculateAnalytics(from, to);

        assertEquals(100.0, result.discrepancyRate(), 0.0001);
        assertEquals(0.0, result.damagedPalletsRate(), 0.0001);
        assertEquals(1, result.receiptsByStatus().get("PLACING"));
    }

    @Test
    void shouldExportCsv_WhenRequested() {
        LocalDateTime from = LocalDateTime.of(2026, 2, 1, 0, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 2, 1, 23, 59, 59);

        Receipt receipt = new Receipt();
        receipt.setId(1L);
        receipt.setStatus(ReceiptStatus.ACCEPTED);
        receipt.setCreatedAt(from.plusHours(1));

        Task task = new Task();
        task.setReceipt(receipt);
        task.setTaskType(TaskType.RECEIVING);
        task.setStatus(TaskStatus.COMPLETED);
        task.setStartedAt(receipt.getCreatedAt().plusHours(1));
        task.setClosedAt(receipt.getCreatedAt().plusHours(2));

        when(receiptRepository.findByCreatedAtBetween(from, to)).thenReturn(List.of(receipt));
        when(receiptRepository.findAllById(any())).thenReturn(List.of(receipt));
        when(discrepancyRepository.findByCreatedAtBetween(from, to)).thenReturn(List.of());
        when(palletRepository.findByCreatedAtBetweenAndReceiptIsNotNull(from, to)).thenReturn(List.of());
        when(taskRepository.findByTaskTypeAndClosedAtBetween(TaskType.RECEIVING, from, to)).thenReturn(List.of(task));
        when(taskRepository.findByTaskTypeAndClosedAtBetween(TaskType.PLACEMENT, from, to)).thenReturn(List.of());
        when(csvExportService.generateCsv(anyList(), anyList())).thenReturn(new byte[] {1, 2, 3});

        byte[] exported = analyticsService.exportAnalyticsCsv(from, to);

        assertEquals(3, exported.length);
        ArgumentCaptor<List<String>> headersCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<List<String>>> rowsCaptor = ArgumentCaptor.forClass(List.class);
        verify(csvExportService).generateCsv(headersCaptor.capture(), rowsCaptor.capture());
        assertEquals(List.of("metric", "value"), headersCaptor.getValue());
        assertEquals(true, rowsCaptor.getValue().stream().anyMatch(row -> row.get(0).equals("fromDate")));
        assertEquals(true, rowsCaptor.getValue().stream().anyMatch(row -> row.get(0).equals("toDate")));
        assertEquals(true, rowsCaptor.getValue().stream().anyMatch(row -> row.get(0).equals("avgPlacingTimeHours")));
    }

    @Test
    void shouldThrowException_WhenDateRangeInvalid() {
        LocalDateTime from = LocalDateTime.of(2026, 2, 8, 0, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 2, 1, 23, 59, 59);

        assertThrows(IllegalArgumentException.class, () -> analyticsService.calculateAnalytics(from, to));
    }

    @Test
    void shouldCalculateReceivingHealth_WhenDataExists() {
        LocalDateTime from = LocalDateTime.now().minusDays(7);
        LocalDateTime to = LocalDateTime.now();

        Receipt stuckReceiving = new Receipt();
        stuckReceiving.setStatus(ReceiptStatus.IN_PROGRESS);
        stuckReceiving.setCreatedAt(LocalDateTime.now().minusDays(2));
        stuckReceiving.setUpdatedAt(LocalDateTime.now().minusHours(10));

        Receipt stuckPlacing = new Receipt();
        stuckPlacing.setStatus(ReceiptStatus.PLACING);
        stuckPlacing.setCreatedAt(LocalDateTime.now().minusDays(1));
        stuckPlacing.setUpdatedAt(LocalDateTime.now().minusHours(9));

        Task staleTask = new Task();
        staleTask.setId(1L);
        staleTask.setTaskType(TaskType.RECEIVING);
        staleTask.setStatus(TaskStatus.IN_PROGRESS);
        staleTask.setCreatedAt(LocalDateTime.now().minusDays(1));
        staleTask.setStartedAt(LocalDateTime.now().minusHours(8));

        Discrepancy resolved = new Discrepancy();
        resolved.setResolved(true);
        resolved.setType("UNDER_QTY");
        resolved.setCreatedAt(LocalDateTime.now().minusDays(1));

        when(receiptRepository.findByStatusAndUpdatedAtBefore(eq(ReceiptStatus.IN_PROGRESS), any()))
            .thenReturn(List.of(stuckReceiving));
        when(receiptRepository.findByStatusAndUpdatedAtBefore(eq(ReceiptStatus.PLACING), any()))
            .thenReturn(List.of(stuckPlacing));
        when(taskRepository.findAll()).thenReturn(List.of(staleTask));
        when(scanRepository.existsByTaskIdAndScannedAtAfter(eq(1L), any())).thenReturn(false);
        when(discrepancyRepository.findByCreatedAtBetween(any(), any())).thenReturn(List.of(resolved));

        ReceivingHealthDto result = analyticsService.calculateReceivingHealth(from, to, 4);

        assertEquals(1L, result.stuckReceivingReceipts());
        assertEquals(1L, result.stuckPlacingReceipts());
        assertEquals(1L, result.staleTasks());
        assertEquals(1L, result.autoResolvedDiscrepancies());
        assertEquals(1L, result.criticalDiscrepancies());
    }
}
