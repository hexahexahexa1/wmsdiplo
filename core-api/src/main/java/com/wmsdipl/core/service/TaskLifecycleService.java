package com.wmsdipl.core.service;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
     * Assigns a task to a user.
     * 
     * @param id task ID
     * @param assignee username of the assignee
     * @param assignedBy username of the person assigning the task
     * @return updated task
     */
    @Transactional
    public Task assign(Long id, String assignee, String assignedBy) {
        Task task = getTask(id);
        task.setAssignee(assignee);
        task.setAssignedBy(assignedBy);
        task.setStatus(TaskStatus.ASSIGNED);
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
        
        // Validation: Cannot complete empty task
        BigDecimal qtyDone = task.getQtyDone() != null ? task.getQtyDone() : BigDecimal.ZERO;
        if (qtyDone.compareTo(BigDecimal.ZERO) <= 0) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST, 
                "Нельзя завершить задание: ничего не отсканировано");
        }

        BigDecimal qtyAssigned = task.getQtyAssigned() != null ? task.getQtyAssigned() : BigDecimal.ZERO;

        // Specific validation for PLACEMENT tasks: no discrepancies allowed
        if (task.getTaskType() == TaskType.PLACEMENT) {
            if (qtyDone.compareTo(qtyAssigned) != 0) {
                throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Нельзя завершить размещение при наличии расхождений. Ожидалось: " + qtyAssigned + ", Размещено: " + qtyDone);
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
                discrepancy.setType("UNDER_QTY");
                discrepancy.setQtyExpected(qtyAssigned);
                discrepancy.setQtyActual(qtyDone);
                
                // Auto-resolve: user confirmed completion despite shortage
                discrepancy.setResolved(true);
                discrepancy.setDescription("Shortage confirmed by operator during task completion. " +
                    "Expected: " + qtyAssigned + ", Received: " + qtyDone);
                
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
}
