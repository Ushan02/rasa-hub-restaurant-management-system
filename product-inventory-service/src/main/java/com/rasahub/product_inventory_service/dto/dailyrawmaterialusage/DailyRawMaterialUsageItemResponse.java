package com.rasahub.product_inventory_service.dto.dailyrawmaterialusage;

import com.rasahub.product_inventory_service.entity.DailyRawMaterialUsageItem;
import com.rasahub.product_inventory_service.entity.InventoryItem;
import com.rasahub.product_inventory_service.enums.MeasurementUnit;

import java.math.BigDecimal;

public class DailyRawMaterialUsageItemResponse {

    private final Long id;
    private final Long inventoryItemId;
    private final String inventoryItemCode;
    private final String inventoryItemName;
    private final MeasurementUnit measurementUnit;
    private final BigDecimal usedQuantity;

    public DailyRawMaterialUsageItemResponse(
            DailyRawMaterialUsageItem item
    ) {
        InventoryItem inventoryItem = item.getInventoryItem();
        this.id = item.getId();
        this.inventoryItemId = inventoryItem.getId();
        this.inventoryItemCode = inventoryItem.getItemCode();
        this.inventoryItemName = inventoryItem.getItemName();
        this.measurementUnit = inventoryItem.getMeasurementUnit();
        this.usedQuantity = item.getUsedQuantity();
    }

    public Long getId() {
        return id;
    }

    public Long getInventoryItemId() {
        return inventoryItemId;
    }

    public String getInventoryItemCode() {
        return inventoryItemCode;
    }

    public String getInventoryItemName() {
        return inventoryItemName;
    }

    public MeasurementUnit getMeasurementUnit() {
        return measurementUnit;
    }

    public BigDecimal getUsedQuantity() {
        return usedQuantity;
    }
}
