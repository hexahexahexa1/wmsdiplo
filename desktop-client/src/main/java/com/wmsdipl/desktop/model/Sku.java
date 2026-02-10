package com.wmsdipl.desktop.model;

/**
 * SKU (Stock Keeping Unit) model for desktop client.
 * Represents a product in the warehouse catalog.
 */
public record Sku(
    Long id,
    String code,
    String name,
    String uom,
    String status
) {}
