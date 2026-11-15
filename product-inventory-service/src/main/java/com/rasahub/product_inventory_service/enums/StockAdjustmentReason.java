package com.rasahub.product_inventory_service.enums;

public enum StockAdjustmentReason {
    DAMAGED,
    EXPIRED,
    LOST,
    FOUND,
    MANUAL_CORRECTION;

    public boolean allows(StockAdjustmentDirection direction) {
        if (direction == null) {
            return false;
        }

        return switch (this) {
            case DAMAGED, EXPIRED, LOST ->
                    direction == StockAdjustmentDirection.OUT;
            case FOUND -> direction == StockAdjustmentDirection.IN;
            case MANUAL_CORRECTION -> true;
        };
    }
}
