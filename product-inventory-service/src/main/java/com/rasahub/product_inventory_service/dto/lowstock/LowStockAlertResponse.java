package com.rasahub.product_inventory_service.dto.lowstock;

import com.rasahub.product_inventory_service.entity.LowStockAlert;
import com.rasahub.product_inventory_service.enums.AlertResolutionReason;
import com.rasahub.product_inventory_service.enums.AlertSeverity;
import com.rasahub.product_inventory_service.enums.AlertStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LowStockAlertResponse(
        Long id,
        String locationCode,
        Long inventoryItemId,
        String itemName,
        BigDecimal currentQuantity,
        BigDecimal reorderLevel,
        AlertStatus status,
        AlertSeverity severity,
        AlertResolutionReason resolutionReason,
        LocalDateTime detectedAt,
        Long acknowledgedByAccountId,
        LocalDateTime acknowledgedAt,
        LocalDateTime resolvedAt,
        LocalDateTime lastEvaluatedAt,
        String remarks
) {
    public static LowStockAlertResponse from(LowStockAlert alert) {
        return new LowStockAlertResponse(
                alert.getId(),
                alert.getStockLocation().getLocationCode(),
                alert.getInventoryItem().getId(),
                alert.getInventoryItem().getItemName(),
                alert.getCurrentQuantitySnapshot(),
                alert.getReorderLevelSnapshot(),
                alert.getStatus(),
                alert.getSeverity(),
                alert.getResolutionReason(),
                alert.getDetectedAt(),
                alert.getAcknowledgedByAccountId(),
                alert.getAcknowledgedAt(),
                alert.getResolvedAt(),
                alert.getLastEvaluatedAt(),
                alert.getRemarks()
        );
    }
}
