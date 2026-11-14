
package com.rasahub.product_inventory_service.repository;

import com.rasahub.product_inventory_service.entity.PurchaseRequest;
import com.rasahub.product_inventory_service.enums.PurchaseRequestStatus;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PurchaseRequestRepository
        extends JpaRepository<PurchaseRequest, Long> {

    @EntityGraph(attributePaths = "destinationLocation")
    Page<PurchaseRequest> findAllByOrderByCreatedAtDescIdDesc(
            Pageable pageable
    );

    @EntityGraph(attributePaths = "destinationLocation")
    Page<PurchaseRequest>
    findByStatusOrderByCreatedAtDescIdDesc(
            PurchaseRequestStatus status,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "destinationLocation")
    Page<PurchaseRequest>
    findByRequestedByAccountIdOrderByCreatedAtDescIdDesc(
            Long requestedByAccountId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "destinationLocation",
            "items",
            "items.inventoryItem"
    })
    @Query("""
            SELECT pr
            FROM PurchaseRequest pr
            WHERE pr.id = :id
            """)
    Optional<PurchaseRequest> findWithItemsById(
            @Param("id") Long id
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT pr
            FROM PurchaseRequest pr
            WHERE pr.id = :id
            """)
    Optional<PurchaseRequest> findByIdForUpdate(
            @Param("id") Long id
    );

    boolean existsByIdAndStatus(
            Long id,
            PurchaseRequestStatus status
    );
}
