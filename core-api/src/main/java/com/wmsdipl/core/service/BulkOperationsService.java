package com.wmsdipl.core.service;

import com.wmsdipl.contracts.dto.BulkAssignRequest;
import com.wmsdipl.contracts.dto.BulkCreatePalletsRequest;
import com.wmsdipl.contracts.dto.BulkOperationFailure;
import com.wmsdipl.contracts.dto.BulkOperationResult;
import com.wmsdipl.contracts.dto.BulkSetPriorityRequest;
import com.wmsdipl.contracts.dto.PalletCreationResult;
import com.wmsdipl.core.domain.Location;
import com.wmsdipl.core.domain.LocationStatus;
import com.wmsdipl.core.domain.LocationType;
import com.wmsdipl.core.domain.Pallet;
import com.wmsdipl.core.domain.PalletStatus;
import com.wmsdipl.core.domain.Receipt;
import com.wmsdipl.core.domain.Task;
import com.wmsdipl.core.domain.TaskStatus;
import com.wmsdipl.core.repository.LocationRepository;
import com.wmsdipl.core.repository.PalletRepository;
import com.wmsdipl.core.repository.ReceiptRepository;
import com.wmsdipl.core.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for bulk operations on tasks and pallets.
 * Provides best-effort execution with partial success support.
 */
@Service
public class BulkOperationsService {

    private final TaskRepository taskRepository;
    private final PalletRepository palletRepository;
    private final LocationRepository locationRepository;
    private final ReceiptRepository receiptRepository;
    private final TaskLifecycleService taskLifecycleService;

    public BulkOperationsService(
            TaskRepository taskRepository,
            PalletRepository palletRepository,
            LocationRepository locationRepository,
            ReceiptRepository receiptRepository,
            TaskLifecycleService taskLifecycleService
    ) {
        this.taskRepository = taskRepository;
        this.palletRepository = palletRepository;
        this.locationRepository = locationRepository;
        this.receiptRepository = receiptRepository;
        this.taskLifecycleService = taskLifecycleService;
    }

    /**
     * Assigns multiple tasks to an operator.
     * Best-effort: continues on errors, returns success/failure counts.
     * 
     * @param request bulk assign request with task IDs and assignee info
     * @return result with success/failure counts and failure details
     */
    public BulkOperationResult<Long> bulkAssignTasks(BulkAssignRequest request) {
        List<Long> successes = new ArrayList<>();
        List<BulkOperationFailure> failures = new ArrayList<>();
        String assignedBy = resolveCurrentUsername();

        for (Long taskId : request.taskIds()) {
            try {
                taskLifecycleService.assign(taskId, request.assignee(), assignedBy);
                successes.add(taskId);
            } catch (Exception e) {
                failures.add(new BulkOperationFailure(
                    taskId,
                    e.getMessage()
                ));
            }
        }

        return new BulkOperationResult<>(
            successes,
            failures
        );
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
     * Sets priority for multiple tasks.
     * Best-effort: continues on errors, returns success/failure counts.
     * 
     * @param request bulk priority request with task IDs and priority value
     * @return result with success/failure counts and failure details
     */
    @Transactional
    public BulkOperationResult<Long> bulkSetPriority(BulkSetPriorityRequest request) {
        List<Long> successes = new ArrayList<>();
        List<BulkOperationFailure> failures = new ArrayList<>();

        for (Long taskId : request.taskIds()) {
            try {
                Task task = taskRepository.findById(taskId)
                    .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
                task.setPriority(request.priority());
                taskRepository.save(task);
                successes.add(taskId);
            } catch (Exception e) {
                failures.add(new BulkOperationFailure(
                    taskId,
                    e.getMessage()
                ));
            }
        }

        return new BulkOperationResult<>(
            successes,
            failures
        );
    }

    /**
     * Creates multiple pallets with auto-generated codes (PLT-XXX series).
     * Pallets are created in RECEIVING status and assigned to RECEIVING location.
     * Uses startNumber from request for sequential numbering.
     * Best-effort: continues on errors, returns success/failure counts.
     * 
     * @param request bulk pallet creation request with start number and count
     * @return result with created pallet codes and failure details
     */
    @Transactional
    public PalletCreationResult bulkCreatePallets(BulkCreatePalletsRequest request) {
        // Find RECEIVING location for initial placement
        Location receivingLocation = locationRepository
            .findFirstByLocationTypeAndStatusAndActiveTrueOrderByIdAsc(
                LocationType.RECEIVING, LocationStatus.AVAILABLE
            )
            .orElse(null);

        List<String> createdPalletCodes = new ArrayList<>();
        List<BulkOperationFailure> failures = new ArrayList<>();

        int currentNumber = request.startNumber();
        for (int i = 0; i < request.count(); i++) {
            try {
                // Generate pallet code with sequential numbering
                String palletCode = String.format("PLT-%05d", currentNumber);

                Pallet pallet = new Pallet();
                pallet.setCode(palletCode);
                pallet.setStatus(PalletStatus.RECEIVING);  // Use RECEIVING instead of DRAFT
                pallet.setQuantity(BigDecimal.ZERO);
                pallet.setLocation(receivingLocation);

                palletRepository.save(pallet);
                createdPalletCodes.add(palletCode);
                currentNumber++;
            } catch (Exception e) {
                failures.add(new BulkOperationFailure(
                    (long) currentNumber,
                    e.getMessage()
                ));
                currentNumber++;
            }
        }

        return new PalletCreationResult(
            createdPalletCodes,
            failures
        );
    }

    /**
     * Cancels multiple tasks.
     * Best-effort: continues on errors, returns success/failure counts.
     * 
     * @param taskIds list of task IDs to cancel
     * @return result with success/failure counts and failure details
     */
    @Transactional
    public BulkOperationResult<Long> bulkCancelTasks(List<Long> taskIds) {
        List<Long> successes = new ArrayList<>();
        List<BulkOperationFailure> failures = new ArrayList<>();

        for (Long taskId : taskIds) {
            try {
                Task task = taskRepository.findById(taskId)
                    .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
                
                if (task.getStatus() != TaskStatus.COMPLETED) {
                    task.setStatus(TaskStatus.CANCELLED);
                    task.setClosedAt(java.time.LocalDateTime.now());
                    taskRepository.save(task);
                    successes.add(taskId);
                } else {
                    failures.add(new BulkOperationFailure(
                        taskId,
                        "Cannot cancel completed task"
                    ));
                }
            } catch (Exception e) {
                failures.add(new BulkOperationFailure(
                    taskId,
                    e.getMessage()
                ));
            }
        }

        return new BulkOperationResult<>(
            successes,
            failures
        );
    }
}
