package com.rasahub.product_inventory_service.dto.report;

import com.rasahub.product_inventory_service.enums.GoodsReceiptStatus;
import com.rasahub.product_inventory_service.enums.MeasurementUnit;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record GoodsReceivedReportResponse(
        Long goodsReceiptId,
        Long purchaseOrderId,
        Long supplierId,
        String supplierName,
        String locationCode,
        String locationName,
        GoodsReceiptStatus status,
        LocalDate receivedDate,
        Long inventoryItemId,
        String itemCode,
        String itemName,
        MeasurementUnit measurementUnit,
        BigDecimal deliveredQuantity,
        BigDecimal acceptedQuantity,
        BigDecimal rejectedQuantity,
        Long receivedByAccountId,
        Long reviewedByAccountId,
        LocalDateTime reviewedAt,
        LocalDateTime createdAt
) {
}
