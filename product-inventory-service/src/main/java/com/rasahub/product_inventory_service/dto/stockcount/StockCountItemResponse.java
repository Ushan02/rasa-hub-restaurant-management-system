package com.rasahub.product_inventory_service.dto.stockcount;

import com.rasahub.product_inventory_service.entity.StockCountItem;

import java.math.BigDecimal;

public class StockCountItemResponse {

    private final Long id;
    private final Long inventoryItemId;
    private final String itemCode;
    private final String itemName;
    private final String measurementUnit;
    private final BigDecimal systemQuantity;
    private final Long systemBalanceVersion;
    private final BigDecimal physicalQuantity;
    private final BigDecimal varianceQuantity;
    private final String itemRemarks;

    public StockCountItemResponse(StockCountItem item) {
        id = item.getId();
        inventoryItemId = item.getInventoryItem().getId();
        itemCode = item.getInventoryItem().getItemCode();
        itemName = item.getInventoryItem().getItemName();
        measurementUnit = item.getInventoryItem()
                .getMeasurementUnit().name();
        systemQuantity = item.getSystemQuantity();
        systemBalanceVersion = item.getSystemBalanceVersion();
        physicalQuantity = item.getPhysicalQuantity();
        varianceQuantity = item.getVarianceQuantity();
        itemRemarks = item.getItemRemarks();
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

    public String getMeasurementUnit() {
        return measurementUnit;
    }

    public BigDecimal getSystemQuantity() {
        return systemQuantity;
    }

    public Long getSystemBalanceVersion() {
        return systemBalanceVersion;
    }

    public BigDecimal getPhysicalQuantity() {
        return physicalQuantity;
    }

    public BigDecimal getVarianceQuantity() {
        return varianceQuantity;
    }

    public String getItemRemarks() {
        return itemRemarks;
    }
}
