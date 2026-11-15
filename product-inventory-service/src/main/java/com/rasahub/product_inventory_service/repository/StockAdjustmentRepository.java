package com.rasahub.product_inventory_service.repository;

import com.rasahub.product_inventory_service.entity.StockAdjustment;
import com.rasahub.product_inventory_service.enums.StockAdjustmentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface StockAdjustmentRepository
        extends JpaRepository<StockAdjustment, Long> {

    @EntityGraph(attributePaths = "stockLocation")
    Page<StockAdjustment> findAllByOrderByCreatedAtDescIdDesc(
            Pageable pageable
    );

    @EntityGraph(attributePaths = "stockLocation")
    Page<StockAdjustment>
    findByStockLocation_LocationCodeOrderByCreatedAtDescIdDesc(
            String locationCode,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "stockLocation")
    Page<StockAdjustment> findByStatusOrderByCreatedAtDescIdDesc(
            StockAdjustmentStatus status,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "stockLocation")
    Page<StockAdjustment>
    findByStockLocation_LocationCodeAndStatusOrderByCreatedAtDescIdDesc(
            String locationCode,
            StockAdjustmentStatus status,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "stockLocation")
    Page<StockAdjustment>
    findByStockLocation_LocationCodeAndCreatedByAccountIdOrderByCreatedAtDescIdDesc(
            String locationCode,
            Long createdByAccountId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "stockLocation")
    Page<StockAdjustment>
    findByCreatedByAccountIdOrderByCreatedAtDescIdDesc(
            Long createdByAccountId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "stockLocation")
    @Query("""
            SELECT adjustment
            FROM StockAdjustment adjustment
            WHERE adjustment.createdAt >= :from
              AND adjustment.createdAt < :until
              AND (:locationCode IS NULL
                   OR adjustment.stockLocation.locationCode = :locationCode)
              AND (:status IS NULL OR adjustment.status = :status)
            ORDER BY adjustment.createdAt DESC, adjustment.id DESC
            """)
    Page<StockAdjustment> findByFilters(
            @Param("from") LocalDateTime from,
            @Param("until") LocalDateTime until,
            @Param("locationCode") String locationCode,
            @Param("status") StockAdjustmentStatus status,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "stockLocation",
            "items",
            "items.inventoryItem"
    })
    @Query("""
            SELECT adjustment
            FROM StockAdjustment adjustment
            WHERE adjustment.id = :id
            """)
    Optional<StockAdjustment> findWithItemsById(
            @Param("id") Long id
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT adjustment
            FROM StockAdjustment adjustment
            WHERE adjustment.id = :id
            """)
    Optional<StockAdjustment> findByIdForUpdate(
            @Param("id") Long id
    );
}
