package com.wmsdipl.core.service.putaway;

import com.wmsdipl.core.domain.Location;
import com.wmsdipl.core.domain.Receipt;
import com.wmsdipl.core.domain.Zone;

public class PutawayContext {
    private final Receipt receipt;
    private final Zone preferredZone;
    private final String velocityClass;
    private final String skuCategory;
    private final Location currentLocation;

    public PutawayContext(Receipt receipt, Zone preferredZone, String velocityClass, String skuCategory, Location currentLocation) {
        this.receipt = receipt;
        this.preferredZone = preferredZone;
        this.velocityClass = velocityClass;
        this.skuCategory = skuCategory;
        this.currentLocation = currentLocation;
    }

    public Receipt getReceipt() {
        return receipt;
    }

    public Zone getPreferredZone() {
        return preferredZone;
    }

    public String getVelocityClass() {
        return velocityClass;
    }

    public String getSkuCategory() {
        return skuCategory;
    }

    public Location getCurrentLocation() {
        return currentLocation;
    }
}
