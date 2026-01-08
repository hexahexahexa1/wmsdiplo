package com.wmsdipl.core.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sku_storage_config")
public class SkuStorageConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sku_id", nullable = false, unique = true)
    private Long skuId;

    @Column(name = "velocity_class", length = 1)
    private String velocityClass = "C";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preferred_zone_id")
    private Zone preferredZone;

    @Column(name = "hazmat_class", length = 16)
    private String hazmatClass;

    @Column(name = "temp_range", length = 32)
    private String tempRange;

    @Column(name = "min_stock", precision = 18, scale = 3)
    private BigDecimal minStock;

    @Column(name = "max_stock", precision = 18, scale = 3)
    private BigDecimal maxStock;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public Long getSkuId() {
        return skuId;
    }

    public void setSkuId(Long skuId) {
        this.skuId = skuId;
    }

    public String getVelocityClass() {
        return velocityClass;
    }

    public void setVelocityClass(String velocityClass) {
        this.velocityClass = velocityClass;
    }

    public Zone getPreferredZone() {
        return preferredZone;
    }

    public void setPreferredZone(Zone preferredZone) {
        this.preferredZone = preferredZone;
    }

    public String getHazmatClass() {
        return hazmatClass;
    }

    public void setHazmatClass(String hazmatClass) {
        this.hazmatClass = hazmatClass;
    }

    public String getTempRange() {
        return tempRange;
    }

    public void setTempRange(String tempRange) {
        this.tempRange = tempRange;
    }

    public BigDecimal getMinStock() {
        return minStock;
    }

    public void setMinStock(BigDecimal minStock) {
        this.minStock = minStock;
    }

    public BigDecimal getMaxStock() {
        return maxStock;
    }

    public void setMaxStock(BigDecimal maxStock) {
        this.maxStock = maxStock;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
