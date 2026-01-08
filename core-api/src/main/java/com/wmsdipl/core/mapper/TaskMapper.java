package com.wmsdipl.core.mapper;

import com.wmsdipl.contracts.dto.TaskDto;
import com.wmsdipl.core.domain.Task;
import org.springframework.stereotype.Component;

@Component
public class TaskMapper {
    
    public TaskDto toDto(Task task) {
        return new TaskDto(
            task.getId(),
            task.getTaskType() != null ? task.getTaskType().name() : null,
            task.getStatus() != null ? task.getStatus().name() : null,
            task.getAssignee(),
            task.getAssignedBy(),
            task.getPalletId(),
            task.getSourceLocationId(),
            task.getTargetLocationId(),
            task.getReceipt() != null ? task.getReceipt().getId() : null,
            task.getReceipt() != null ? task.getReceipt().getDocNo() : null,
            task.getLine() != null ? task.getLine().getId() : null,
            task.getLine() != null ? task.getLine().getSkuId() : null,
            task.getQtyAssigned(),
            task.getQtyDone(),
            task.getPriority(),
            task.getCreatedAt(),
            task.getStartedAt(),
            task.getClosedAt()
        );
    }
}
