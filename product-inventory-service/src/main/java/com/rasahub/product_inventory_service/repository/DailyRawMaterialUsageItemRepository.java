package com.rasahub.product_inventory_service.repository;

import com.rasahub.product_inventory_service.dto.report.DailyUsageReportResponse;
import com.rasahub.product_inventory_service.entity.DailyRawMaterialUsageItem;
import com.rasahub.product_inventory_service.enums.DailyRawMaterialUsageStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface DailyRawMaterialUsageItemRepository
        extends JpaRepository<DailyRawMaterialUsageItem, Long> {

    @Query(
            value = """
                    SELECT new com.rasahub.product_inventory_service.dto.report.DailyUsageReportResponse(
                        usage.id,
                        usage.usageDate,
                        location.locationCode,
                        location.locationName,
                        item.id,
                        item.itemCode,
                        item.itemName,
                        item.measurementUnit,
                        usageItem.usedQuantity,
                        usage.status,
                        usage.createdByAccountId,
                        usage.postedByAccountId,
                        usage.postedAt
                    )
                    FROM DailyRawMaterialUsageItem usageItem
                    JOIN usageItem.dailyRawMaterialUsage usage
                    JOIN usage.branchLocation location
                    JOIN usageItem.inventoryItem item
                    WHERE usage.status = :status
                      AND (:locationCode IS NULL
                           OR location.locationCode = :locationCode)
                      AND (:inventoryItemId IS NULL OR item.id = :inventoryItemId)
                      AND (:from IS NULL OR usage.usageDate >= :from)
                      AND (:until IS NULL OR usage.usageDate < :until)
                    ORDER BY usage.usageDate DESC, usage.id DESC, item.id ASC
                    """,
            countQuery = """
                    SELECT COUNT(usageItem)
                    FROM DailyRawMaterialUsageItem usageItem
                    JOIN usageItem.dailyRawMaterialUsage usage
                    WHERE usage.status = :status
                      AND (:locationCode IS NULL
                           OR usage.branchLocation.locationCode = :locationCode)
                      AND (:inventoryItemId IS NULL
                           OR usageItem.inventoryItem.id = :inventoryItemId)
                      AND (:from IS NULL OR usage.usageDate >= :from)
                      AND (:until IS NULL OR usage.usageDate < :until)
                    """
    )
    Page<DailyUsageReportResponse> findReportRows(
            @Param("status") DailyRawMaterialUsageStatus status,
            @Param("locationCode") String locationCode,
            @Param("inventoryItemId") Long inventoryItemId,
            @Param("from") LocalDate from,
            @Param("until") LocalDate until,
            Pageable pageable
    );

    @Query("""
            SELECT usage.usageDate AS periodStart,
                   YEAR(usage.usageDate) AS periodYear,
                   MONTH(usage.usageDate) AS periodMonth,
                   item.id AS inventoryItemId,
                   item.itemCode AS itemCode,
                   item.itemName AS itemName,
                   item.measurementUnit AS measurementUnit,
                   SUM(usageItem.usedQuantity) AS quantityUsed
            FROM DailyRawMaterialUsageItem usageItem
            JOIN usageItem.dailyRawMaterialUsage usage
            JOIN usageItem.inventoryItem item
            WHERE usage.status = :status
              AND usage.branchLocation.locationCode = :locationCode
              AND usage.usageDate BETWEEN :startDate AND :endDate
            GROUP BY usage.usageDate, item.id, item.itemCode,
                     item.itemName, item.measurementUnit
            ORDER BY usage.usageDate DESC, item.id ASC
            """)
    List<DailyUsageSummaryProjection> summarizeDaily(
            @Param("status") DailyRawMaterialUsageStatus status,
            @Param("locationCode") String locationCode,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
            SELECT MIN(usage.usageDate) AS periodStart,
                   YEAR(usage.usageDate) AS periodYear,
                   MONTH(usage.usageDate) AS periodMonth,
                   item.id AS inventoryItemId,
                   item.itemCode AS itemCode,
                   item.itemName AS itemName,
                   item.measurementUnit AS measurementUnit,
                   SUM(usageItem.usedQuantity) AS quantityUsed
            FROM DailyRawMaterialUsageItem usageItem
            JOIN usageItem.dailyRawMaterialUsage usage
            JOIN usageItem.inventoryItem item
            WHERE usage.status = :status
              AND usage.branchLocation.locationCode = :locationCode
              AND usage.usageDate BETWEEN :startDate AND :endDate
            GROUP BY YEAR(usage.usageDate), MONTH(usage.usageDate),
                     item.id, item.itemCode, item.itemName, item.measurementUnit
            ORDER BY YEAR(usage.usageDate) DESC, MONTH(usage.usageDate) DESC,
                     item.id ASC
            """)
    List<DailyUsageSummaryProjection> summarizeMonthly(
            @Param("status") DailyRawMaterialUsageStatus status,
            @Param("locationCode") String locationCode,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
