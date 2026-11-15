package com.rasahub.product_inventory_service.repository;

import com.rasahub.product_inventory_service.dto.report.StockCountReportResponse;
import com.rasahub.product_inventory_service.entity.StockCountItem;
import com.rasahub.product_inventory_service.enums.StockCountStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface StockCountItemRepository
        extends JpaRepository<StockCountItem, Long> {

    @Query(
            value = """
                    SELECT new com.rasahub.product_inventory_service.dto.report.StockCountReportResponse(
                        stockCount.id,
                        location.locationCode,
                        location.locationName,
                        stockCount.countDate,
                        stockCount.status,
                        item.id,
                        item.itemCode,
                        item.itemName,
                        item.measurementUnit,
                        line.systemQuantity,
                        line.physicalQuantity,
                        CASE WHEN line.physicalQuantity IS NULL THEN NULL
                             ELSE line.physicalQuantity - line.systemQuantity END,
                        stockCount.createdByAccountId,
                        stockCount.submittedByAccountId,
                        stockCount.submittedAt,
                        stockCount.reviewedByAccountId,
                        stockCount.reviewedAt,
                        stockCount.reconciledByAccountId,
                        stockCount.reconciledAt,
                        stockCount.rejectionReason
                    )
                    FROM StockCountItem line
                    JOIN line.stockCount stockCount
                    JOIN stockCount.stockLocation location
                    JOIN line.inventoryItem item
                    WHERE (:locationCode IS NULL
                           OR location.locationCode = :locationCode)
                      AND (:status IS NULL OR stockCount.status = :status)
                      AND (:inventoryItemId IS NULL OR item.id = :inventoryItemId)
                      AND (:startDate IS NULL OR stockCount.countDate >= :startDate)
                      AND (:endDate IS NULL OR stockCount.countDate <= :endDate)
                      AND (:variancesOnly = false
                           OR (line.physicalQuantity IS NOT NULL
                               AND line.physicalQuantity <> line.systemQuantity))
                    ORDER BY stockCount.countDate DESC, stockCount.id DESC, item.id ASC
                    """,
            countQuery = """
                    SELECT COUNT(line)
                    FROM StockCountItem line
                    JOIN line.stockCount stockCount
                    WHERE (:locationCode IS NULL
                           OR stockCount.stockLocation.locationCode = :locationCode)
                      AND (:status IS NULL OR stockCount.status = :status)
                      AND (:inventoryItemId IS NULL
                           OR line.inventoryItem.id = :inventoryItemId)
                      AND (:startDate IS NULL OR stockCount.countDate >= :startDate)
                      AND (:endDate IS NULL OR stockCount.countDate <= :endDate)
                      AND (:variancesOnly = false
                           OR (line.physicalQuantity IS NOT NULL
                               AND line.physicalQuantity <> line.systemQuantity))
                    """
    )
    Page<StockCountReportResponse> findReportRows(
            @Param("locationCode") String locationCode,
            @Param("status") StockCountStatus status,
            @Param("inventoryItemId") Long inventoryItemId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("variancesOnly") boolean variancesOnly,
            Pageable pageable
    );
}
