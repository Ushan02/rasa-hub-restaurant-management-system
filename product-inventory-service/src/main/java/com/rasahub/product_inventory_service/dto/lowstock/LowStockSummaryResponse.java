package com.rasahub.product_inventory_service.dto.lowstock;

public record LowStockSummaryResponse(
        String locationCode,
        long monitoredItemCount,
        long normalCount,
        long lowStockCount,
        long outOfStockCount,
        long activeAlertCount
) {
}
