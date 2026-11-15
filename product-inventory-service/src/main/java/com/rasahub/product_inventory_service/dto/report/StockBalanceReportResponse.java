package com.rasahub.product_inventory_service.dto.report;

import com.rasahub.product_inventory_service.enums.MeasurementUnit;
import com.rasahub.product_inventory_service.enums.StockStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StockBalanceReportResponse(
        String locationCode,
        String locationName,
        Long inventoryItemId,
        String itemCode,
        String itemName,
        MeasurementUnit measurementUnit,
        BigDecimal currentQuantity,
        BigDecimal reorderLevel,
        StockStatus stockStatus,
        LocalDateTime lastStockUpdateAt
) {
}
