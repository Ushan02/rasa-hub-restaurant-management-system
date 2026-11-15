package com.rasahub.product_inventory_service.dto.report;

import com.rasahub.product_inventory_service.enums.MeasurementUnit;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SupplierPurchaseReportResponse(
        Long supplierId,
        String supplierCode,
        String supplierName,
        Long purchaseOrderId,
        LocalDateTime orderedAt,
        String locationCode,
        Long inventoryItemId,
        String itemCode,
        String itemName,
        MeasurementUnit measurementUnit,
        BigDecimal orderedQuantity,
        BigDecimal acceptedReceiptQuantity
) {
}
