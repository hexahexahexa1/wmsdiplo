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
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tasks")
@Getter
@Setter
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

    @Version
    @Column(name = "entity_version", nullable = false)
    private Long entityVersion = 0L;

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
}
