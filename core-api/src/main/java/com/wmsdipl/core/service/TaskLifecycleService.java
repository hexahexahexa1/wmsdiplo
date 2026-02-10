package com.wmsdipl.core.service;

import com.wmsdipl.contracts.dto.DiscrepancyType;
import com.wmsdipl.core.domain.Discrepancy;
import com.wmsdipl.core.domain.Receipt;
import com.wmsdipl.core.domain.ReceiptLine;
import com.wmsdipl.core.domain.Scan;
import com.wmsdipl.core.domain.Task;
import com.wmsdipl.core.domain.TaskStatus;
import com.wmsdipl.core.domain.TaskType;
import com.wmsdipl.core.repository.DiscrepancyRepository;
import com.wmsdipl.core.repository.ScanRepository;
import com.wmsdipl.core.repository.TaskRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for managing task lifecycle transitions and state changes.
 * Handles common task operations like assigning, starting, completing, and canceling.
 */
@Service
public class TaskLifecycleService {

    private final TaskRepository taskRepository;
    private final ScanRepository scanRepository;
    private final DiscrepancyRepository discrepancyRepository;

    public TaskLifecycleService(
            TaskRepository taskRepository,
            ScanRepository scanRepository,
            DiscrepancyRepository discrepancyRepository
    ) {
        this.taskRepository = taskRepository;
        this.scanRepository = scanRepository;
        this.discrepancyRepository = discrepancyRepository;
    }

    /**
     * Retrieves a task by ID.
     *
     * @param id task ID
     * @return the task
     * @throws IllegalArgumentException if task not found
     */
    @Transactional(readOnly = true)
    public Task getTask(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + id));
    }

    /**
     * Assigns or reassigns a task to a user.
     * NEW task -> ASSIGNED.
     * ASSIGNED task -> assignee/assignedBy updated, status remains ASSIGNED.
     *
     * @param id task ID
     * @param assignee username of the assignee
     * @param assignedBy username of the person assigning the task
     * @return updated task
     */
    @Transactional
    public Task assign(Long id, String assignee, String assignedBy) {
        Task task = getTask(id);
        if (task.getStatus() == TaskStatus.IN_PROGRESS) {
            throw new IllegalStateException("Task in IN_PROGRESS status cannot be reassigned");
        }
        if (task.getStatus() == TaskStatus.COMPLETED) {
            throw new IllegalStateException("Completed task cannot be reassigned");
        }
        if (task.getStatus() == TaskStatus.CANCELLED) {
            throw new IllegalStateException("Cancelled task cannot be assigned");
        }
        if (task.getStatus() != TaskStatus.NEW && task.getStatus() != TaskStatus.ASSIGNED) {
            throw new IllegalStateException(
                "Task can only be assigned from NEW or ASSIGNED status. Current status: " + task.getStatus()
            );
        }
        task.setAssignee(assignee);
        task.setAssignedBy(assignedBy);
        if (task.getStatus() == TaskStatus.NEW) {
            task.setStatus(TaskStatus.ASSIGNED);
        }
        return taskRepository.save(task);
    }

    /**
     * Starts a task, transitioning it to IN_PROGRESS status.
     *
     * @param id task ID
     * @return updated task
     */
    @Transactional
    public Task start(Long id) {
        Task task = getTask(id);
        if (task.getStatus() != TaskStatus.NEW && task.getStatus() != TaskStatus.ASSIGNED) {
            throw new IllegalStateException(
                "Task can only be started from NEW or ASSIGNED status. Current status: " + task.getStatus()
            );
        }
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setStartedAt(LocalDateTime.now());
        return taskRepository.save(task);
    }

    /**
     * Completes a task, transitioning it to COMPLETED status.
     *
     * Detects UNDER_QTY discrepancy if qtyDone < qtyAssigned:
     * - Creates Discrepancy record (auto-resolved, since user confirmed completion)
     * - Marks last scan with discrepancy flag
     *
     * The UNDER_QTY discrepancy is automatically marked as resolved because:
     * - User explicitly clicked "Complete" button after seeing the shortage
     * - Desktop client shows confirmation dialog before allowing completion
     * - This means the operator has visually confirmed and accepted the discrepancy
     *
     * @param id task ID
     * @return updated task
     */
    @Transactional
    public Task complete(Long id) {
        Task task = getTask(id);
        if (task.getStatus() != TaskStatus.IN_PROGRESS) {
            throw new IllegalStateException(
                "Task can only be completed from IN_PROGRESS status. Current status: " + task.getStatus()
            );
        }

        BigDecimal qtyDone = task.getQtyDone() != null ? task.getQtyDone() : BigDecimal.ZERO;
        if (qtyDone.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot complete task: no scans recorded");
        }

        BigDecimal qtyAssigned = task.getQtyAssigned() != null ? task.getQtyAssigned() : BigDecimal.ZERO;

        // PLACEMENT and SHIPPING require full quantity completion.
        if (task.getTaskType() == TaskType.PLACEMENT || task.getTaskType() == TaskType.SHIPPING) {
            if (qtyDone.compareTo(qtyAssigned) != 0) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot complete task with incomplete quantity. Expected: " + qtyAssigned + ", Done: " + qtyDone
                );
            }
        }

        // Check for UNDER_QTY discrepancy (for RECEIVING tasks)
        if (task.getTaskType() == TaskType.RECEIVING && qtyAssigned.compareTo(BigDecimal.ZERO) > 0 && qtyDone.compareTo(qtyAssigned) < 0) {
            // Create UNDER_QTY discrepancy record (auto-resolved)
            Receipt receipt = task.getReceipt();
            ReceiptLine line = task.getLine();

            if (receipt != null && line != null) {
                Discrepancy discrepancy = new Discrepancy();
                discrepancy.setReceipt(receipt);
                discrepancy.setLine(line);
                discrepancy.setTaskId(task.getId());
                discrepancy.setType(DiscrepancyType.UNDER_QTY.name());
                discrepancy.setQtyExpected(qtyAssigned);
                discrepancy.setQtyActual(qtyDone);

                // Auto-resolve: user confirmed completion despite shortage
                discrepancy.setResolved(true);
                discrepancy.setResolvedBy(resolveCurrentUsername(task));
                discrepancy.setResolvedAt(LocalDateTime.now());
                discrepancy.setDescription(null);
                discrepancy.setSystemCommentKey("discrepancy.journal.comment.system.under_qty_confirmed");
                discrepancy.setSystemCommentParams(joinParams(
                    formatDecimal(qtyAssigned),
                    formatDecimal(qtyDone)
                ));

                discrepancyRepository.save(discrepancy);

                // Mark last scan with discrepancy flag for UI visibility
                List<Scan> scans = scanRepository.findByTask(task);
                if (!scans.isEmpty()) {
                    Scan lastScan = scans.get(scans.size() - 1);
                    lastScan.setDiscrepancy(true);
                    scanRepository.save(lastScan);
                }
            }
        }

        task.setStatus(TaskStatus.COMPLETED);
        task.setClosedAt(LocalDateTime.now());
        return taskRepository.save(task);
    }

    /**
     * Cancels a task, transitioning it to CANCELLED status.
     *
     * @param id task ID
     * @return updated task
     */
    @Transactional
    public Task cancel(Long id) {
        Task task = getTask(id);
        if (task.getStatus() == TaskStatus.COMPLETED) {
            throw new IllegalStateException("Completed task cannot be cancelled");
        }
        task.setStatus(TaskStatus.CANCELLED);
        task.setClosedAt(LocalDateTime.now());
        return taskRepository.save(task);
    }

    /**
     * Automatically starts a task if it's in NEW or ASSIGNED status.
     * Used during scan recording to auto-transition tasks.
     *
     * @param task the task to potentially start
     */
    @Transactional
    public void autoStartIfNeeded(Task task) {
        if (task.getStatus() == TaskStatus.NEW || task.getStatus() == TaskStatus.ASSIGNED) {
            task.setStatus(TaskStatus.IN_PROGRESS);
            task.setStartedAt(LocalDateTime.now());
            taskRepository.save(task);
        }
    }

    private String resolveCurrentUsername(Task task) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
            && authentication.getName() != null
            && !authentication.getName().isBlank()
            && !"anonymousUser".equalsIgnoreCase(authentication.getName())) {
            return authentication.getName();
        }
        if (task != null && task.getAssignee() != null && !task.getAssignee().isBlank()) {
            return task.getAssignee();
        }
        return "system";
    }

    private String joinParams(String... values) {
        if (values == null || values.length == 0) {
            return null;
        }
        return java.util.Arrays.stream(values)
            .map(v -> v == null ? "" : v)
            .collect(java.util.stream.Collectors.joining("|"));
    }

    private String formatDecimal(BigDecimal value) {
        if (value == null) {
            return "";
        }
        return value.stripTrailingZeros().toPlainString();
    }
}
