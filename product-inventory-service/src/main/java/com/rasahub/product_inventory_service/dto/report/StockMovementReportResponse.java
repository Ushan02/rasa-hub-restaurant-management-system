package com.rasahub.product_inventory_service.dto.report;

import com.rasahub.product_inventory_service.enums.MeasurementUnit;
import com.rasahub.product_inventory_service.enums.StockMovementType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StockMovementReportResponse(
        Long movementId,
        Long inventoryItemId,
        String itemCode,
        String itemName,
        MeasurementUnit measurementUnit,
        String locationCode,
        String locationName,
        StockMovementType movementType,
        BigDecimal quantity,
        BigDecimal previousBalance,
        BigDecimal newBalance,
        String referenceType,
        String referenceId,
        Long accountId,
        LocalDateTime movementAt,
        String remarks
) {
}
