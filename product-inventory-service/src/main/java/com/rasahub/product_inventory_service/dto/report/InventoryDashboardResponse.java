package com.rasahub.product_inventory_service.dto.report;

import java.time.LocalDate;
import java.util.List;

public record InventoryDashboardResponse(
        LocalDate reportingPeriodStart,
        LocalDate reportingPeriodEnd,
        List<LocationDashboardSummary> locations
) {
    public InventoryDashboardResponse {
        locations = List.copyOf(locations);
    }

    public record LocationDashboardSummary(
            String locationCode,
            String locationName,
            long activeInventoryItemCount,
            long lowStockItemCount,
            long outOfStockItemCount,
            long activeAlertCount,
            long pendingAdjustmentCount,
            long pendingStockCountApprovalCount,
            long pendingBranchStockRequestCount,
            long completedTransferCount
    ) {
    }
}
