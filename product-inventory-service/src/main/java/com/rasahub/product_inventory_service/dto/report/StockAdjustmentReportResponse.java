package com.rasahub.product_inventory_service.dto.report;

import com.rasahub.product_inventory_service.enums.StockAdjustmentDirection;
import com.rasahub.product_inventory_service.enums.StockAdjustmentReason;
import com.rasahub.product_inventory_service.enums.StockAdjustmentStatus;
import com.rasahub.product_inventory_service.enums.MeasurementUnit;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StockAdjustmentReportResponse(
        Long adjustmentId,
        String locationCode,
        String locationName,
        Long inventoryItemId,
        String itemCode,
        String itemName,
        MeasurementUnit measurementUnit,
        StockAdjustmentReason adjustmentReason,
        StockAdjustmentDirection direction,
        StockAdjustmentStatus status,
        BigDecimal quantity,
        Long createdByAccountId,
        Long submittedByAccountId,
        Long reviewedByAccountId,
        Long postedByAccountId,
        LocalDateTime createdAt,
        LocalDateTime submittedAt,
        LocalDateTime reviewedAt,
        LocalDateTime postedAt,
        Long sourceStockCountId,
        String remarks
) {
}
