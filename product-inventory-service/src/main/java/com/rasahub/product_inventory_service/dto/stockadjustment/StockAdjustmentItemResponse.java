package com.rasahub.product_inventory_service.dto.stockadjustment;

import com.rasahub.product_inventory_service.entity.InventoryItem;
import com.rasahub.product_inventory_service.entity.StockAdjustmentItem;
import com.rasahub.product_inventory_service.enums.MeasurementUnit;

import java.math.BigDecimal;

public class StockAdjustmentItemResponse {

    private final Long id;
    private final Long inventoryItemId;
    private final String inventoryItemCode;
    private final String inventoryItemName;
    private final MeasurementUnit measurementUnit;
    private final BigDecimal adjustmentQuantity;

    public StockAdjustmentItemResponse(StockAdjustmentItem item) {
        InventoryItem inventoryItem = item.getInventoryItem();
        id = item.getId();
        inventoryItemId = inventoryItem.getId();
        inventoryItemCode = inventoryItem.getItemCode();
        inventoryItemName = inventoryItem.getItemName();
        measurementUnit = inventoryItem.getMeasurementUnit();
        adjustmentQuantity = item.getAdjustmentQuantity();
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

    public BigDecimal getAdjustmentQuantity() {
        return adjustmentQuantity;
    }
}
