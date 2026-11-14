
package com.rasahub.product_inventory_service.repository;

import com.rasahub.product_inventory_service.entity.BranchStockRequest;
import com.rasahub.product_inventory_service.enums.BranchStockRequestStatus;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BranchStockRequestRepository
        extends JpaRepository<BranchStockRequest, Long> {

    @EntityGraph(attributePaths = "branchLocation")
    Page<BranchStockRequest> findAllByOrderByCreatedAtDescIdDesc(
            Pageable pageable
    );

    @EntityGraph(attributePaths = "branchLocation")
    Page<BranchStockRequest> findByStatusOrderByCreatedAtDescIdDesc(
            BranchStockRequestStatus status,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "branchLocation")
    Page<BranchStockRequest>
    findByBranchLocation_LocationCodeOrderByCreatedAtDescIdDesc(
            String branchLocationCode,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "branchLocation")
    Page<BranchStockRequest>
    findByRequestedByAccountIdOrderByCreatedAtDescIdDesc(
            Long requestedByAccountId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "branchLocation")
    Page<BranchStockRequest>
    findByBranchLocation_LocationCodeAndStatusOrderByCreatedAtDescIdDesc(
            String branchLocationCode,
            BranchStockRequestStatus status,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "branchLocation",
            "items",
            "items.inventoryItem"
    })
    @Query("""
            SELECT bsr
            FROM BranchStockRequest bsr
            WHERE bsr.id = :id
            """)
    Optional<BranchStockRequest> findWithItemsById(
            @Param("id") Long id
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT bsr
            FROM BranchStockRequest bsr
            WHERE bsr.id = :id
            """)
    Optional<BranchStockRequest> findByIdForUpdate(
            @Param("id") Long id
    );
}
