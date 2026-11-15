package com.rasahub.product_inventory_service.dto.report;

import com.rasahub.product_inventory_service.enums.DailyRawMaterialUsageStatus;
import com.rasahub.product_inventory_service.enums.MeasurementUnit;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record DailyUsageReportResponse(
        Long usageId,
        LocalDate usageDate,
        String branchCode,
        String branchName,
        Long inventoryItemId,
        String itemCode,
        String itemName,
        MeasurementUnit measurementUnit,
        BigDecimal quantityUsed,
        DailyRawMaterialUsageStatus status,
        Long createdByAccountId,
        Long postedByAccountId,
        LocalDateTime postedAt
) {
}
