package com.wmsdipl.core.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "receipt_lines")
public class ReceiptLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_id", nullable = false)
    private Receipt receipt;

    @Column(name = "sku_id")
    private Long skuId;

    @Column(name = "packaging_id")
    private Long packagingId;

    @Column(name = "uom")
    private String uom;

    @Column(name = "qty_expected", nullable = false)
    private BigDecimal qtyExpected;

    @Column(name = "sscc_expected")
    private String ssccExpected;

    @Column(name = "line_no")
    private Integer lineNo;
}
