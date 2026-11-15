package com.rasahub.product_inventory_service.dto.stockadjustment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class StockAdjustmentItemRequest {

    @NotNull(message = "Inventory item ID is required")
    @Positive(message = "Inventory item ID must be positive")
    private Long inventoryItemId;

    @NotNull(message = "Adjustment quantity is required")
    @DecimalMin(
            value = "0.000",
            inclusive = false,
            message = "Adjustment quantity must be greater than zero"
    )
    @Digits(
            integer = 15,
            fraction = 3,
            message = "Adjustment quantity must have at most 15 integer digits and 3 decimals"
    )
    private BigDecimal adjustmentQuantity;

    public Long getInventoryItemId() {
        return inventoryItemId;
    }

    public void setInventoryItemId(Long inventoryItemId) {
        this.inventoryItemId = inventoryItemId;
    }

    public BigDecimal getAdjustmentQuantity() {
        return adjustmentQuantity;
    }

    public void setAdjustmentQuantity(BigDecimal adjustmentQuantity) {
        this.adjustmentQuantity = adjustmentQuantity;
    }
}
