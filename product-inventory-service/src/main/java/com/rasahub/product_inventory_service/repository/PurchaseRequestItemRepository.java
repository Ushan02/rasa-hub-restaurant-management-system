
package com.rasahub.product_inventory_service.repository;

import com.rasahub.product_inventory_service.dto.report.PurchaseRequestReportResponse;
import com.rasahub.product_inventory_service.entity.PurchaseRequestItem;
import com.rasahub.product_inventory_service.enums.PurchaseRequestStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PurchaseRequestItemRepository
        extends JpaRepository<PurchaseRequestItem, Long> {

    @Query(
            value = """
                    SELECT new com.rasahub.product_inventory_service.dto.report.PurchaseRequestReportResponse(
                        request.id,
                        location.locationCode,
                        location.locationName,
                        request.status,
                        item.id,
                        item.itemCode,
                        item.itemName,
                        item.measurementUnit,
                        line.requestedQuantity,
                        request.requestedByAccountId,
                        request.reviewedByAccountId,
                        request.createdAt,
                        request.reviewedAt
                    )
                    FROM PurchaseRequestItem line
                    JOIN line.purchaseRequest request
                    JOIN request.destinationLocation location
                    JOIN line.inventoryItem item
                    WHERE (:locationCode IS NULL OR location.locationCode = :locationCode)
                      AND (:status IS NULL OR request.status = :status)
                      AND (:from IS NULL OR request.createdAt >= :from)
                      AND (:until IS NULL OR request.createdAt < :until)
                    ORDER BY request.createdAt DESC, request.id DESC, item.id ASC
                    """,
            countQuery = """
                    SELECT COUNT(line)
                    FROM PurchaseRequestItem line
                    JOIN line.purchaseRequest request
                    WHERE (:locationCode IS NULL
                           OR request.destinationLocation.locationCode = :locationCode)
                      AND (:status IS NULL OR request.status = :status)
                      AND (:from IS NULL OR request.createdAt >= :from)
                      AND (:until IS NULL OR request.createdAt < :until)
                    """
    )
    Page<PurchaseRequestReportResponse> findReportRows(
            @Param("locationCode") String locationCode,
            @Param("status") PurchaseRequestStatus status,
            @Param("from") LocalDateTime from,
            @Param("until") LocalDateTime until,
            Pageable pageable
    );

    List<PurchaseRequestItem> findByPurchaseRequest_IdOrderByIdAsc(
            Long purchaseRequestId
    );

    boolean existsByPurchaseRequest_IdAndInventoryItem_Id(
            Long purchaseRequestId,
            Long inventoryItemId
    );

    long countByPurchaseRequest_Id(
            Long purchaseRequestId
    );
}
