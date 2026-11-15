package com.rasahub.product_inventory_service.repository;

import com.rasahub.product_inventory_service.entity.StockCount;
import com.rasahub.product_inventory_service.enums.StockCountStatus;
import com.rasahub.product_inventory_service.repository.LocationCountProjection;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;
import java.util.List;

public interface StockCountRepository
        extends JpaRepository<StockCount, Long> {

    @Query("""
            SELECT stockCount.stockLocation.locationCode AS locationCode,
                   COUNT(stockCount.id) AS recordCount
            FROM StockCount stockCount
            WHERE stockCount.status = :status
            GROUP BY stockCount.stockLocation.locationCode
            """)
    List<LocationCountProjection> countByStatusGroupedByLocation(
            @Param("status") StockCountStatus status
    );

    @EntityGraph(attributePaths = "stockLocation")
    Page<StockCount> findAllByOrderByCountDateDescCreatedAtDescIdDesc(
            Pageable pageable
    );

    @EntityGraph(attributePaths = "stockLocation")
    Page<StockCount> findByStockLocation_LocationCodeOrderByCountDateDescCreatedAtDescIdDesc(
            String locationCode,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "stockLocation")
    Page<StockCount> findByStatusOrderByCountDateDescCreatedAtDescIdDesc(
            StockCountStatus status,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "stockLocation")
    Page<StockCount> findByStockLocation_LocationCodeAndStatusOrderByCountDateDescCreatedAtDescIdDesc(
            String locationCode,
            StockCountStatus status,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "stockLocation")
    @Query("""
            SELECT stockCount
            FROM StockCount stockCount
            WHERE stockCount.countDate BETWEEN :startDate AND :endDate
              AND (:locationCode IS NULL
                   OR stockCount.stockLocation.locationCode = :locationCode)
              AND (:status IS NULL OR stockCount.status = :status)
            ORDER BY stockCount.countDate DESC,
                     stockCount.createdAt DESC,
                     stockCount.id DESC
            """)
    Page<StockCount> findByFilters(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("locationCode") String locationCode,
            @Param("status") StockCountStatus status,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "stockLocation",
            "items",
            "items.inventoryItem"
    })
    @Query("""
            SELECT stockCount
            FROM StockCount stockCount
            WHERE stockCount.id = :id
            """)
    Optional<StockCount> findWithItemsById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {
            "stockLocation",
            "items",
            "items.inventoryItem"
    })
    @Query("""
            SELECT stockCount
            FROM StockCount stockCount
            WHERE stockCount.id = :id
            """)
    Optional<StockCount> findByIdForUpdate(@Param("id") Long id);
}
