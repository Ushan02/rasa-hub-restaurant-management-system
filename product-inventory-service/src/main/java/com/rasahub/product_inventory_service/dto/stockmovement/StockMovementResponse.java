
package com.rasahub.product_inventory_service.dto.stockmovement;

import com.rasahub.product_inventory_service.entity.StockMovement;
import com.rasahub.product_inventory_service.enums.MeasurementUnit;
import com.rasahub.product_inventory_service.enums.StockMovementType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class StockMovementResponse {

    private final Long id;

    private final String locationCode;
    private final String locationName;

    private final Long inventoryItemId;
    private final String itemCode;
    private final String itemName;
    private final MeasurementUnit measurementUnit;

    private final StockMovementType movementType;

    private final BigDecimal quantity;
    private final BigDecimal quantityBefore;
    private final BigDecimal quantityAfter;

    private final String referenceType;
    private final String referenceId;

    private final Long performedByAccountId;
    private final String remarks;

    private final LocalDateTime createdAt;

    public StockMovementResponse(StockMovement movement) {

        this.id = movement.getId();

        this.locationCode = movement
                .getStockLocation()
                .getLocationCode();

        this.locationName = movement
                .getStockLocation()
                .getLocationName();

        this.inventoryItemId = movement
                .getInventoryItem()
                .getId();

        this.itemCode = movement
                .getInventoryItem()
                .getItemCode();

        this.itemName = movement
                .getInventoryItem()
                .getItemName();

        this.measurementUnit = movement
                .getInventoryItem()
                .getMeasurementUnit();

        this.movementType = movement.getMovementType();

        this.quantity = movement.getQuantity();
        this.quantityBefore = movement.getQuantityBefore();
        this.quantityAfter = movement.getQuantityAfter();

        this.referenceType = movement.getReferenceType();
        this.referenceId = movement.getReferenceId();

        this.performedByAccountId =
                movement.getPerformedByAccountId();

        this.remarks = movement.getRemarks();
        this.createdAt = movement.getCreatedAt();
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

    public StockMovementType getMovementType() {
        return movementType;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getQuantityBefore() {
        return quantityBefore;
    }

    public BigDecimal getQuantityAfter() {
        return quantityAfter;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public Long getPerformedByAccountId() {
        return performedByAccountId;
    }

    public String getRemarks() {
        return remarks;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
