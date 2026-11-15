package com.rasahub.product_inventory_service.dto.report;

import com.rasahub.product_inventory_service.enums.AlertResolutionReason;
import com.rasahub.product_inventory_service.enums.AlertSeverity;
import com.rasahub.product_inventory_service.enums.AlertStatus;
import com.rasahub.product_inventory_service.enums.MeasurementUnit;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LowStockReportRow(
        Long alertId,
        String locationCode,
        String locationName,
        Long inventoryItemId,
        String itemCode,
        String itemName,
        MeasurementUnit measurementUnit,
        BigDecimal currentQuantity,
        BigDecimal reorderLevel,
        AlertSeverity severity,
        AlertStatus alertStatus,
        LocalDateTime detectedAt,
        LocalDateTime resolvedAt,
        AlertResolutionReason resolutionReason
) {
}
