package com.wmsdipl.core.web;

import com.wmsdipl.contracts.dto.DiscrepancyRetentionConfigDto;
import com.wmsdipl.contracts.dto.DiscrepancyDto;
import com.wmsdipl.contracts.dto.UpdateDiscrepancyRetentionRequest;
import com.wmsdipl.core.domain.Discrepancy;
import com.wmsdipl.core.mapper.DiscrepancyMapper;
import com.wmsdipl.core.service.DiscrepancyJournalConfigService;
import com.wmsdipl.core.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/api/discrepancies")
@Tag(name = "Discrepancies", description = "Discrepancy management for tracking and resolving warehouse exceptions")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
public class DiscrepancyController {

    private final TaskService taskService;
    private final DiscrepancyMapper discrepancyMapper;
    private final DiscrepancyJournalConfigService discrepancyJournalConfigService;

    public DiscrepancyController(
        TaskService taskService,
        DiscrepancyMapper discrepancyMapper,
        DiscrepancyJournalConfigService discrepancyJournalConfigService
    ) {
        this.taskService = taskService;
        this.discrepancyMapper = discrepancyMapper;
        this.discrepancyJournalConfigService = discrepancyJournalConfigService;
    }

    @GetMapping
    @Operation(summary = "Discrepancy journal", description = "Returns discrepancy journal with optional filters")
    public List<DiscrepancyDto> list(
        @RequestParam(required = false) Long receiptId,
        @RequestParam(required = false) String type,
        @RequestParam(required = false) String docNo,
        @RequestParam(required = false) Long skuId,
        @RequestParam(required = false) String operator,
        @RequestParam(required = false) Boolean resolved,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new ResponseStatusException(BAD_REQUEST, "fromDate must be before or equal to toDate");
        }
        int retentionDays = discrepancyJournalConfigService.getRetentionDays();
        LocalDateTime retentionFrom = LocalDate.now().minusDays(retentionDays).atStartOfDay();
        LocalDateTime from = fromDate != null ? fromDate.atStartOfDay() : retentionFrom;
        if (from.isBefore(retentionFrom)) {
            from = retentionFrom;
        }
        LocalDateTime to = toDate != null ? toDate.atTime(23, 59, 59) : null;

        List<Discrepancy> discrepancies = taskService.findDiscrepancyJournal(
            receiptId, from, to, type, docNo, skuId, operator, resolved
        );
        Map<Long, String> taskAssignees = taskService.findTaskAssigneesByDiscrepancies(discrepancies);

        return discrepancies.stream()
            .map(discrepancy -> discrepancyMapper.toDto(
                discrepancy,
                taskService.resolveDiscrepancyOperator(discrepancy, taskAssignees)
            ))
            .toList();
    }

    @PatchMapping("/{id}/comment")
    @Operation(summary = "Update discrepancy comment", description = "Updates comment in discrepancy journal entry")
    public DiscrepancyDto updateComment(@PathVariable Long id, @RequestBody UpdateCommentRequest req) {
        return discrepancyMapper.toDto(taskService.updateDiscrepancyComment(id, req.comment));
    }

    @GetMapping("/retention")
    @Operation(summary = "Get discrepancy journal retention", description = "Returns journal retention in days")
    public DiscrepancyRetentionConfigDto getRetentionConfig() {
        return new DiscrepancyRetentionConfigDto(discrepancyJournalConfigService.getRetentionDays());
    }

    @PutMapping("/retention")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update discrepancy journal retention", description = "Updates journal retention in days (ADMIN only)")
    public DiscrepancyRetentionConfigDto updateRetentionConfig(@RequestBody UpdateDiscrepancyRetentionRequest request) {
        if (request == null || request.retentionDays() == null || request.retentionDays() <= 0) {
            throw new ResponseStatusException(BAD_REQUEST, "retentionDays must be greater than 0");
        }
        int updated = discrepancyJournalConfigService.updateRetentionDays(request.retentionDays());
        return new DiscrepancyRetentionConfigDto(updated);
    }

    private static class UpdateCommentRequest {
        public String comment;
    }
}
