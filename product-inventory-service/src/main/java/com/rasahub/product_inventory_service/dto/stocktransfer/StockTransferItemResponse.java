
package com.rasahub.product_inventory_service.dto.stocktransfer;

import com.rasahub.product_inventory_service.entity.InventoryItem;
import com.rasahub.product_inventory_service.entity.StockTransferItem;
import com.rasahub.product_inventory_service.enums.MeasurementUnit;
import java.math.BigDecimal;

public class StockTransferItemResponse {

    private final Long id;
    private final Long inventoryItemId;
    private final String itemCode;
    private final String itemName;
    private final MeasurementUnit measurementUnit;
    private final BigDecimal transferQuantity;

    public StockTransferItemResponse(StockTransferItem item) {
        InventoryItem inventoryItem = item.getInventoryItem();

        id = item.getId();
        inventoryItemId = inventoryItem.getId();
        itemCode = inventoryItem.getItemCode();
        itemName = inventoryItem.getItemName();
        measurementUnit = inventoryItem.getMeasurementUnit();
        transferQuantity = item.getTransferQuantity();
    }

    public Long getId() { return id; }
    public Long getInventoryItemId() { return inventoryItemId; }
    public String getItemCode() { return itemCode; }
    public String getItemName() { return itemName; }
    public MeasurementUnit getMeasurementUnit() { return measurementUnit; }
    public BigDecimal getTransferQuantity() { return transferQuantity; }
}
