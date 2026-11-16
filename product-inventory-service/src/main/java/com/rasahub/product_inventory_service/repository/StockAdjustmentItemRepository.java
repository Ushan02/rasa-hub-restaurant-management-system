
package com.rasahub.product_inventory_service.repository;

import com.rasahub.product_inventory_service.dto.report.StockAdjustmentReportResponse;
import com.rasahub.product_inventory_service.entity.StockAdjustmentItem;
import com.rasahub.product_inventory_service.enums.StockAdjustmentReason;
import com.rasahub.product_inventory_service.enums.StockAdjustmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface StockAdjustmentItemRepository
        extends JpaRepository<StockAdjustmentItem, Long> {

    @Query(
            value = """
                    SELECT new com.rasahub.product_inventory_service.dto.report.StockAdjustmentReportResponse(
                        adjustment.id,
                        location.locationCode,
                        location.locationName,
                        item.id,
                        item.itemCode,
                        item.itemName,
                        item.measurementUnit,
                        adjustment.adjustmentReason,
                        adjustment.direction,
                        adjustment.status,
                        line.adjustmentQuantity,
                        adjustment.createdByAccountId,
                        adjustment.submittedByAccountId,
                        adjustment.reviewedByAccountId,
                        adjustment.postedByAccountId,
                        adjustment.createdAt,
                        adjustment.submittedAt,
                        adjustment.reviewedAt,
                        adjustment.postedAt,
                        sourceCount.id,
                        adjustment.remarks
                    )
                    FROM StockAdjustmentItem line
                    JOIN line.stockAdjustment adjustment
                    JOIN adjustment.stockLocation location
                    JOIN line.inventoryItem item
                    LEFT JOIN adjustment.sourceStockCount sourceCount
                    WHERE (:locationCode IS NULL
                           OR location.locationCode = :locationCode)
                      AND (:reason IS NULL OR adjustment.adjustmentReason = :reason)
                      AND (:status IS NULL OR adjustment.status = :status)
                      AND (:inventoryItemId IS NULL OR item.id = :inventoryItemId)
                      AND (:from IS NULL OR adjustment.createdAt >= :from)
                      AND (:until IS NULL OR adjustment.createdAt < :until)
                    ORDER BY adjustment.createdAt DESC, adjustment.id DESC, item.id ASC
                    """,
            countQuery = """
                    SELECT COUNT(line)
                    FROM StockAdjustmentItem line
                    JOIN line.stockAdjustment adjustment
                    WHERE (:locationCode IS NULL
                           OR adjustment.stockLocation.locationCode = :locationCode)
                      AND (:reason IS NULL OR adjustment.adjustmentReason = :reason)
                      AND (:status IS NULL OR adjustment.status = :status)
                      AND (:inventoryItemId IS NULL
                           OR line.inventoryItem.id = :inventoryItemId)
                      AND (:from IS NULL OR adjustment.createdAt >= :from)
                      AND (:until IS NULL OR adjustment.createdAt < :until)
                    """
    )
    Page<StockAdjustmentReportResponse> findReportRows(
            @Param("locationCode") String locationCode,
            @Param("reason") StockAdjustmentReason reason,
            @Param("status") StockAdjustmentStatus status,
            @Param("inventoryItemId") Long inventoryItemId,
            @Param("from") LocalDateTime from,
            @Param("until") LocalDateTime until,
            Pageable pageable
    );
}
