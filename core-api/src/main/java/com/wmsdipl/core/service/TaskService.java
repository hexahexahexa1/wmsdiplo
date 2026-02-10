package com.wmsdipl.core.service;

import com.wmsdipl.contracts.dto.UndoLastScanResultDto;
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
import com.wmsdipl.core.service.workflow.PlacementWorkflowService;
import com.wmsdipl.core.service.workflow.ReceivingWorkflowService;
import com.wmsdipl.core.service.workflow.ShippingWorkflowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import jakarta.persistence.criteria.Predicate;

import java.math.BigInteger;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * Service for task management and querying.
 * Delegates lifecycle operations to TaskLifecycleService.
 */
@Service
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);

    private final TaskRepository taskRepository;
    private final ReceiptRepository receiptRepository;
    private final DiscrepancyRepository discrepancyRepository;
    private final ScanRepository scanRepository;
    private final TaskLifecycleService taskLifecycleService;
    private final TaskScanUndoService taskScanUndoService;
    private final ReceivingWorkflowService receivingWorkflowService;
    private final PlacementWorkflowService placementWorkflowService;
    private final ShippingWorkflowService shippingWorkflowService;
    private final AuditLogService auditLogService;

    public TaskService(
            TaskRepository taskRepository,
            ReceiptRepository receiptRepository,
            DiscrepancyRepository discrepancyRepository,
            ScanRepository scanRepository,
            TaskLifecycleService taskLifecycleService,
            TaskScanUndoService taskScanUndoService,
            ReceivingWorkflowService receivingWorkflowService,
            PlacementWorkflowService placementWorkflowService,
            ShippingWorkflowService shippingWorkflowService,
            AuditLogService auditLogService
    ) {
        this.taskRepository = taskRepository;
        this.receiptRepository = receiptRepository;
        this.discrepancyRepository = discrepancyRepository;
        this.scanRepository = scanRepository;
        this.taskLifecycleService = taskLifecycleService;
        this.taskScanUndoService = taskScanUndoService;
        this.receivingWorkflowService = receivingWorkflowService;
        this.placementWorkflowService = placementWorkflowService;
        this.shippingWorkflowService = shippingWorkflowService;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<Task> findAll() {
        return taskRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Page<Task> findFiltered(
            String assignee,
            TaskStatus status,
            TaskType taskType,
            Long receiptId,
            Long taskId,
            Pageable pageable
    ) {
        Specification<Task> spec = Specification.where(null);
        if (assignee != null && !assignee.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("assignee"), assignee));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (taskType != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("taskType"), taskType));
        }
        if (receiptId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("receipt").get("id"), receiptId));
        }
        if (taskId != null) {
            List<long[]> prefixRanges = buildTaskIdPrefixRanges(taskId);
            spec = spec.and((root, query, cb) -> {
                if (prefixRanges.isEmpty()) {
                    return cb.disjunction();
                }
                List<Predicate> predicates = new ArrayList<>();
                for (long[] range : prefixRanges) {
                    predicates.add(cb.between(root.get("id"), range[0], range[1]));
                }
                return cb.or(predicates.toArray(new Predicate[0]));
            });
        }
        return taskRepository.findAll(spec, pageable);
    }

    private List<long[]> buildTaskIdPrefixRanges(Long taskIdPrefix) {
        if (taskIdPrefix == null || taskIdPrefix <= 0) {
            return List.of();
        }

        String prefixText = taskIdPrefix.toString();
        int maxDigits = Long.toString(Long.MAX_VALUE).length();
        int maxSuffixDigits = maxDigits - prefixText.length();

        BigInteger prefix = BigInteger.valueOf(taskIdPrefix);
        BigInteger nextPrefix = prefix.add(BigInteger.ONE);
        BigInteger maxLong = BigInteger.valueOf(Long.MAX_VALUE);

        List<long[]> ranges = new ArrayList<>();
        for (int suffixDigits = 0; suffixDigits <= maxSuffixDigits; suffixDigits++) {
            BigInteger multiplier = BigInteger.TEN.pow(suffixDigits);
            BigInteger lowerBound = prefix.multiply(multiplier);
            if (lowerBound.compareTo(maxLong) > 0) {
                break;
            }

            BigInteger upperBound = nextPrefix.multiply(multiplier).subtract(BigInteger.ONE);
            if (upperBound.compareTo(maxLong) > 0) {
                upperBound = maxLong;
            }
            if (upperBound.compareTo(lowerBound) < 0) {
                continue;
            }

            ranges.add(new long[] { lowerBound.longValueExact(), upperBound.longValueExact() });
        }
        return ranges;
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
        Task completedTask = taskLifecycleService.complete(id);
        
        // Auto-complete receipt based on task type
        if (completedTask.getReceipt() != null) {
            if (completedTask.getTaskType() == TaskType.RECEIVING) {
                try {
                    receivingWorkflowService.checkAndCompleteReceipt(completedTask.getReceipt().getId());
                } catch (ReceiptWorkflowBlockedException ex) {
                    log.info(
                        "Task {} completed, receipt {} was not advanced due to workflow blockers: {}",
                        completedTask.getId(),
                        completedTask.getReceipt().getId(),
                        ex.getBlockers() != null ? ex.getBlockers().size() : 0
                    );
                }
            } else if (completedTask.getTaskType() == TaskType.PLACEMENT) {
                placementWorkflowService.autoCompleteReceiptIfAllTasksCompleted(completedTask.getReceipt().getId());
            } else if (completedTask.getTaskType() == TaskType.SHIPPING) {
                shippingWorkflowService.autoCompleteShippingIfAllTasksCompleted(completedTask.getReceipt().getId());
            }
        }
        
        return completedTask;
    }

    @Transactional
    public Task cancel(Long id) {
        return taskLifecycleService.cancel(id);
    }

    /**
     * Releases a task back to NEW status.
     * Operator can release an assigned/in-progress task back to the pool.
     * The work already done (qtyDone, scans) is wiped to allow a fresh start.
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
        
        while (scanRepository.findFirstByTaskOrderByScannedAtDescIdDesc(task).isPresent()) {
            taskScanUndoService.undoLastScan(task.getId(), resolveCurrentUsername());
        }

        // Reset to initial state
        task.setStatus(TaskStatus.NEW);
        task.setAssignee(null);
        task.setStartedAt(null);
        task.setQtyDone(BigDecimal.ZERO);

        return taskRepository.save(task);
    }

    @Transactional
    public UndoLastScanResultDto undoLastScan(Long taskId) {
        return taskScanUndoService.undoLastScan(taskId, resolveCurrentUsername());
    }

    @Transactional(readOnly = true)
    public List<Discrepancy> findOpenDiscrepancies() {
        return discrepancyRepository.findByResolvedFalse();
    }

    @Transactional
    public Discrepancy resolveDiscrepancy(Long id, String comment) {
        Discrepancy discrepancy = discrepancyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Discrepancy not found: " + id));
        String normalizedComment = comment == null ? null : comment.trim();
        String oldComment = discrepancy.getDescription();
        discrepancy.setResolved(true);
        discrepancy.setDescription(normalizedComment);
        discrepancy.setResolvedBy(resolveCurrentUsername());
        discrepancy.setResolvedAt(LocalDateTime.now());
        Discrepancy saved = discrepancyRepository.save(discrepancy);
        if (!Objects.equals(oldComment, normalizedComment)) {
            auditLogService.logUpdate(
                "DISCREPANCY",
                saved.getId(),
                resolveCurrentUsername(),
                "comment",
                oldComment,
                normalizedComment
            );
        }
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Discrepancy> findDiscrepancyJournal(
        Long receiptId,
        LocalDateTime from,
        LocalDateTime to,
        String type,
        String docNo,
        Long skuId,
        String operator,
        Boolean resolved
    ) {
        List<Discrepancy> discrepancies = discrepancyRepository.findAll();
        Map<Long, String> taskAssignees = findTaskAssigneesByDiscrepancies(discrepancies);

        String normalizedType = normalizeFilter(type);
        String normalizedDocNo = normalizeFilter(docNo);
        String normalizedOperator = normalizeFilter(operator);

        return discrepancies.stream()
            .filter(d -> receiptId == null || (d.getReceipt() != null && receiptId.equals(d.getReceipt().getId())))
            .filter(d -> from == null || (d.getCreatedAt() != null && !d.getCreatedAt().isBefore(from)))
            .filter(d -> to == null || (d.getCreatedAt() != null && !d.getCreatedAt().isAfter(to)))
            .filter(d -> normalizedType == null || (d.getType() != null && d.getType().equalsIgnoreCase(normalizedType)))
            .filter(d -> normalizedDocNo == null || (
                d.getReceipt() != null
                    && d.getReceipt().getDocNo() != null
                    && d.getReceipt().getDocNo().toLowerCase().contains(normalizedDocNo.toLowerCase())
            ))
            .filter(d -> skuId == null || (d.getLine() != null && skuId.equals(d.getLine().getSkuId())))
            .filter(d -> resolved == null || Objects.equals(resolved, d.getResolved()))
            .filter(d -> normalizedOperator == null || normalizedOperator.equalsIgnoreCase(
                resolveDiscrepancyOperator(d, taskAssignees)
            ))
            .sorted(Comparator.comparing(Discrepancy::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
            .toList();
    }

    @Transactional(readOnly = true)
    public Map<Long, String> findTaskAssigneesByDiscrepancies(List<Discrepancy> discrepancies) {
        if (discrepancies == null || discrepancies.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> taskIds = discrepancies.stream()
            .map(Discrepancy::getTaskId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        if (taskIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return taskRepository.findAllById(taskIds).stream()
            .filter(task -> task.getAssignee() != null && !task.getAssignee().isBlank())
            .collect(Collectors.toMap(Task::getId, Task::getAssignee, (left, right) -> left));
    }

    @Transactional(readOnly = true)
    public String resolveDiscrepancyOperator(Discrepancy discrepancy, Map<Long, String> taskAssignees) {
        if (discrepancy == null) {
            return null;
        }
        String operator = null;
        if (discrepancy.getTaskId() != null && taskAssignees != null) {
            operator = taskAssignees.get(discrepancy.getTaskId());
        }
        if (operator == null || operator.isBlank()) {
            operator = discrepancy.getResolvedBy();
        }
        return (operator == null || operator.isBlank()) ? null : operator;
    }

    @Transactional
    public Discrepancy updateDiscrepancyComment(Long id, String comment) {
        Discrepancy discrepancy = discrepancyRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Discrepancy not found: " + id));
        String oldComment = discrepancy.getDescription();
        String newComment = comment == null ? null : comment.trim();
        discrepancy.setDescription(newComment);
        Discrepancy saved = discrepancyRepository.save(discrepancy);
        if (!Objects.equals(oldComment, newComment)) {
            auditLogService.logUpdate(
                "DISCREPANCY",
                saved.getId(),
                resolveCurrentUsername(),
                "comment",
                oldComment,
                newComment
            );
        }
        return saved;
    }

    private String normalizeFilter(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String resolveCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()
            || "anonymousUser".equalsIgnoreCase(authentication.getName())) {
            return "system";
        }
        return authentication.getName();
    }

    /**
     * Checks if a task has any scans with QUANTITY discrepancies.
     * Quality issues (damage, expired products) are NOT considered discrepancies
     * for the purpose of completion confirmation, as they don't affect quantity matching.
     * 
     * @param taskId task ID
     * @return true if at least one scan has a quantity-related discrepancy 
     *         (excludes DAMAGE and EXPIRED_PRODUCT)
     */
    @Transactional(readOnly = true)
    public boolean hasDiscrepancies(Long taskId) {
        Task task = taskLifecycleService.getTask(taskId);
        List<Scan> scans = scanRepository.findByTask(task);
        
        // Only count as discrepancy if:
        // 1. discrepancy flag is true AND
        // 2. It's NOT a quality issue (damage or expired product)
        return scans.stream().anyMatch(scan -> {
            if (scan.getDiscrepancy() == null || !scan.getDiscrepancy()) {
                return false;
            }
            
            // Exclude quality issues - they don't affect quantity matching
            boolean isDamage = Boolean.TRUE.equals(scan.getDamageFlag());
            boolean isExpired = scan.getExpiryDate() != null 
                && scan.getExpiryDate().isBefore(java.time.LocalDate.now());
            
            // Return true only if it's a quantity discrepancy (not a quality issue)
            return !isDamage && !isExpired;
        });
    }

    /**
     * Updates the priority of a specific task.
     * Higher priority values mean higher urgency (e.g., 200 > 100).
     * Default priority is 100.
     * 
     * @param taskId the task ID
     * @param priority the new priority value
     * @return the updated task
     */
    @Transactional
    public Task setPriority(Long taskId, Integer priority) {
        Task task = taskLifecycleService.getTask(taskId);
        task.setPriority(priority);
        return taskRepository.save(task);
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
        
        List<ReceiptLine> activeLines = lines.stream()
            .filter(line -> !Boolean.TRUE.equals(line.getExcludedFromWorkflow()))
            .toList();
        if (activeLines.isEmpty()) {
            throw new IllegalStateException(
                "Cannot create tasks for receipt " + receipt.getDocNo() +
                ": all lines are excluded from workflow");
        }

        // VALIDATION: Check that all active lines have SKU assigned
        for (ReceiptLine line : activeLines) {
            if (line.getSkuId() == null) {
                throw new IllegalStateException(
                    "Cannot create tasks for receipt " + receipt.getDocNo() + 
                    ": line #" + line.getLineNo() + " has no SKU assigned. " +
                    "All lines must have valid SKU before receiving tasks can be created.");
            }
        }
        
        // Create tasks and distribute them across lines in round-robin fashion
        for (int i = 0; i < count; i++) {
            ReceiptLine line = activeLines.get(i % activeLines.size());
            
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
