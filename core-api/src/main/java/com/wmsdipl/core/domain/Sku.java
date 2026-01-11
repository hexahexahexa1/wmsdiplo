package com.wmsdipl.core.domain;

import jakarta.persistence.*;
import java.time.Instant;

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

    public Sku() {}

    public Sku(String code, String name, String uom) {
        this.code = code;
        this.name = name;
        this.uom = uom;
    }
}
