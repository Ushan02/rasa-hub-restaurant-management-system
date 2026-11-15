package com.rasahub.product_inventory_service.repository;

import com.rasahub.product_inventory_service.enums.MeasurementUnit;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface DailyUsageSummaryProjection {
    LocalDate getPeriodStart();
    Integer getPeriodYear();
    Integer getPeriodMonth();
    Long getInventoryItemId();
    String getItemCode();
    String getItemName();
    MeasurementUnit getMeasurementUnit();
    BigDecimal getQuantityUsed();
}
