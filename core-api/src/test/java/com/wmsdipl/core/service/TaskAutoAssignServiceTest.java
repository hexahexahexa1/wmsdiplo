package com.wmsdipl.core.service;

import com.wmsdipl.contracts.dto.AutoAssignRequest;
import com.wmsdipl.contracts.dto.AutoAssignResultDto;
import com.wmsdipl.core.domain.Task;
import com.wmsdipl.core.domain.TaskStatus;
import com.wmsdipl.core.domain.User;
import com.wmsdipl.core.domain.UserRole;
import com.wmsdipl.core.repository.TaskRepository;
import com.wmsdipl.core.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskAutoAssignServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TaskLifecycleService taskLifecycleService;

    @InjectMocks
    private TaskAutoAssignService taskAutoAssignService;

    @Test
    void shouldDryRunAutoAssign_ByMinimalLoad() {
        Task task1 = new Task();
        task1.setId(1L);
        task1.setStatus(TaskStatus.NEW);
        Task task2 = new Task();
        task2.setId(2L);
        task2.setStatus(TaskStatus.NEW);

        User op1 = user("operator1");
        User op2 = user("operator2");

        when(taskRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(task1, task2));
        when(userRepository.findAll()).thenReturn(List.of(op1, op2));
        when(taskRepository.findByAssigneeAndStatus("operator1", TaskStatus.ASSIGNED)).thenReturn(List.of(new Task()));
        when(taskRepository.findByAssigneeAndStatus("operator1", TaskStatus.IN_PROGRESS)).thenReturn(List.of());
        when(taskRepository.findByAssigneeAndStatus("operator2", TaskStatus.ASSIGNED)).thenReturn(List.of());
        when(taskRepository.findByAssigneeAndStatus("operator2", TaskStatus.IN_PROGRESS)).thenReturn(List.of());

        AutoAssignResultDto result = taskAutoAssignService.dryRun(new AutoAssignRequest(List.of(1L, 2L), false));

        assertEquals(2, result.totalCandidates());
        assertEquals(2, result.assignedCount());
        assertEquals("operator2", result.items().get(0).suggestedAssignee());
    }

    @Test
    void shouldApplyAutoAssign_ForNewTasks() {
        Task task1 = new Task();
        task1.setId(10L);
        task1.setStatus(TaskStatus.NEW);

        User op1 = user("operator1");
        when(taskRepository.findAllById(List.of(10L))).thenReturn(List.of(task1));
        when(userRepository.findAll()).thenReturn(List.of(op1));
        when(taskRepository.findByAssigneeAndStatus("operator1", TaskStatus.ASSIGNED)).thenReturn(List.of());
        when(taskRepository.findByAssigneeAndStatus("operator1", TaskStatus.IN_PROGRESS)).thenReturn(List.of());

        AutoAssignResultDto result = taskAutoAssignService.apply(new AutoAssignRequest(List.of(10L), false));

        assertEquals(1, result.assignedCount());
        verify(taskLifecycleService).assign(10L, "operator1", "auto-assign");
    }

    @Test
    void shouldSkipInProgressTask_WhenReassignAssignedEnabled() {
        Task task = new Task();
        task.setId(20L);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setAssignee("operator1");

        User op1 = user("operator1");
        when(taskRepository.findAllById(List.of(20L))).thenReturn(List.of(task));
        when(userRepository.findAll()).thenReturn(List.of(op1));
        when(taskRepository.findByAssigneeAndStatus("operator1", TaskStatus.ASSIGNED)).thenReturn(List.of());
        when(taskRepository.findByAssigneeAndStatus("operator1", TaskStatus.IN_PROGRESS)).thenReturn(List.of(new Task()));

        AutoAssignResultDto result = taskAutoAssignService.dryRun(new AutoAssignRequest(List.of(20L), true));

        assertEquals(1, result.totalCandidates());
        assertEquals(0, result.assignedCount());
        assertEquals(1, result.skippedCount());
        assertNotNull(result.items());
        assertEquals("SKIP_STATUS", result.items().get(0).decision());
        verify(taskLifecycleService, never()).assign(anyLong(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }

    private User user(String username) {
        User user = new User();
        user.setUsername(username);
        user.setRole(UserRole.OPERATOR);
        user.setActive(true);
        return user;
    }
}
