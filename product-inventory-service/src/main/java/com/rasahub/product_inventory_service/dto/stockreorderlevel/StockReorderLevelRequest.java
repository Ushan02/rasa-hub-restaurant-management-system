package com.rasahub.product_inventory_service.dto.stockreorderlevel;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record StockReorderLevelRequest(
        @NotNull
        @DecimalMin("0.000")
        @Digits(integer = 15, fraction = 3)
        BigDecimal reorderLevel,
        @DecimalMin("0.000")
        @Digits(integer = 15, fraction = 3)
        BigDecimal recommendedReorderQuantity
) {
}
