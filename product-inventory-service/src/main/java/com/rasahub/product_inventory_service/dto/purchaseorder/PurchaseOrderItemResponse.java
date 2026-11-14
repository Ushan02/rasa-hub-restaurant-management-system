
package com.rasahub.product_inventory_service.dto.purchaseorder;

import com.rasahub.product_inventory_service.entity.InventoryItem;
import com.rasahub.product_inventory_service.entity.PurchaseOrderItem;
import com.rasahub.product_inventory_service.enums.MeasurementUnit;

import java.math.BigDecimal;

public class PurchaseOrderItemResponse {

    private final Long id;
    private final Long inventoryItemId;
    private final String itemCode;
    private final String itemName;
    private final MeasurementUnit measurementUnit;
    private final BigDecimal orderedQuantity;
    private final BigDecimal unitPrice;
    private final BigDecimal receivedQuantity;
    private final BigDecimal outstandingQuantity;
    private final BigDecimal lineTotal;
    private final boolean fullyReceived;

    public PurchaseOrderItemResponse(
            PurchaseOrderItem item
    ) {
        InventoryItem inventoryItem = item.getInventoryItem();

        this.id = item.getId();

        this.inventoryItemId = inventoryItem.getId();

        this.itemCode = inventoryItem.getItemCode();

        this.itemName = inventoryItem.getItemName();

        this.measurementUnit = inventoryItem.getMeasurementUnit();

        this.orderedQuantity = item.getOrderedQuantity();

        this.unitPrice = item.getUnitPrice();

        this.receivedQuantity = item.getReceivedQuantity();

        this.outstandingQuantity = item.getOutstandingQuantity();

        this.lineTotal = item.getLineTotal();

        this.fullyReceived = item.isFullyReceived();
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

    public BigDecimal getOrderedQuantity() {
        return orderedQuantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getReceivedQuantity() {
        return receivedQuantity;
    }

    public BigDecimal getOutstandingQuantity() {
        return outstandingQuantity;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public boolean isFullyReceived() {
        return fullyReceived;
    }
}
