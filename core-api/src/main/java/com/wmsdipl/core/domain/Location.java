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

    public Long getId() {
        return id;
    }

    public Zone getZone() {
        return zone;
    }

    public void setZone(Zone zone) {
        this.zone = zone;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getAisle() {
        return aisle;
    }

    public void setAisle(String aisle) {
        this.aisle = aisle;
    }

    public String getBay() {
        return bay;
    }

    public void setBay(String bay) {
        this.bay = bay;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public BigDecimal getXCoord() {
        return xCoord;
    }

    public void setXCoord(BigDecimal xCoord) {
        this.xCoord = xCoord;
    }

    public BigDecimal getYCoord() {
        return yCoord;
    }

    public void setYCoord(BigDecimal yCoord) {
        this.yCoord = yCoord;
    }

    public BigDecimal getZCoord() {
        return zCoord;
    }

    public void setZCoord(BigDecimal zCoord) {
        this.zCoord = zCoord;
    }

    public BigDecimal getMaxWeightKg() {
        return maxWeightKg;
    }

    public void setMaxWeightKg(BigDecimal maxWeightKg) {
        this.maxWeightKg = maxWeightKg;
    }

    public BigDecimal getMaxHeightCm() {
        return maxHeightCm;
    }

    public void setMaxHeightCm(BigDecimal maxHeightCm) {
        this.maxHeightCm = maxHeightCm;
    }

    public BigDecimal getMaxWidthCm() {
        return maxWidthCm;
    }

    public void setMaxWidthCm(BigDecimal maxWidthCm) {
        this.maxWidthCm = maxWidthCm;
    }

    public BigDecimal getMaxDepthCm() {
        return maxDepthCm;
    }

    public void setMaxDepthCm(BigDecimal maxDepthCm) {
        this.maxDepthCm = maxDepthCm;
    }

    public Integer getMaxPallets() {
        return maxPallets;
    }

    public void setMaxPallets(Integer maxPallets) {
        this.maxPallets = maxPallets;
    }

    public LocationStatus getStatus() {
        return status;
    }

    public void setStatus(LocationStatus status) {
        this.status = status;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
