
package com.rasahub.product_inventory_service.dto.purchaserequest;

import com.rasahub.product_inventory_service.entity.PurchaseRequestItem;
import com.rasahub.product_inventory_service.enums.MeasurementUnit;

import java.math.BigDecimal;

public class PurchaseRequestItemResponse {

    private final Long id;
    private final Long inventoryItemId;
    private final String itemCode;
    private final String itemName;
    private final MeasurementUnit measurementUnit;
    private final BigDecimal requestedQuantity;

    public PurchaseRequestItemResponse(
            PurchaseRequestItem item
    ) {
        this.id = item.getId();

        this.inventoryItemId =
                item.getInventoryItem().getId();

        this.itemCode =
                item.getInventoryItem().getItemCode();

        this.itemName =
                item.getInventoryItem().getItemName();

        this.measurementUnit =
                item.getInventoryItem().getMeasurementUnit();

        this.requestedQuantity = item.getRequestedQuantity();
    }

    public Long getId() {
        return id;
    }

    public Long getInventoryItemId() {
        return inventoryItemId;
    }

    public String getItemCode() {
        return itemCode;
    }

    public String getItemName() {
        return itemName;
    }

    public MeasurementUnit getMeasurementUnit() {
        return measurementUnit;
    }

    public BigDecimal getRequestedQuantity() {
        return requestedQuantity;
    }
}
