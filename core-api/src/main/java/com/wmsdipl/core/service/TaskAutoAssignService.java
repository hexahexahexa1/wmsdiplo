package com.wmsdipl.core.service;

import com.wmsdipl.contracts.dto.AutoAssignPreviewItemDto;
import com.wmsdipl.contracts.dto.AutoAssignRequest;
import com.wmsdipl.contracts.dto.AutoAssignResultDto;
import com.wmsdipl.core.domain.Task;
import com.wmsdipl.core.domain.TaskStatus;
import com.wmsdipl.core.domain.User;
import com.wmsdipl.core.domain.UserRole;
import com.wmsdipl.core.repository.TaskRepository;
import com.wmsdipl.core.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class TaskAutoAssignService {

    private static final Set<TaskStatus> ACTIVE_LOAD_STATUSES = Set.of(TaskStatus.ASSIGNED, TaskStatus.IN_PROGRESS);

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TaskLifecycleService taskLifecycleService;

    public TaskAutoAssignService(
        TaskRepository taskRepository,
        UserRepository userRepository,
        TaskLifecycleService taskLifecycleService
    ) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.taskLifecycleService = taskLifecycleService;
    }

    @Transactional(readOnly = true)
    public AutoAssignResultDto dryRun(AutoAssignRequest request) {
        return buildPreview(request);
    }

    @Transactional
    public AutoAssignResultDto apply(AutoAssignRequest request) {
        AutoAssignResultDto preview = buildPreview(request);
        if (preview.items().isEmpty()) {
            return preview;
        }

        boolean reassignAssigned = Boolean.TRUE.equals(request.reassignAssigned());
        Map<Long, Task> tasksById = taskRepository.findAllById(
            preview.items().stream().map(AutoAssignPreviewItemDto::taskId).toList()
        ).stream().collect(Collectors.toMap(Task::getId, task -> task));

        List<AutoAssignPreviewItemDto> appliedItems = new ArrayList<>();
        int assignedCount = 0;
        int skippedCount = 0;

        for (AutoAssignPreviewItemDto item : preview.items()) {
            Task task = tasksById.get(item.taskId());
            if (task == null) {
                skippedCount++;
                appliedItems.add(new AutoAssignPreviewItemDto(
                    item.taskId(),
                    item.currentAssignee(),
                    item.suggestedAssignee(),
                    item.suggestedAssigneeLoadBeforeAssign(),
                    "SKIP_NOT_FOUND"
                ));
                continue;
            }
            if (!"ASSIGN".equals(item.decision())) {
                skippedCount++;
                appliedItems.add(item);
                continue;
            }
            if (!reassignAssigned && task.getStatus() != TaskStatus.NEW) {
                skippedCount++;
                appliedItems.add(new AutoAssignPreviewItemDto(
                    item.taskId(),
                    item.currentAssignee(),
                    item.suggestedAssignee(),
                    item.suggestedAssigneeLoadBeforeAssign(),
                    "SKIP_STATUS"
                ));
                continue;
            }
            if (task.getStatus() == TaskStatus.NEW) {
                taskLifecycleService.assign(task.getId(), item.suggestedAssignee(), "auto-assign");
            } else if (task.getStatus() == TaskStatus.ASSIGNED) {
                task.setAssignee(item.suggestedAssignee());
                task.setAssignedBy("auto-assign");
                taskRepository.save(task);
            } else {
                skippedCount++;
                appliedItems.add(new AutoAssignPreviewItemDto(
                    item.taskId(),
                    item.currentAssignee(),
                    item.suggestedAssignee(),
                    item.suggestedAssigneeLoadBeforeAssign(),
                    "SKIP_STATUS"
                ));
                continue;
            }
            assignedCount++;
            appliedItems.add(item);
        }

        return new AutoAssignResultDto(
            preview.totalCandidates(),
            assignedCount,
            skippedCount,
            appliedItems
        );
    }

    private AutoAssignResultDto buildPreview(AutoAssignRequest request) {
        List<Long> taskIds = request.taskIds();
        if (taskIds == null || taskIds.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "taskIds must not be empty");
        }
        boolean reassignAssigned = Boolean.TRUE.equals(request.reassignAssigned());

        List<Task> tasks = taskRepository.findAllById(taskIds);
        if (tasks.isEmpty()) {
            throw new ResponseStatusException(NOT_FOUND, "No tasks found for provided taskIds");
        }

        List<String> operators = userRepository.findAll().stream()
            .filter(user -> Boolean.TRUE.equals(user.getActive()))
            .filter(user -> user.getRole() == UserRole.OPERATOR || user.getRole() == UserRole.PC_OPERATOR)
            .map(User::getUsername)
            .filter(Objects::nonNull)
            .filter(username -> !username.isBlank())
            .sorted()
            .toList();

        if (operators.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "No active operators available for auto-assignment");
        }

        Map<String, Integer> loadByOperator = new HashMap<>();
        for (String operator : operators) {
            int load = 0;
            for (TaskStatus status : ACTIVE_LOAD_STATUSES) {
                load += taskRepository.findByAssigneeAndStatus(operator, status).size();
            }
            loadByOperator.put(operator, load);
        }

        List<Task> orderedTasks = tasks.stream()
            .sorted(Comparator.comparing(Task::getId))
            .toList();
        List<AutoAssignPreviewItemDto> items = new ArrayList<>();
        int assignCount = 0;
        int skipCount = 0;

        for (Task task : orderedTasks) {
            if (task.getStatus() == TaskStatus.COMPLETED
                || task.getStatus() == TaskStatus.CANCELLED
                || task.getStatus() == TaskStatus.IN_PROGRESS) {
                skipCount++;
                items.add(new AutoAssignPreviewItemDto(
                    task.getId(),
                    task.getAssignee(),
                    null,
                    null,
                    "SKIP_STATUS"
                ));
                continue;
            }
            if (!reassignAssigned && task.getStatus() != TaskStatus.NEW) {
                skipCount++;
                items.add(new AutoAssignPreviewItemDto(
                    task.getId(),
                    task.getAssignee(),
                    null,
                    null,
                    "SKIP_STATUS"
                ));
                continue;
            }

            String suggested = operators.stream()
                .min(Comparator.comparingInt((String op) -> loadByOperator.getOrDefault(op, 0))
                    .thenComparing(Comparator.naturalOrder()))
                .orElse(null);
            if (suggested == null) {
                skipCount++;
                items.add(new AutoAssignPreviewItemDto(
                    task.getId(),
                    task.getAssignee(),
                    null,
                    null,
                    "SKIP_NO_OPERATOR"
                ));
                continue;
            }
            int loadBefore = loadByOperator.getOrDefault(suggested, 0);
            loadByOperator.put(suggested, loadBefore + 1);
            assignCount++;
            items.add(new AutoAssignPreviewItemDto(
                task.getId(),
                task.getAssignee(),
                suggested,
                loadBefore,
                "ASSIGN"
            ));
        }

        return new AutoAssignResultDto(
            orderedTasks.size(),
            assignCount,
            skipCount,
            items
        );
    }
}
