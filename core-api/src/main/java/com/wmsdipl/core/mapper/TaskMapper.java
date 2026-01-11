package com.wmsdipl.core.mapper;

import com.wmsdipl.contracts.dto.TaskDto;
import com.wmsdipl.core.domain.Task;
import com.wmsdipl.core.repository.LocationRepository;
import org.springframework.stereotype.Component;

@Component
public class TaskMapper {
    
    private final LocationRepository locationRepository;
    
    public TaskMapper(LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }
    
    public TaskDto toDto(Task task) {
        String targetLocationCode = null;
        if (task.getTargetLocationId() != null) {
            targetLocationCode = locationRepository.findById(task.getTargetLocationId())
                .map(location -> location.getCode())
                .orElse(null);
        }
        
        return new TaskDto(
            task.getId(),
            task.getTaskType() != null ? task.getTaskType().name() : null,
            task.getStatus() != null ? task.getStatus().name() : null,
            task.getAssignee(),
            task.getAssignedBy(),
            task.getPalletId(),
            task.getSourceLocationId(),
            task.getTargetLocationId(),
            targetLocationCode,
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
