package com.wmsdipl.core.mapper;

import com.wmsdipl.contracts.dto.TaskDto;
import com.wmsdipl.core.domain.Task;
import com.wmsdipl.core.domain.Pallet;
import com.wmsdipl.core.domain.Sku;
import com.wmsdipl.core.repository.LocationRepository;
import com.wmsdipl.core.repository.PalletRepository;
import com.wmsdipl.core.repository.SkuRepository;
import org.springframework.stereotype.Component;

@Component
public class TaskMapper {
    
    private final LocationRepository locationRepository;
    private final SkuRepository skuRepository;
    private final PalletRepository palletRepository;
    
    public TaskMapper(LocationRepository locationRepository, SkuRepository skuRepository, PalletRepository palletRepository) {
        this.locationRepository = locationRepository;
        this.skuRepository = skuRepository;
        this.palletRepository = palletRepository;
    }
    
    public TaskDto toDto(Task task) {
        String targetLocationCode = null;
        if (task.getTargetLocationId() != null) {
            targetLocationCode = locationRepository.findById(task.getTargetLocationId())
                .map(location -> location.getCode())
                .orElse(null);
        }

        String skuCode = null;
        if (task.getLine() != null && task.getLine().getSkuId() != null) {
            skuCode = skuRepository.findById(task.getLine().getSkuId())
                .map(Sku::getCode)
                .orElse(null);
        } else if (task.getPalletId() != null) {
            skuCode = palletRepository.findById(task.getPalletId())
                .flatMap(pallet -> {
                    if (pallet.getSkuId() != null) {
                        return skuRepository.findById(pallet.getSkuId()).map(Sku::getCode);
                    }
                    return java.util.Optional.empty();
                })
                .orElse(null);
        }

        String palletCode = null;
        if (task.getPalletId() != null) {
            palletCode = palletRepository.findById(task.getPalletId())
                .map(Pallet::getCode)
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
            skuCode,
            palletCode,
            task.getPriority(),
            task.getCreatedAt(),
            task.getStartedAt(),
            task.getClosedAt()
        );
    }
}
