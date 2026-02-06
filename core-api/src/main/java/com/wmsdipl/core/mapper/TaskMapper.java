package com.wmsdipl.core.mapper;

import com.wmsdipl.contracts.dto.TaskDto;
import com.wmsdipl.core.domain.Location;
import com.wmsdipl.core.domain.Receipt;
import com.wmsdipl.core.domain.ReceiptLine;
import com.wmsdipl.core.domain.Task;
import com.wmsdipl.core.domain.Pallet;
import com.wmsdipl.core.domain.Sku;
import com.wmsdipl.core.repository.LocationRepository;
import com.wmsdipl.core.repository.PalletRepository;
import com.wmsdipl.core.repository.SkuRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

    public List<TaskDto> toDtoList(List<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, String> targetLocationCodes = locationRepository.findAllById(
            tasks.stream()
                .map(Task::getTargetLocationId)
                .filter(Objects::nonNull)
                .distinct()
                .toList()
        ).stream().collect(java.util.stream.Collectors.toMap(Location::getId, Location::getCode));

        Map<Long, Pallet> palletsById = palletRepository.findAllById(
            tasks.stream()
                .map(Task::getPalletId)
                .filter(Objects::nonNull)
                .distinct()
                .toList()
        ).stream().collect(java.util.stream.Collectors.toMap(Pallet::getId, pallet -> pallet));

        Map<Long, String> skuCodesById = loadSkuCodes(
            tasks,
            palletsById.values()
        );

        return tasks.stream()
            .map(task -> toDto(task, targetLocationCodes, palletsById, skuCodesById))
            .toList();
    }
    
    public TaskDto toDto(Task task) {
        Map<Long, Pallet> pallets = new HashMap<>();
        if (task.getPalletId() != null) {
            palletRepository.findById(task.getPalletId()).ifPresent(pallet -> pallets.put(pallet.getId(), pallet));
        }
        Map<Long, String> locationCodes = new HashMap<>();
        if (task.getTargetLocationId() != null) {
            locationRepository.findById(task.getTargetLocationId())
                .ifPresent(location -> locationCodes.put(location.getId(), location.getCode()));
        }
        Map<Long, String> skuCodes = loadSkuCodes(List.of(task), pallets.values());
        return toDto(task, locationCodes, pallets, skuCodes);
    }

    private Map<Long, String> loadSkuCodes(List<Task> tasks, Collection<Pallet> pallets) {
        List<Long> skuIdsFromLines = tasks.stream()
            .map(Task::getLine)
            .filter(Objects::nonNull)
            .map(ReceiptLine::getSkuId)
            .filter(Objects::nonNull)
            .toList();
        List<Long> skuIdsFromPallets = pallets.stream()
            .map(Pallet::getSkuId)
            .filter(Objects::nonNull)
            .toList();

        List<Long> allSkuIds = java.util.stream.Stream.concat(
            skuIdsFromLines.stream(),
            skuIdsFromPallets.stream()
        ).distinct().toList();

        return skuRepository.findAllById(allSkuIds).stream()
            .collect(java.util.stream.Collectors.toMap(Sku::getId, Sku::getCode));
    }

    private TaskDto toDto(
            Task task,
            Map<Long, String> targetLocationCodes,
            Map<Long, Pallet> palletsById,
            Map<Long, String> skuCodesById
    ) {
        String targetLocationCode = task.getTargetLocationId() != null
            ? targetLocationCodes.get(task.getTargetLocationId())
            : null;
        Pallet pallet = task.getPalletId() != null ? palletsById.get(task.getPalletId()) : null;
        String palletCode = pallet != null ? pallet.getCode() : null;

        Long skuId = null;
        ReceiptLine line = task.getLine();
        if (line != null && line.getSkuId() != null) {
            skuId = line.getSkuId();
        } else if (pallet != null) {
            skuId = pallet.getSkuId();
        }
        String skuCode = skuId != null ? skuCodesById.get(skuId) : null;

        Receipt receipt = task.getReceipt();
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
                receipt != null ? receipt.getId() : null,
                receipt != null ? receipt.getDocNo() : null,
                line != null ? line.getId() : null,
                line != null ? line.getSkuId() : null,
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
