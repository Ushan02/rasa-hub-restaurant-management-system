package com.rasahub.product_inventory_service.dto.stockcount;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class StockCountItemRequest {

    @NotNull(message = "Inventory item ID is required")
    @Positive(message = "Inventory item ID must be positive")
    private Long inventoryItemId;

    @DecimalMin(
            value = "0.000",
            message = "Physical quantity cannot be negative"
    )
    @Digits(
            integer = 15,
            fraction = 3,
            message = "Physical quantity must have at most 15 integer digits and 3 decimals"
    )
    private BigDecimal physicalQuantity;

    @Size(max = 300, message = "Item remarks must not exceed 300 characters")
    private String itemRemarks;

    public Long getInventoryItemId() {
        return inventoryItemId;
    }

    public void setInventoryItemId(Long inventoryItemId) {
        this.inventoryItemId = inventoryItemId;
    }

    public BigDecimal getPhysicalQuantity() {
        return physicalQuantity;
    }

    public void setPhysicalQuantity(BigDecimal physicalQuantity) {
        this.physicalQuantity = physicalQuantity;
    }

    public String getItemRemarks() {
        return itemRemarks;
    }

    public void setItemRemarks(String itemRemarks) {
        this.itemRemarks = itemRemarks;
    }
}
