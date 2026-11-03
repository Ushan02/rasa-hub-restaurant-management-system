
package com.rasahub.product_inventory_service.dto.inventory;

import com.rasahub.product_inventory_service.enums.InventoryItemType;
import com.rasahub.product_inventory_service.enums.MeasurementUnit;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class InventoryItemUpdateRequest {

    @Pattern(
            regexp = ".*\\S.*",
            message = "Item name must not be blank"
    )
    @Size(max = 150, message = "Item name must not exceed 150 characters")
    private String itemName;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    private InventoryItemType itemType;

    private MeasurementUnit measurementUnit;

    public InventoryItemUpdateRequest() {
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public InventoryItemType getItemType() {
        return itemType;
    }

    public void setItemType(InventoryItemType itemType) {
        this.itemType = itemType;
    }

    public MeasurementUnit getMeasurementUnit() {
        return measurementUnit;
    }

    public void setMeasurementUnit(MeasurementUnit measurementUnit) {
        this.measurementUnit = measurementUnit;
    }
}
