package com.rasahub.product_inventory_service.dto.report;

import com.rasahub.product_inventory_service.enums.MeasurementUnit;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyUsageSummaryResponse(
        LocalDate periodStart,
        Integer periodYear,
        Integer periodMonth,
        Long inventoryItemId,
        String itemCode,
        String itemName,
        MeasurementUnit measurementUnit,
        BigDecimal quantityUsed
) {
}
