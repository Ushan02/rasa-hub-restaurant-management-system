
package com.rasahub.product_inventory_service.repository;

import com.rasahub.product_inventory_service.dto.report.StockMovementReportResponse;
import com.rasahub.product_inventory_service.entity.StockMovement;
import com.rasahub.product_inventory_service.enums.StockMovementType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface StockMovementRepository
        extends JpaRepository<StockMovement, Long> {

    @Query(
            value = """
                    SELECT new com.rasahub.product_inventory_service.dto.report.StockMovementReportResponse(
                        movement.id,
                        item.id,
                        item.itemCode,
                        item.itemName,
                        item.measurementUnit,
                        location.locationCode,
                        location.locationName,
                        movement.movementType,
                        movement.quantity,
                        movement.quantityBefore,
                        movement.quantityAfter,
                        movement.referenceType,
                        movement.referenceId,
                        movement.performedByAccountId,
                        movement.createdAt,
                        movement.remarks
                    )
                    FROM StockMovement movement
                    JOIN movement.inventoryItem item
                    JOIN movement.stockLocation location
                    WHERE (:locationCode IS NULL OR location.locationCode = :locationCode)
                      AND (:inventoryItemId IS NULL OR item.id = :inventoryItemId)
                      AND (:movementType IS NULL OR movement.movementType = :movementType)
                      AND (:from IS NULL OR movement.createdAt >= :from)
                      AND (:until IS NULL OR movement.createdAt < :until)
                    ORDER BY movement.createdAt DESC, movement.id DESC
                    """,
            countQuery = """
                    SELECT COUNT(movement)
                    FROM StockMovement movement
                    WHERE (:locationCode IS NULL
                           OR movement.stockLocation.locationCode = :locationCode)
                      AND (:inventoryItemId IS NULL
                           OR movement.inventoryItem.id = :inventoryItemId)
                      AND (:movementType IS NULL OR movement.movementType = :movementType)
                      AND (:from IS NULL OR movement.createdAt >= :from)
                      AND (:until IS NULL OR movement.createdAt < :until)
                    """
    )
    Page<StockMovementReportResponse> findReportRows(
            @Param("locationCode") String locationCode,
            @Param("inventoryItemId") Long inventoryItemId,
            @Param("movementType") StockMovementType movementType,
            @Param("from") LocalDateTime from,
            @Param("until") LocalDateTime until,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "stockLocation",
            "inventoryItem"
    })
    Page<StockMovement>
    findByStockLocation_LocationCodeOrderByCreatedAtDescIdDesc(
            String locationCode,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "stockLocation",
            "inventoryItem"
    })
    Page<StockMovement>
    findByInventoryItem_IdOrderByCreatedAtDescIdDesc(
            Long inventoryItemId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "stockLocation",
            "inventoryItem"
    })
    Page<StockMovement>
    findByStockLocation_LocationCodeAndInventoryItem_IdOrderByCreatedAtDescIdDesc(
            String locationCode,
            Long inventoryItemId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "stockLocation",
            "inventoryItem"
    })
    List<StockMovement>
    findByReferenceTypeAndReferenceIdOrderByIdAsc(
            String referenceType,
            String referenceId
    );

    @EntityGraph(attributePaths = {
            "stockLocation",
            "inventoryItem"
    })
    Page<StockMovement>
    findByMovementTypeOrderByCreatedAtDescIdDesc(
            StockMovementType movementType,
            Pageable pageable
    );
}
