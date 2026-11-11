
package com.rasahub.product_inventory_service.dto.stockbalance;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class StockBalanceMinimumLevelUpdateRequest {

    @NotNull(message = "Minimum stock level is required")
    @DecimalMin(
            value = "0.000",
            message = "Minimum stock level cannot be negative"
    )
    @Digits(
            integer = 15,
            fraction = 3,
            message = "Maximum 15 integer digits and 3 decimal places allowed"
    )
    private BigDecimal minimumStockLevel;

    public StockBalanceMinimumLevelUpdateRequest() {
    }

    public BigDecimal getMinimumStockLevel() {
        return minimumStockLevel;
    }

    public void setMinimumStockLevel(
            BigDecimal minimumStockLevel
    ) {
        this.minimumStockLevel = minimumStockLevel;
    }
}
