package com.rasahub.product_inventory_service.dto.report;

import com.rasahub.product_inventory_service.enums.BranchStockRequestStatus;
import com.rasahub.product_inventory_service.enums.MeasurementUnit;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BranchStockRequestReportResponse(
        Long requestId,
        String branchCode,
        String branchName,
        BranchStockRequestStatus status,
        Long inventoryItemId,
        String itemCode,
        String itemName,
        MeasurementUnit measurementUnit,
        BigDecimal requestedQuantity,
        BigDecimal receivedQuantity,
        Long requestedByAccountId,
        Long reviewedByAccountId,
        LocalDateTime createdAt,
        LocalDateTime reviewedAt
) {
}
