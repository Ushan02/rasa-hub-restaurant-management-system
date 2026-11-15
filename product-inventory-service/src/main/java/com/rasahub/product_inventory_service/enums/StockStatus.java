package com.rasahub.product_inventory_service.enums;

import java.math.BigDecimal;

public enum StockStatus {
    NORMAL,
    LOW_STOCK,
    OUT_OF_STOCK;

    public static StockStatus fromQuantity(
            BigDecimal currentQuantity,
            BigDecimal reorderLevel
    ) {
        BigDecimal current = currentQuantity == null
                ? BigDecimal.ZERO
                : currentQuantity;
        BigDecimal threshold = reorderLevel == null
                ? BigDecimal.ZERO
                : reorderLevel;

        if (current.compareTo(BigDecimal.ZERO) == 0) {
            return OUT_OF_STOCK;
        }

        if (current.compareTo(threshold) <= 0) {
            return LOW_STOCK;
        }

        return NORMAL;
    }
}
