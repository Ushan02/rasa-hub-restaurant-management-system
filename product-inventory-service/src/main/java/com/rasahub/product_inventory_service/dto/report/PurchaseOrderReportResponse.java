package com.rasahub.product_inventory_service.dto.report;

import com.rasahub.product_inventory_service.enums.MeasurementUnit;
import com.rasahub.product_inventory_service.enums.PurchaseOrderStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PurchaseOrderReportResponse(
        Long purchaseOrderId,
        Long purchaseRequestId,
        Long supplierId,
        String supplierCode,
        String supplierName,
        String locationCode,
        String locationName,
        PurchaseOrderStatus status,
        LocalDate expectedDeliveryDate,
        Long inventoryItemId,
        String itemCode,
        String itemName,
        MeasurementUnit measurementUnit,
        BigDecimal orderedQuantity,
        BigDecimal receivedQuantity,
        BigDecimal unitPrice,
        Long createdByAccountId,
        LocalDateTime createdAt
) {
}
