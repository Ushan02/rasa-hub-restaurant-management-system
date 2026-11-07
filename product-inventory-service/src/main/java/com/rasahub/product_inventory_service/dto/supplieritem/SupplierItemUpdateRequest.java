
package com.rasahub.product_inventory_service.dto.supplieritem;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;

public class SupplierItemUpdateRequest {

    @DecimalMin(
            value = "0.00",
            inclusive = false,
            message = "Unit price must be greater than zero"
    )
    @Digits(
            integer = 13,
            fraction = 2,
            message = "Unit price must have at most 13 integer digits and 2 decimal places"
    )
    private BigDecimal unitPrice;

    @DecimalMin(
            value = "0.000",
            inclusive = false,
            message = "Minimum order quantity must be greater than zero"
    )
    @Digits(
            integer = 12,
            fraction = 3,
            message = "Minimum order quantity must have at most 12 integer digits and 3 decimal places"
    )
    private BigDecimal minimumOrderQuantity;

    @Min(
            value = 0,
            message = "Lead time cannot be negative"
    )
    private Integer leadTimeDays;

    public SupplierItemUpdateRequest() {
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
