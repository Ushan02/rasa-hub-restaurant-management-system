
package com.rasahub.product_inventory_service.dto.supplieritem;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class SupplierItemCreateRequest {

    @NotNull(message = "Supplier ID is required")
    @Positive(message = "Supplier ID must be positive")
    private Long supplierId;

    @NotNull(message = "Inventory item ID is required")
    @Positive(message = "Inventory item ID must be positive")
    private Long inventoryItemId;

    @NotNull(message = "Unit price is required")
    @DecimalMin(
            value = "0.00",
            inclusive = false,
            message = "Unit price must be greater than zero"
    )
    @Digits(integer = 13, fraction = 2)
    private BigDecimal unitPrice;

    @NotNull(message = "Minimum order quantity is required")
    @DecimalMin(
            value = "0.000",
            inclusive = false,
            message = "Minimum order quantity must be greater than zero"
    )
    @Digits(integer = 12, fraction = 3)
    private BigDecimal minimumOrderQuantity;

    @Min(value = 0, message = "Lead time cannot be negative")
    private Integer leadTimeDays;

    public SupplierItemCreateRequest() {
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public Long getInventoryItemId() {
        return inventoryItemId;
    }

    public void setInventoryItemId(Long inventoryItemId) {
        this.inventoryItemId = inventoryItemId;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getMinimumOrderQuantity() {
        return minimumOrderQuantity;
    }

    public void setMinimumOrderQuantity(
            BigDecimal minimumOrderQuantity
    ) {
        this.minimumOrderQuantity = minimumOrderQuantity;
    }

    public Integer getLeadTimeDays() {
        return leadTimeDays;
    }

    public void setLeadTimeDays(Integer leadTimeDays) {
        this.leadTimeDays = leadTimeDays;
    }
}
