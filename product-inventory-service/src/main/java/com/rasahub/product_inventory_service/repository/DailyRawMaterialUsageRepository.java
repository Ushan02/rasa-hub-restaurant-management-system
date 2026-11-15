
package com.rasahub.product_inventory_service.repository;

import com.rasahub.product_inventory_service.entity.DailyRawMaterialUsage;
import com.rasahub.product_inventory_service.enums.DailyRawMaterialUsageStatus;

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

public interface DailyRawMaterialUsageRepository
        extends JpaRepository<DailyRawMaterialUsage, Long> {

    @EntityGraph(attributePaths = "branchLocation")
    Page<DailyRawMaterialUsage> findAllByOrderByUsageDateDescIdDesc(
            Pageable pageable
    );

    @EntityGraph(attributePaths = "branchLocation")
    Page<DailyRawMaterialUsage>
    findByBranchLocation_LocationCodeOrderByUsageDateDescIdDesc(
            String branchLocationCode,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "branchLocation")
    Page<DailyRawMaterialUsage>
    findByStatusOrderByUsageDateDescIdDesc(
            DailyRawMaterialUsageStatus status,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "branchLocation")
    Page<DailyRawMaterialUsage>
    findByBranchLocation_LocationCodeAndStatusOrderByUsageDateDescIdDesc(
            String branchLocationCode,
            DailyRawMaterialUsageStatus status,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "branchLocation")
    Page<DailyRawMaterialUsage>
    findByBranchLocation_LocationCodeAndUsageDateBetweenOrderByUsageDateDescIdDesc(
            String branchLocationCode,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "branchLocation")
    Page<DailyRawMaterialUsage>
    findByCreatedByAccountIdOrderByUsageDateDescIdDesc(
            Long createdByAccountId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "branchLocation",
            "items",
            "items.inventoryItem"
    })
    @Query("""
            SELECT usage
            FROM DailyRawMaterialUsage usage
            WHERE usage.id = :id
            """)
    Optional<DailyRawMaterialUsage> findWithItemsById(
            @Param("id") Long id
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT usage
            FROM DailyRawMaterialUsage usage
            WHERE usage.id = :id
            """)
    Optional<DailyRawMaterialUsage> findByIdForUpdate(
            @Param("id") Long id
    );
}
