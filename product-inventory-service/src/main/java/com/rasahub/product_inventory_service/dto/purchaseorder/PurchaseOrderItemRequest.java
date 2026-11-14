
package com.rasahub.product_inventory_service.dto.purchaseorder;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class PurchaseOrderItemRequest {

    @NotNull(message = "Inventory item ID is required")
    @Positive(message = "Inventory item ID must be positive")
    private Long inventoryItemId;

    @NotNull(message = "Ordered quantity is required")
    @DecimalMin(
            value = "0.000",
            inclusive = false,
            message = "Ordered quantity must be greater than zero"
    )
    @Digits(
            integer = 15,
            fraction = 3,
            message = "Maximum 15 integer digits and 3 decimal places allowed"
    )
    private BigDecimal orderedQuantity;

    @NotNull(message = "Unit price is required")
    @DecimalMin(
            value = "0.00",
            inclusive = false,
            message = "Unit price must be greater than zero"
    )
    @Digits(
            integer = 13,
            fraction = 2,
            message = "Maximum 13 integer digits and 2 decimal places allowed"
    )
    private BigDecimal unitPrice;

    public PurchaseOrderItemRequest() {
    }

    public Long getInventoryItemId() {
        return inventoryItemId;
    }

    public void setInventoryItemId(Long inventoryItemId) {
        this.inventoryItemId = inventoryItemId;
    }

    public BigDecimal getOrderedQuantity() {
        return orderedQuantity;
    }

    public void setOrderedQuantity(BigDecimal orderedQuantity) {
        this.orderedQuantity = orderedQuantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }
}
