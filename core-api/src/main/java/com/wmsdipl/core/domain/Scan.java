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

@Entity
@Table(name = "scans")
public class Scan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private Task task;

    @Column(name = "pallet_code", length = 64)
    private String palletCode;

    @Column(length = 64)
    private String sscc;

    @Column(length = 128)
    private String barcode;

    @Column(precision = 18, scale = 3)
    private BigDecimal qty;

    @Column(name = "device_id", length = 128)
    private String deviceId;

    @Column
    private Boolean discrepancy = false;

    @Column(name = "damage_flag")
    private Boolean damageFlag = false;

    @Column(name = "damage_type", length = 64)
    private String damageType;

    @Column(name = "damage_description", length = 512)
    private String damageDescription;

    @Column(name = "lot_number", length = 128)
    private String lotNumber;

    @Column(name = "expiry_date")
    private java.time.LocalDate expiryDate;

    @Column(name = "scanned_at")
    private LocalDateTime scannedAt;

    @PrePersist
    void onCreate() {
        if (scannedAt == null) {
            scannedAt = LocalDateTime.now();
        }
        if (discrepancy == null) {
            discrepancy = false;
        }
    }

    public Long getId() {
        return id;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public String getPalletCode() {
        return palletCode;
    }

    public void setPalletCode(String palletCode) {
        this.palletCode = palletCode;
    }

    public String getSscc() {
        return sscc;
    }

    public void setSscc(String sscc) {
        this.sscc = sscc;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public BigDecimal getQty() {
        return qty;
    }

    public void setQty(BigDecimal qty) {
        this.qty = qty;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public Boolean getDiscrepancy() {
        return discrepancy;
    }

    public void setDiscrepancy(Boolean discrepancy) {
        this.discrepancy = discrepancy;
    }

    public LocalDateTime getScannedAt() {
        return scannedAt;
    }

    public void setScannedAt(LocalDateTime scannedAt) {
        this.scannedAt = scannedAt;
    }

    public Boolean getDamageFlag() {
        return damageFlag;
    }

    public void setDamageFlag(Boolean damageFlag) {
        this.damageFlag = damageFlag;
    }

    public String getDamageType() {
        return damageType;
    }

    public void setDamageType(String damageType) {
        this.damageType = damageType;
    }

    public String getDamageDescription() {
        return damageDescription;
    }

    public void setDamageDescription(String damageDescription) {
        this.damageDescription = damageDescription;
    }

    public String getLotNumber() {
        return lotNumber;
    }

    public void setLotNumber(String lotNumber) {
        this.lotNumber = lotNumber;
    }

    public java.time.LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(java.time.LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }
}
