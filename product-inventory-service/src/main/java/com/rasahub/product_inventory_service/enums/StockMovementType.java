
package com.rasahub.product_inventory_service.enums;

public enum StockMovementType {

    GOODS_RECEIPT(true),
    TRANSFER_IN(true),
    TRANSFER_OUT(false),
    DAILY_USAGE(false),
    ADJUSTMENT_IN(true),
    ADJUSTMENT_OUT(false);

    private final boolean increasesStock;

    StockMovementType(boolean increasesStock) {
        this.increasesStock = increasesStock;
    }

    public boolean increasesStock() {
        return increasesStock;
    }
}
