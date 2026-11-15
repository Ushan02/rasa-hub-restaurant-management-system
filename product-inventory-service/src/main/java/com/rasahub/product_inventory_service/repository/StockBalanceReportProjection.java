package com.rasahub.product_inventory_service.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface StockBalanceReportProjection {
    String getLocationCode();
    String getLocationName();
    Long getInventoryItemId();
    String getItemCode();
    String getItemName();
    String getMeasurementUnit();
    BigDecimal getCurrentQuantity();
    BigDecimal getReorderLevel();
    LocalDateTime getLastStockUpdateAt();
}
