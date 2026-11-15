
package com.rasahub.product_inventory_service.repository;

import com.rasahub.product_inventory_service.dto.report.BranchStockRequestReportResponse;
import com.rasahub.product_inventory_service.entity.BranchStockRequestItem;
import com.rasahub.product_inventory_service.enums.BranchStockRequestStatus;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BranchStockRequestItemRepository
        extends JpaRepository<BranchStockRequestItem, Long> {

    @Query(
            value = """
                    SELECT new com.rasahub.product_inventory_service.dto.report.BranchStockRequestReportResponse(
                        request.id,
                        location.locationCode,
                        location.locationName,
                        request.status,
                        item.id,
                        item.itemCode,
                        item.itemName,
                        item.measurementUnit,
                        line.requestedQuantity,
                        line.receivedQuantity,
                        request.requestedByAccountId,
                        request.reviewedByAccountId,
                        request.createdAt,
                        request.reviewedAt
                    )
                    FROM BranchStockRequestItem line
                    JOIN line.branchStockRequest request
                    JOIN request.branchLocation location
                    JOIN line.inventoryItem item
                    WHERE (:locationCode IS NULL OR location.locationCode = :locationCode)
                      AND (:status IS NULL OR request.status = :status)
                      AND (:from IS NULL OR request.createdAt >= :from)
                      AND (:until IS NULL OR request.createdAt < :until)
                    ORDER BY request.createdAt DESC, request.id DESC, item.id ASC
                    """,
            countQuery = """
                    SELECT COUNT(line)
                    FROM BranchStockRequestItem line
                    JOIN line.branchStockRequest request
                    WHERE (:locationCode IS NULL
                           OR request.branchLocation.locationCode = :locationCode)
                      AND (:status IS NULL OR request.status = :status)
                      AND (:from IS NULL OR request.createdAt >= :from)
                      AND (:until IS NULL OR request.createdAt < :until)
                    """
    )
    Page<BranchStockRequestReportResponse> findReportRows(
            @Param("locationCode") String locationCode,
            @Param("status") BranchStockRequestStatus status,
            @Param("from") LocalDateTime from,
            @Param("until") LocalDateTime until,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "inventoryItem")
    List<BranchStockRequestItem>
    findByBranchStockRequest_IdOrderByIdAsc(
            Long branchStockRequestId
    );

    boolean existsByBranchStockRequest_IdAndInventoryItem_Id(
            Long branchStockRequestId,
            Long inventoryItemId
    );

    long countByBranchStockRequest_Id(
            Long branchStockRequestId
    );
}
