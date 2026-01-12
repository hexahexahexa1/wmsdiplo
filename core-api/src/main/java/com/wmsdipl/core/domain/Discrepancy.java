package com.wmsdipl.core.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "discrepancies")
public class Discrepancy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_id", nullable = false)
    private Receipt receipt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "line_id")
    private ReceiptLine line;

    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "pallet_id")
    private Long palletId;

    @Column(length = 50, nullable = false)
    private String type;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "qty_expected", precision = 10, scale = 2)
    private BigDecimal qtyExpected;

    @Column(name = "qty_actual", precision = 10, scale = 2)
    private BigDecimal qtyActual;

    @Column
    private Boolean resolved = false;

    @Column(name = "resolved_by", length = 100)
    private String resolvedBy;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (resolved == null) {
            resolved = false;
        }
    }
}
