
package com.rasahub.product_inventory_service.dto.goodsreceipt;

import com.rasahub.product_inventory_service.entity.GoodsReceiptItem;
import com.rasahub.product_inventory_service.entity.InventoryItem;
import com.rasahub.product_inventory_service.enums.MeasurementUnit;

import java.math.BigDecimal;

public class GoodsReceiptItemResponse {

    private final Long id;
    private final Long inventoryItemId;
    private final String itemCode;
    private final String itemName;
    private final MeasurementUnit measurementUnit;
    private final BigDecimal deliveredQuantity;
    private final BigDecimal acceptedQuantity;
    private final BigDecimal rejectedQuantity;

    public GoodsReceiptItemResponse(
            GoodsReceiptItem item
    ) {
        InventoryItem inventoryItem =
                item.getInventoryItem();

        this.id = item.getId();

        this.inventoryItemId = inventoryItem.getId();

        this.itemCode = inventoryItem.getItemCode();

        this.itemName = inventoryItem.getItemName();

        this.measurementUnit =
                inventoryItem.getMeasurementUnit();

        this.deliveredQuantity =
                item.getDeliveredQuantity();

        this.acceptedQuantity =
                item.getAcceptedQuantity();

        this.rejectedQuantity =
                item.getRejectedQuantity();
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

    public BigDecimal getDeliveredQuantity() {
        return deliveredQuantity;
    }

    public BigDecimal getAcceptedQuantity() {
        return acceptedQuantity;
    }

    public BigDecimal getRejectedQuantity() {
        return rejectedQuantity;
    }
}
