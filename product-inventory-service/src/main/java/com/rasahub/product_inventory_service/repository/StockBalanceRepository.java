
package com.rasahub.product_inventory_service.repository;

import com.rasahub.product_inventory_service.entity.StockBalance;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface StockBalanceRepository
        extends JpaRepository<StockBalance, Long> {

    @Query(
            value = """
                    SELECT sl.location_code AS "locationCode",
                           sl.location_name AS "locationName",
                           i.id AS "inventoryItemId",
                           i.item_code AS "itemCode",
                           i.item_name AS "itemName",
                           i.measurement_unit AS "measurementUnit",
                           sb.quantity_on_hand AS "currentQuantity",
                           srl.reorder_level AS "reorderLevel",
                           sb.updated_at AS "lastStockUpdateAt"
                    FROM stock_balances sb
                    JOIN stock_locations sl ON sl.location_code = sb.location_code
                    JOIN inventory_items i ON i.id = sb.inventory_item_id
                    LEFT JOIN stock_reorder_levels srl
                      ON srl.location_code = sb.location_code
                     AND srl.inventory_item_id = sb.inventory_item_id
                     AND srl.active = true
                    WHERE (CAST(:locationCode AS varchar) IS NULL
                           OR sl.location_code = :locationCode)
                      AND (CAST(:inventoryItemId AS bigint) IS NULL
                           OR i.id = :inventoryItemId)
                    UNION ALL
                    SELECT sl.location_code AS "locationCode",
                           sl.location_name AS "locationName",
                           i.id AS "inventoryItemId",
                           i.item_code AS "itemCode",
                           i.item_name AS "itemName",
                           i.measurement_unit AS "measurementUnit",
                           CAST(0 AS decimal(18, 3)) AS "currentQuantity",
                           srl.reorder_level AS "reorderLevel",
                           CAST(NULL AS timestamp) AS "lastStockUpdateAt"
                    FROM stock_reorder_levels srl
                    JOIN stock_locations sl ON sl.location_code = srl.location_code
                    JOIN inventory_items i ON i.id = srl.inventory_item_id
                    LEFT JOIN stock_balances sb
                      ON sb.location_code = srl.location_code
                     AND sb.inventory_item_id = srl.inventory_item_id
                    WHERE srl.active = true
                      AND sb.id IS NULL
                      AND (CAST(:locationCode AS varchar) IS NULL
                           OR sl.location_code = :locationCode)
                      AND (CAST(:inventoryItemId AS bigint) IS NULL
                           OR i.id = :inventoryItemId)
                    ORDER BY "locationCode", "itemCode"
                    """,
            countQuery = """
                    SELECT COALESCE(SUM(report_count), 0)
                    FROM (
                        SELECT COUNT(*) AS report_count
                        FROM stock_balances sb
                        JOIN stock_locations sl ON sl.location_code = sb.location_code
                        JOIN inventory_items i ON i.id = sb.inventory_item_id
                        WHERE (CAST(:locationCode AS varchar) IS NULL
                               OR sl.location_code = :locationCode)
                          AND (CAST(:inventoryItemId AS bigint) IS NULL
                               OR i.id = :inventoryItemId)
                        UNION ALL
                        SELECT COUNT(*) AS report_count
                        FROM stock_reorder_levels srl
                        JOIN stock_locations sl ON sl.location_code = srl.location_code
                        JOIN inventory_items i ON i.id = srl.inventory_item_id
                        LEFT JOIN stock_balances sb
                          ON sb.location_code = srl.location_code
                         AND sb.inventory_item_id = srl.inventory_item_id
                        WHERE srl.active = true
                          AND sb.id IS NULL
                          AND (CAST(:locationCode AS varchar) IS NULL
                               OR sl.location_code = :locationCode)
                          AND (CAST(:inventoryItemId AS bigint) IS NULL
                               OR i.id = :inventoryItemId)
                    ) report_counts
                    """,
            nativeQuery = true
    )
    Page<StockBalanceReportProjection> findStockBalanceReportRows(
            @Param("locationCode") String locationCode,
            @Param("inventoryItemId") Long inventoryItemId,
            Pageable pageable
    );

    Optional<StockBalance> findByStockLocation_LocationCodeAndInventoryItem_Id(
            String locationCode,
            Long inventoryItemId
    );

    List<StockBalance> findByStockLocation_LocationCode(
            String locationCode
    );

    List<StockBalance> findByInventoryItem_Id(
            Long inventoryItemId
    );

    boolean existsByStockLocation_LocationCodeAndInventoryItem_Id(
            String locationCode,
            Long inventoryItemId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT sb
            FROM StockBalance sb
            WHERE sb.stockLocation.locationCode = :locationCode
              AND sb.inventoryItem.id = :inventoryItemId
            """)
    Optional<StockBalance> findForUpdate(
            @Param("locationCode") String locationCode,
            @Param("inventoryItemId") Long inventoryItemId
    );
}
