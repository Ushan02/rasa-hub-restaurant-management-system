
package com.rasahub.product_inventory_service.dto.stocklocation;

import com.rasahub.product_inventory_service.entity.StockLocation;
import com.rasahub.product_inventory_service.enums.StockLocationType;

import java.time.LocalDateTime;

public class StockLocationResponse {

    private final String locationCode;
    private final String locationName;
    private final StockLocationType locationType;
    private final boolean active;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public StockLocationResponse(StockLocation stockLocation) {

        this.locationCode = stockLocation.getLocationCode();
        this.locationName = stockLocation.getLocationName();
        this.locationType = stockLocation.getLocationType();
        this.active = stockLocation.isActive();
        this.createdAt = stockLocation.getCreatedAt();
        this.updatedAt = stockLocation.getUpdatedAt();
    }

    public String getLocationCode() {
        return locationCode;
    }

    public String getLocationName() {
        return locationName;
    }

    public StockLocationType getLocationType() {
        return locationType;
    }

    public boolean isActive() {
        return active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
