package com.wmsdipl.core.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_id", nullable = false)
    private Receipt receipt;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "line_id")
    private ReceiptLine line;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", length = 32)
    private TaskType taskType = TaskType.RECEIVING;

    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false)
    private TaskStatus status = TaskStatus.NEW;

    @Column(length = 128)
    private String assignee;

    @Column(name = "assigned_by", length = 128)
    private String assignedBy;

    @Column(name = "pallet_id")
    private Long palletId;

    @Column(name = "source_location_id")
    private Long sourceLocationId;

    @Column(name = "target_location_id")
    private Long targetLocationId;

    @Column(name = "qty_assigned", precision = 18, scale = 3)
    private BigDecimal qtyAssigned;

    @Column(name = "qty_done", precision = 18, scale = 3)
    private BigDecimal qtyDone;

    @Column(name = "priority")
    private Integer priority = 100;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = TaskStatus.NEW;
        }
        if (taskType == null) {
            taskType = TaskType.RECEIVING;
        }
        if (priority == null) {
            priority = 100;
        }
    }

    public Long getId() {
        return id;
    }

    public Receipt getReceipt() {
        return receipt;
    }

    public void setReceipt(Receipt receipt) {
        this.receipt = receipt;
    }

    public ReceiptLine getLine() {
        return line;
    }

    public void setLine(ReceiptLine line) {
        this.line = line;
    }

    public TaskType getTaskType() {
        return taskType;
    }

    public void setTaskType(TaskType taskType) {
        this.taskType = taskType;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public String getAssignedBy() {
        return assignedBy;
    }

    public void setAssignedBy(String assignedBy) {
        this.assignedBy = assignedBy;
    }

    public Long getPalletId() {
        return palletId;
    }

    public void setPalletId(Long palletId) {
        this.palletId = palletId;
    }

    public Long getSourceLocationId() {
        return sourceLocationId;
    }

    public void setSourceLocationId(Long sourceLocationId) {
        this.sourceLocationId = sourceLocationId;
    }

    public Long getTargetLocationId() {
        return targetLocationId;
    }

    public void setTargetLocationId(Long targetLocationId) {
        this.targetLocationId = targetLocationId;
    }

    public BigDecimal getQtyAssigned() {
        return qtyAssigned;
    }

    public void setQtyAssigned(BigDecimal qtyAssigned) {
        this.qtyAssigned = qtyAssigned;
    }

    public BigDecimal getQtyDone() {
        return qtyDone;
    }

    public void setQtyDone(BigDecimal qtyDone) {
        this.qtyDone = qtyDone;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }
}
