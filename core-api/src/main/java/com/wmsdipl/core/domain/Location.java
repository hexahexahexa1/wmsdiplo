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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "locations")
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id", nullable = false)
    private Zone zone;

    @Column(nullable = false, unique = true, length = 32)
    private String code;

    @Column(length = 8)
    private String aisle;

    @Column(length = 8)
    private String bay;

    @Column(length = 8)
    private String level;

    @Column(name = "x_coord", precision = 10, scale = 2)
    private BigDecimal xCoord;

    @Column(name = "y_coord", precision = 10, scale = 2)
    private BigDecimal yCoord;

    @Column(name = "z_coord", precision = 10, scale = 2)
    private BigDecimal zCoord;

    @Column(name = "max_weight_kg", precision = 10, scale = 2)
    private BigDecimal maxWeightKg;

    @Column(name = "max_height_cm", precision = 10, scale = 2)
    private BigDecimal maxHeightCm;

    @Column(name = "max_width_cm", precision = 10, scale = 2)
    private BigDecimal maxWidthCm;

    @Column(name = "max_depth_cm", precision = 10, scale = 2)
    private BigDecimal maxDepthCm;

    @Column(name = "max_pallets")
    private Integer maxPallets = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private LocationStatus status = LocationStatus.AVAILABLE;

    @Enumerated(EnumType.STRING)
    @Column(name = "location_type", length = 32)
    private LocationType locationType = LocationType.STORAGE;

    @Column(name = "is_active")
    private Boolean active = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        if (status == null) {
            status = LocationStatus.AVAILABLE;
        }
        if (maxPallets == null) {
            maxPallets = 1;
        }
        if (active == null) {
            active = true;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
