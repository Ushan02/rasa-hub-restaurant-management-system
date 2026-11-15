package com.rasahub.product_inventory_service.dto.report;

import com.rasahub.product_inventory_service.enums.MeasurementUnit;
import com.rasahub.product_inventory_service.enums.PurchaseRequestStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PurchaseRequestReportResponse(
        Long purchaseRequestId,
        String locationCode,
        String locationName,
        PurchaseRequestStatus status,
        Long inventoryItemId,
        String itemCode,
        String itemName,
        MeasurementUnit measurementUnit,
        BigDecimal requestedQuantity,
        Long requestedByAccountId,
        Long reviewedByAccountId,
        LocalDateTime createdAt,
        LocalDateTime reviewedAt
) {
}
