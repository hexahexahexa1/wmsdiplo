package com.wmsdipl.core.repository;

import com.wmsdipl.core.domain.Task;
import com.wmsdipl.core.domain.TaskStatus;
import com.wmsdipl.core.domain.TaskType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByReceiptId(Long receiptId);
    List<Task> findByReceiptIdAndTaskType(Long receiptId, TaskType taskType);
    List<Task> findByStatus(TaskStatus status);
    
    /**
     * Finds tasks by status and type.
     * Used for terminal to filter tasks (e.g., all NEW RECEIVING tasks).
     */
    List<Task> findByStatusAndTaskType(TaskStatus status, TaskType taskType);
    
    /**
     * Finds all tasks assigned to a specific operator.
     * Used in terminal to show "My Tasks" view.
     */
    List<Task> findByAssignee(String assignee);
    
    /**
     * Finds tasks by assignee and status.
     * Used to filter "My tasks in progress", "My assigned tasks", etc.
     */
    List<Task> findByAssigneeAndStatus(String assignee, TaskStatus status);
}
