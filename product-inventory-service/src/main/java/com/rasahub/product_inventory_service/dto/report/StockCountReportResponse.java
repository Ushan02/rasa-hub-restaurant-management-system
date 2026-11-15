package com.rasahub.product_inventory_service.dto.report;

import com.rasahub.product_inventory_service.enums.MeasurementUnit;
import com.rasahub.product_inventory_service.enums.StockCountStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record StockCountReportResponse(
        Long countId,
        String locationCode,
        String locationName,
        LocalDate countDate,
        StockCountStatus status,
        Long inventoryItemId,
        String itemCode,
        String itemName,
        MeasurementUnit measurementUnit,
        BigDecimal systemQuantitySnapshot,
        BigDecimal physicalQuantity,
        BigDecimal varianceQuantity,
        Long createdByAccountId,
        Long submittedByAccountId,
        LocalDateTime submittedAt,
        Long reviewedByAccountId,
        LocalDateTime reviewedAt,
        Long reconciledByAccountId,
        LocalDateTime reconciledAt,
        String rejectionReason
) {
}
