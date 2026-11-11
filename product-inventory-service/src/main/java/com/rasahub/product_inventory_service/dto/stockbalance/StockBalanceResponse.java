
package com.rasahub.product_inventory_service.dto.stockbalance;

import com.rasahub.product_inventory_service.entity.StockBalance;
import com.rasahub.product_inventory_service.enums.MeasurementUnit;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class StockBalanceResponse {

    private final Long id;
    private final String locationCode;
    private final String locationName;
    private final Long inventoryItemId;
    private final String itemCode;
    private final String itemName;
    private final MeasurementUnit measurementUnit;
    private final BigDecimal quantityOnHand;
    private final BigDecimal minimumStockLevel;
    private final boolean lowStock;
    private final Long version;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public StockBalanceResponse(StockBalance balance) {

        this.id = balance.getId();

        this.locationCode =
                balance.getStockLocation().getLocationCode();

        this.locationName =
                balance.getStockLocation().getLocationName();

        this.inventoryItemId =
                balance.getInventoryItem().getId();

        this.itemCode =
                balance.getInventoryItem().getItemCode();

        this.itemName =
                balance.getInventoryItem().getItemName();

        this.measurementUnit =
                balance.getInventoryItem().getMeasurementUnit();

        this.quantityOnHand = balance.getQuantityOnHand();
        this.minimumStockLevel = balance.getMinimumStockLevel();
        this.lowStock = balance.isLowStock();
        this.version = balance.getVersion();
        this.createdAt = balance.getCreatedAt();
        this.updatedAt = balance.getUpdatedAt();
    }

    public Long getId() {
        return id;
    }

    public String getLocationCode() {
        return locationCode;
    }

    public String getLocationName() {
        return locationName;
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

    public BigDecimal getQuantityOnHand() {
        return quantityOnHand;
    }

    public BigDecimal getMinimumStockLevel() {
        return minimumStockLevel;
    }

    public boolean isLowStock() {
        return lowStock;
    }

    public Long getVersion() {
        return version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
