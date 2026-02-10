package com.wmsdipl.core.domain;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "skus")
public class Sku {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 32)
    private String uom;

    @Column(name = "pallet_capacity", precision = 10, scale = 2)
    private java.math.BigDecimal palletCapacity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 16)
    private SkuStatus status = SkuStatus.ACTIVE;

    public Sku() {}

    public Sku(String code, String name, String uom) {
        this.code = code;
        this.name = name;
        this.uom = uom;
    }
}
