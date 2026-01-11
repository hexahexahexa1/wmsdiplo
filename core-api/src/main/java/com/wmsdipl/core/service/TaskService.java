package com.wmsdipl.core.service;

import com.wmsdipl.core.domain.Discrepancy;
import com.wmsdipl.core.domain.Receipt;
import com.wmsdipl.core.domain.ReceiptLine;
import com.wmsdipl.core.domain.Scan;
import com.wmsdipl.core.domain.Task;
import com.wmsdipl.core.domain.TaskStatus;
import com.wmsdipl.core.domain.TaskType;
import com.wmsdipl.core.repository.DiscrepancyRepository;
import com.wmsdipl.core.repository.ReceiptRepository;
import com.wmsdipl.core.repository.ScanRepository;
import com.wmsdipl.core.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * Service for task management and querying.
 * Delegates lifecycle operations to TaskLifecycleService.
 */
@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final ReceiptRepository receiptRepository;
    private final DiscrepancyRepository discrepancyRepository;
    private final ScanRepository scanRepository;
    private final TaskLifecycleService taskLifecycleService;

    public TaskService(
            TaskRepository taskRepository,
            ReceiptRepository receiptRepository,
            DiscrepancyRepository discrepancyRepository,
            ScanRepository scanRepository,
            TaskLifecycleService taskLifecycleService
    ) {
        this.taskRepository = taskRepository;
        this.receiptRepository = receiptRepository;
        this.discrepancyRepository = discrepancyRepository;
        this.scanRepository = scanRepository;
        this.taskLifecycleService = taskLifecycleService;
    }

    @Transactional(readOnly = true)
    public List<Task> findAll() {
        return taskRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Task> findByReceipt(Long receiptId) {
        return taskRepository.findByReceiptId(receiptId);
    }

    @Transactional(readOnly = true)
    public Task get(Long id) {
        return taskLifecycleService.getTask(id);
    }

    @Transactional
    public Task create(Task task) {
        if (task.getReceipt() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Receipt is required");
        }
        return taskRepository.save(task);
    }

    @Transactional
    public Task assign(Long id, String assignee, String assignedBy) {
        return taskLifecycleService.assign(id, assignee, assignedBy);
    }

    @Transactional
    public Task start(Long id) {
        return taskLifecycleService.start(id);
    }

    @Transactional
    public Task complete(Long id) {
        return taskLifecycleService.complete(id);
    }

    @Transactional
    public Task cancel(Long id) {
        return taskLifecycleService.cancel(id);
    }

    /**
     * Releases a task back to NEW status.
     * Operator can release an assigned/in-progress task back to the pool.
     * The work already done (qtyDone, scans) is preserved.
     * 
     * @param id task ID
     * @return released task
     */
    @Transactional
    public Task release(Long id) {
        Task task = taskLifecycleService.getTask(id);
        
        if (task.getStatus() != TaskStatus.ASSIGNED 
            && task.getStatus() != TaskStatus.IN_PROGRESS) {
            throw new IllegalStateException(
                "Only ASSIGNED or IN_PROGRESS tasks can be released. Current status: " + task.getStatus());
        }
        
        // Reset to initial state
        task.setStatus(TaskStatus.NEW);
        task.setAssignee(null);
        task.setStartedAt(null);
        // qtyDone is NOT reset - work already done is preserved
        
        return taskRepository.save(task);
    }

    @Transactional(readOnly = true)
    public List<Discrepancy> findOpenDiscrepancies() {
        return discrepancyRepository.findByResolvedFalse();
    }

    @Transactional
    public Discrepancy resolveDiscrepancy(Long id, String comment) {
        Discrepancy discrepancy = discrepancyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Discrepancy not found: " + id));
        discrepancy.setResolved(true);
        discrepancy.setComment(comment);
        return discrepancyRepository.save(discrepancy);
    }

    /**
     * Checks if a task has any scans with discrepancies.
     * 
     * @param taskId task ID
     * @return true if at least one scan has discrepancy flag set to true
     */
    @Transactional(readOnly = true)
    public boolean hasDiscrepancies(Long taskId) {
        Task task = taskLifecycleService.getTask(taskId);
        List<Scan> scans = scanRepository.findByTask(task);
        return scans.stream().anyMatch(scan -> 
            scan.getDiscrepancy() != null && scan.getDiscrepancy()
        );
    }

    /**
     * Generates multiple receiving tasks for a receipt.
     * Each task is linked to a receipt line. Tasks are distributed across lines in round-robin fashion.
     * 
     * VALIDATION: All receipt lines must have a valid SKU assigned before tasks can be created.
     * This ensures that tasks can be properly executed during receiving workflow.
     * 
     * @param receiptId receipt ID
     * @param taskType type of tasks to create
     * @param count number of tasks to generate
     * @throws IllegalArgumentException if receipt not found
     * @throws IllegalStateException if any receipt line is missing SKU
     */
    @Transactional
    public void createReceivingTasks(Long receiptId, TaskType taskType, int count) {
        Receipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new IllegalArgumentException("Receipt not found: " + receiptId));
        
        List<ReceiptLine> lines = receipt.getLines();
        
        // VALIDATION: Check that receipt has lines
        if (lines == null || lines.isEmpty()) {
            throw new IllegalStateException(
                "Cannot create tasks for receipt " + receipt.getDocNo() + 
                ": receipt has no lines");
        }
        
        // VALIDATION: Check that all lines have SKU assigned
        for (ReceiptLine line : lines) {
            if (line.getSkuId() == null) {
                throw new IllegalStateException(
                    "Cannot create tasks for receipt " + receipt.getDocNo() + 
                    ": line #" + line.getLineNo() + " has no SKU assigned. " +
                    "All lines must have valid SKU before receiving tasks can be created.");
            }
        }
        
        // Create tasks and distribute them across lines in round-robin fashion
        for (int i = 0; i < count; i++) {
            ReceiptLine line = lines.get(i % lines.size());
            
            Task t = new Task();
            t.setReceipt(receipt);
            t.setLine(line);
            t.setTaskType(taskType);
            t.setStatus(TaskStatus.NEW);
            t.setQtyAssigned(line.getQtyExpected()); // Set expected quantity
            taskRepository.save(t);
        }
    }
}
