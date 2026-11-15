package com.rasahub.product_inventory_service.dto.dailyrawmaterialusage;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class DailyRawMaterialUsageItemRequest {

    @NotNull(message = "Inventory item ID is required")
    @Positive(message = "Inventory item ID must be positive")
    private Long inventoryItemId;

    @NotNull(message = "Used quantity is required")
    @DecimalMin(
            value = "0.000",
            inclusive = false,
            message = "Used quantity must be greater than zero"
    )
    @Digits(
            integer = 15,
            fraction = 3,
            message = "Used quantity must have at most 15 integer digits and 3 decimal places"
    )
    private BigDecimal usedQuantity;

    public DailyRawMaterialUsageItemRequest() {
    }

    public Long getInventoryItemId() {
        return inventoryItemId;
    }

    public void setInventoryItemId(Long inventoryItemId) {
        this.inventoryItemId = inventoryItemId;
    }

    public BigDecimal getUsedQuantity() {
        return usedQuantity;
    }

    public void setUsedQuantity(BigDecimal usedQuantity) {
        this.usedQuantity = usedQuantity;
    }
}
