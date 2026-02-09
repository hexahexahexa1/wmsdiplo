package com.wmsdipl.core.service.workflow;

import com.wmsdipl.contracts.dto.ShippingWaveActionResultDto;
import com.wmsdipl.contracts.dto.ShippingWaveDto;
import com.wmsdipl.core.domain.Receipt;
import com.wmsdipl.core.domain.ReceiptStatus;
import com.wmsdipl.core.domain.Task;
import com.wmsdipl.core.domain.TaskStatus;
import com.wmsdipl.core.domain.TaskType;
import com.wmsdipl.core.repository.ReceiptRepository;
import com.wmsdipl.core.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ShippingWaveService {

    private final ReceiptRepository receiptRepository;
    private final TaskRepository taskRepository;
    private final ShippingWorkflowService shippingWorkflowService;

    public ShippingWaveService(
        ReceiptRepository receiptRepository,
        TaskRepository taskRepository,
        ShippingWorkflowService shippingWorkflowService
    ) {
        this.receiptRepository = receiptRepository;
        this.taskRepository = taskRepository;
        this.shippingWorkflowService = shippingWorkflowService;
    }

    @Transactional(readOnly = true)
    public List<ShippingWaveDto> listWaves() {
        List<Receipt> receipts = receiptRepository.findByCrossDockTrueAndOutboundRefIsNotNull().stream()
            .filter(receipt -> receipt.getOutboundRef() != null && !receipt.getOutboundRef().isBlank())
            .toList();

        Map<String, List<Receipt>> byWave = receipts.stream()
            .collect(Collectors.groupingBy(receipt -> receipt.getOutboundRef().trim()));

        return byWave.entrySet().stream()
            .map(entry -> toWaveDto(entry.getKey(), entry.getValue()))
            .sorted(Comparator.comparing(ShippingWaveDto::outboundRef))
            .toList();
    }

    @Transactional
    public ShippingWaveActionResultDto startWave(String outboundRef) {
        List<Receipt> receipts = findWaveReceipts(outboundRef);
        List<Long> blockedReceiptIds = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        int affected = 0;
        int createdTasks = 0;

        for (Receipt receipt : receipts) {
            if (receipt.getStatus() == ReceiptStatus.SHIPPED || receipt.getStatus() == ReceiptStatus.SHIPPING_IN_PROGRESS) {
                continue;
            }
            if (receipt.getStatus() != ReceiptStatus.READY_FOR_SHIPMENT) {
                blockedReceiptIds.add(receipt.getId());
                warnings.add("Receipt " + receipt.getDocNo() + " is in status " + receipt.getStatus());
                continue;
            }
            try {
                int createdForReceipt = shippingWorkflowService.startShipping(receipt.getId());
                affected++;
                createdTasks += createdForReceipt;
            } catch (RuntimeException ex) {
                blockedReceiptIds.add(receipt.getId());
                warnings.add("Receipt " + receipt.getDocNo() + ": " + ex.getMessage());
            }
        }

        return new ShippingWaveActionResultDto(
            outboundRef,
            receipts.size(),
            affected,
            createdTasks,
            blockedReceiptIds,
            warnings
        );
    }

    @Transactional
    public ShippingWaveActionResultDto completeWave(String outboundRef) {
        List<Receipt> receipts = findWaveReceipts(outboundRef);
        List<Long> blockedReceiptIds = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        int affected = 0;

        Map<Long, List<Task>> shippingTasksByReceipt = taskRepository.findByTaskTypeAndReceiptIdIn(
                TaskType.SHIPPING,
                receipts.stream().map(Receipt::getId).filter(Objects::nonNull).toList()
            ).stream()
            .filter(task -> task.getReceipt() != null && task.getReceipt().getId() != null)
            .collect(Collectors.groupingBy(task -> task.getReceipt().getId()));

        for (Receipt receipt : receipts) {
            if (receipt.getStatus() == ReceiptStatus.SHIPPED) {
                continue;
            }
            if (receipt.getStatus() != ReceiptStatus.SHIPPING_IN_PROGRESS) {
                blockedReceiptIds.add(receipt.getId());
                warnings.add("Receipt " + receipt.getDocNo() + " is in status " + receipt.getStatus());
                continue;
            }

            List<Task> shippingTasks = shippingTasksByReceipt.getOrDefault(receipt.getId(), List.of());
            if (shippingTasks.isEmpty()) {
                blockedReceiptIds.add(receipt.getId());
                warnings.add("Receipt " + receipt.getDocNo() + " has no shipping tasks");
                continue;
            }

            boolean hasOpenTasks = shippingTasks.stream().anyMatch(task -> task.getStatus() != TaskStatus.COMPLETED);
            if (hasOpenTasks) {
                blockedReceiptIds.add(receipt.getId());
                warnings.add("Receipt " + receipt.getDocNo() + " has incomplete shipping tasks");
            }
        }

        if (!blockedReceiptIds.isEmpty()) {
            return new ShippingWaveActionResultDto(
                outboundRef,
                receipts.size(),
                0,
                0,
                blockedReceiptIds,
                warnings
            );
        }

        for (Receipt receipt : receipts) {
            if (receipt.getStatus() == ReceiptStatus.SHIPPING_IN_PROGRESS) {
                shippingWorkflowService.completeShipping(receipt.getId());
                affected++;
            }
        }

        return new ShippingWaveActionResultDto(
            outboundRef,
            receipts.size(),
            affected,
            0,
            blockedReceiptIds,
            warnings
        );
    }

    private ShippingWaveDto toWaveDto(String outboundRef, List<Receipt> receipts) {
        List<Long> receiptIds = receipts.stream().map(Receipt::getId).filter(Objects::nonNull).toList();
        List<Task> shippingTasks = receiptIds.isEmpty()
            ? List.of()
            : taskRepository.findByTaskTypeAndReceiptIdIn(TaskType.SHIPPING, receiptIds);
        EnumSet<ReceiptStatus> receiptStatuses = receipts.stream()
            .map(Receipt::getStatus)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(ReceiptStatus.class)));

        int ready = countByStatus(receipts, ReceiptStatus.READY_FOR_SHIPMENT);
        int inProgress = countByStatus(receipts, ReceiptStatus.SHIPPING_IN_PROGRESS);
        int shipped = countByStatus(receipts, ReceiptStatus.SHIPPED);
        int completedTasks = (int) shippingTasks.stream().filter(task -> task.getStatus() == TaskStatus.COMPLETED).count();
        int openTasks = (int) shippingTasks.stream()
            .filter(task -> task.getStatus() != TaskStatus.COMPLETED && task.getStatus() != TaskStatus.CANCELLED)
            .count();

        String waveStatus;
        if (shipped == receipts.size()) {
            waveStatus = "COMPLETED";
        } else if (inProgress > 0) {
            waveStatus = "IN_PROGRESS";
        } else if (ready == receipts.size()) {
            waveStatus = "READY";
        } else if (receiptStatuses.size() == 1) {
            // Keep wave status explicit when all receipts are in the same non-shipping state (e.g. DRAFT).
            waveStatus = receiptStatuses.iterator().next().name();
        } else {
            waveStatus = "MIXED";
        }

        return new ShippingWaveDto(
            outboundRef,
            receipts.size(),
            ready,
            inProgress,
            shipped,
            openTasks,
            completedTasks,
            waveStatus
        );
    }

    private int countByStatus(Collection<Receipt> receipts, ReceiptStatus status) {
        return (int) receipts.stream().filter(receipt -> receipt.getStatus() == status).count();
    }

    private List<Receipt> findWaveReceipts(String outboundRef) {
        if (outboundRef == null || outboundRef.isBlank()) {
            return List.of();
        }
        return receiptRepository.findByCrossDockTrueAndOutboundRef(outboundRef.trim());
    }
}
