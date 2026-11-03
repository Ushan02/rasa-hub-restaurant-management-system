
package com.rasahub.product_inventory_service.dto.inventory;

import com.rasahub.product_inventory_service.entity.InventoryItem;
import com.rasahub.product_inventory_service.enums.InventoryItemType;
import com.rasahub.product_inventory_service.enums.MeasurementUnit;

import java.time.LocalDateTime;

public class InventoryItemResponse {

    private final Long id;
    private final String itemCode;
    private final String itemName;
    private final String description;
    private final InventoryItemType itemType;
    private final MeasurementUnit measurementUnit;
    private final boolean active;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public InventoryItemResponse(InventoryItem item) {
        this.id = item.getId();
        this.itemCode = item.getItemCode();
        this.itemName = item.getItemName();
        this.description = item.getDescription();
        this.itemType = item.getItemType();
        this.measurementUnit = item.getMeasurementUnit();
        this.active = item.isActive();
        this.createdAt = item.getCreatedAt();
        this.updatedAt = item.getUpdatedAt();
    }

    public Long getId() {
        return id;
    }

    public String getItemCode() {
        return itemCode;
    }

    public String getItemName() {
        return itemName;
    }

    public String getDescription() {
        return description;
    }

    public InventoryItemType getItemType() {
        return itemType;
    }

    public MeasurementUnit getMeasurementUnit() {
        return measurementUnit;
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
