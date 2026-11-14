
package com.rasahub.product_inventory_service.dto.branchstockrequest;

import com.rasahub.product_inventory_service.entity.BranchStockRequestItem;
import com.rasahub.product_inventory_service.entity.InventoryItem;
import com.rasahub.product_inventory_service.enums.MeasurementUnit;

import java.math.BigDecimal;

public class BranchStockRequestItemResponse {

    private final Long id;
    private final Long inventoryItemId;
    private final String itemCode;
    private final String itemName;
    private final MeasurementUnit measurementUnit;
    private final BigDecimal requestedQuantity;
    private final BigDecimal receivedQuantity;
    private final BigDecimal outstandingQuantity;
    private final boolean fullyFulfilled;

    public BranchStockRequestItemResponse(
            BranchStockRequestItem item
    ) {
        InventoryItem inventoryItem =
                item.getInventoryItem();

        this.id = item.getId();

        this.inventoryItemId =
                inventoryItem.getId();

        this.itemCode =
                inventoryItem.getItemCode();

        this.itemName =
                inventoryItem.getItemName();

        this.measurementUnit =
                inventoryItem.getMeasurementUnit();

        this.requestedQuantity =
                item.getRequestedQuantity();

        this.receivedQuantity =
                item.getReceivedQuantity();

        this.outstandingQuantity =
                item.getOutstandingQuantity();

        this.fullyFulfilled =
                item.isFullyFulfilled();
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

    public BigDecimal getReceivedQuantity() {
        return receivedQuantity;
    }

    public BigDecimal getOutstandingQuantity() {
        return outstandingQuantity;
    }

    public boolean isFullyFulfilled() {
        return fullyFulfilled;
    }
}
