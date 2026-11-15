package com.rasahub.product_inventory_service.repository;

public interface LocationStockStatusProjection {
    String getLocationCode();
    Long getLowStockCount();
    Long getOutOfStockCount();
}
