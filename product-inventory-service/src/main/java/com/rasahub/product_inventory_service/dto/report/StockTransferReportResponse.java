package com.rasahub.product_inventory_service.dto.report;

import com.rasahub.product_inventory_service.enums.MeasurementUnit;
import com.rasahub.product_inventory_service.enums.StockTransferStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StockTransferReportResponse(
        Long transferId,
        Long branchStockRequestId,
        String sourceLocationCode,
        String sourceLocationName,
        String destinationLocationCode,
        String destinationLocationName,
        StockTransferStatus status,
        Long inventoryItemId,
        String itemCode,
        String itemName,
        MeasurementUnit measurementUnit,
        BigDecimal transferQuantity,
        Long createdByAccountId,
        Long dispatchedByAccountId,
        LocalDateTime dispatchedAt,
        Long receivedByAccountId,
        LocalDateTime receivedAt,
        LocalDateTime createdAt
) {
}
