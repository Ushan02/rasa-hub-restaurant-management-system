package com.rasahub.product_inventory_service.repository;

import com.rasahub.product_inventory_service.dto.report.LowStockReportRow;
import com.rasahub.product_inventory_service.entity.LowStockAlert;
import com.rasahub.product_inventory_service.enums.AlertStatus;
import com.rasahub.product_inventory_service.repository.LocationCountProjection;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import java.math.BigDecimal;

public interface LowStockAlertRepository extends JpaRepository<LowStockAlert, Long> {

    @Query("""
            SELECT alert.stockLocation.locationCode AS locationCode,
                   COUNT(alert.id) AS recordCount
            FROM LowStockAlert alert
            WHERE alert.status IN :statuses
            GROUP BY alert.stockLocation.locationCode
            """)
    List<LocationCountProjection> countActiveAlertsGroupedByLocation(
            @Param("statuses") Collection<AlertStatus> statuses
    );

    @Query(
            value = """
                    SELECT new com.rasahub.product_inventory_service.dto.report.LowStockReportRow(
                        alert.id,
                        location.locationCode,
                        location.locationName,
                        item.id,
                        item.itemCode,
                        item.itemName,
                        item.measurementUnit,
                        COALESCE(balance.quantityOnHand, :zero),
                        COALESCE(config.reorderLevel, alert.reorderLevelSnapshot),
                        alert.severity,
                        alert.status,
                        alert.detectedAt,
                        alert.resolvedAt,
                        alert.resolutionReason
                    )
                    FROM LowStockAlert alert
                    JOIN alert.stockLocation location
                    JOIN alert.inventoryItem item
                    LEFT JOIN StockBalance balance
                      ON balance.stockLocation = location
                     AND balance.inventoryItem = item
                    LEFT JOIN StockReorderLevel config
                      ON config.stockLocation = location
                     AND config.inventoryItem = item
                     AND config.active = true
                    WHERE (:locationCode IS NULL
                           OR location.locationCode = :locationCode)
                      AND (:inventoryItemId IS NULL OR item.id = :inventoryItemId)
                      AND (:status IS NULL OR alert.status = :status)
                      AND (:from IS NULL OR alert.detectedAt >= :from)
                      AND (:until IS NULL OR alert.detectedAt < :until)
                    ORDER BY alert.detectedAt DESC, alert.id DESC
                    """,
            countQuery = """
                    SELECT COUNT(alert)
                    FROM LowStockAlert alert
                    WHERE (:locationCode IS NULL
                           OR alert.stockLocation.locationCode = :locationCode)
                      AND (:inventoryItemId IS NULL
                           OR alert.inventoryItem.id = :inventoryItemId)
                      AND (:status IS NULL OR alert.status = :status)
                      AND (:from IS NULL OR alert.detectedAt >= :from)
                      AND (:until IS NULL OR alert.detectedAt < :until)
                    """
    )
    Page<LowStockReportRow> findReportRows(
            @Param("locationCode") String locationCode,
            @Param("inventoryItemId") Long inventoryItemId,
            @Param("status") AlertStatus status,
            @Param("from") LocalDateTime from,
            @Param("until") LocalDateTime until,
            @Param("zero") BigDecimal zero,
            Pageable pageable
    );

    @Override
    @EntityGraph(attributePaths = {"stockLocation", "inventoryItem"})
    Optional<LowStockAlert> findById(Long id);

    @EntityGraph(attributePaths = {"stockLocation", "inventoryItem"})
    List<LowStockAlert> findByStockLocation_LocationCodeOrderByDetectedAtDesc(
            String locationCode
    );

    @EntityGraph(attributePaths = {"stockLocation", "inventoryItem"})
    List<LowStockAlert> findByInventoryItem_IdOrderByDetectedAtDesc(
            Long inventoryItemId
    );

    @EntityGraph(attributePaths = {"stockLocation", "inventoryItem"})
    List<LowStockAlert> findByStockLocation_LocationCodeAndInventoryItem_IdOrderByDetectedAtDesc(
            String locationCode,
            Long inventoryItemId
    );

    Optional<LowStockAlert> findTopByStockLocation_LocationCodeAndInventoryItem_IdAndStatusInOrderByDetectedAtDesc(
            String locationCode,
            Long inventoryItemId,
            Collection<AlertStatus> statuses
    );

    boolean existsByStockLocation_LocationCodeAndInventoryItem_IdAndStatusIn(
            String locationCode,
            Long inventoryItemId,
            Collection<AlertStatus> statuses
    );

    long countByStockLocation_LocationCodeAndStatusIn(
            String locationCode,
            Collection<AlertStatus> statuses
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"stockLocation", "inventoryItem"})
    @Query("""
            SELECT la
            FROM LowStockAlert la
            WHERE la.stockLocation.locationCode = :locationCode
              AND la.inventoryItem.id = :inventoryItemId
              AND la.status IN :statuses
            ORDER BY la.detectedAt DESC
            """)
    List<LowStockAlert> findActiveForUpdate(
            @Param("locationCode") String locationCode,
            @Param("inventoryItemId") Long inventoryItemId,
            @Param("statuses") Collection<AlertStatus> statuses
    );
}
