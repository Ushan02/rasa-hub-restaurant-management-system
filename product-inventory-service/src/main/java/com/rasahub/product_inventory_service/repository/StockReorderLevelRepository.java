package com.rasahub.product_inventory_service.repository;

import com.rasahub.product_inventory_service.entity.StockReorderLevel;
import com.rasahub.product_inventory_service.repository.LocationStockStatusProjection;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

public interface StockReorderLevelRepository extends JpaRepository<StockReorderLevel, Long> {

    @EntityGraph(attributePaths = {"stockLocation", "inventoryItem"})
    Optional<StockReorderLevel> findByStockLocation_LocationCodeAndInventoryItem_Id(
            String locationCode,
            Long inventoryItemId
    );

    @EntityGraph(attributePaths = {"stockLocation", "inventoryItem"})
    List<StockReorderLevel> findByStockLocation_LocationCodeAndActiveTrue(
            String locationCode
    );

    @EntityGraph(attributePaths = {"stockLocation", "inventoryItem"})
    List<StockReorderLevel> findByInventoryItem_IdAndActiveTrue(Long inventoryItemId);

    @EntityGraph(attributePaths = {"stockLocation", "inventoryItem"})
    Optional<StockReorderLevel> findByStockLocation_LocationCodeAndInventoryItem_IdAndActiveTrue(
            String locationCode,
            Long inventoryItemId
    );

    @EntityGraph(attributePaths = {"stockLocation", "inventoryItem"})
    List<StockReorderLevel> findByActiveTrue();

    @EntityGraph(attributePaths = {"stockLocation", "inventoryItem"})
    List<StockReorderLevel> findByActiveTrueAndIdGreaterThanOrderByIdAsc(
            Long afterId,
            Pageable pageable
    );

    @Query("""
            SELECT srl.stockLocation.locationCode AS locationCode,
                   SUM(CASE WHEN COALESCE(balance.quantityOnHand, :zero) > 0
                                 AND COALESCE(balance.quantityOnHand, :zero)
                                     <= srl.reorderLevel
                            THEN 1 ELSE 0 END) AS lowStockCount,
                   SUM(CASE WHEN COALESCE(balance.quantityOnHand, :zero) = 0
                            THEN 1 ELSE 0 END) AS outOfStockCount
            FROM StockReorderLevel srl
            LEFT JOIN StockBalance balance
              ON balance.stockLocation = srl.stockLocation
             AND balance.inventoryItem = srl.inventoryItem
            WHERE srl.active = true
            GROUP BY srl.stockLocation.locationCode
            """)
    List<LocationStockStatusProjection> summarizeConfiguredStockStatus(
            @Param("zero") java.math.BigDecimal zero
    );

    boolean existsByStockLocation_LocationCodeAndInventoryItem_Id(
            String locationCode,
            Long inventoryItemId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"stockLocation", "inventoryItem"})
    @Query("""
            SELECT srl
            FROM StockReorderLevel srl
            WHERE srl.stockLocation.locationCode = :locationCode
              AND srl.inventoryItem.id = :inventoryItemId
            """)
    Optional<StockReorderLevel> findForUpdate(
            @Param("locationCode") String locationCode,
            @Param("inventoryItemId") Long inventoryItemId
    );
}
