
package com.rasahub.product_inventory_service.repository;

import com.rasahub.product_inventory_service.entity.PurchaseOrder;
import com.rasahub.product_inventory_service.enums.PurchaseOrderStatus;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PurchaseOrderRepository
        extends JpaRepository<PurchaseOrder, Long> {

    @EntityGraph(attributePaths = {
            "supplier",
            "destinationLocation",
            "purchaseRequest"
    })
    Page<PurchaseOrder> findAllByOrderByCreatedAtDescIdDesc(
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "supplier",
            "destinationLocation",
            "purchaseRequest"
    })
    Page<PurchaseOrder>
    findByStatusOrderByCreatedAtDescIdDesc(
            PurchaseOrderStatus status,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "supplier",
            "destinationLocation",
            "purchaseRequest"
    })
    Page<PurchaseOrder>
    findBySupplier_IdOrderByCreatedAtDescIdDesc(
            Long supplierId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "supplier",
            "destinationLocation",
            "purchaseRequest"
    })
    Page<PurchaseOrder>
    findByCreatedByAccountIdOrderByCreatedAtDescIdDesc(
            Long createdByAccountId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "supplier",
            "destinationLocation",
            "purchaseRequest"
    })
    Optional<PurchaseOrder> findByPurchaseRequest_Id(
            Long purchaseRequestId
    );

    boolean existsByPurchaseRequest_Id(
            Long purchaseRequestId
    );

    @EntityGraph(attributePaths = {
            "purchaseRequest",
            "supplier",
            "destinationLocation",
            "items",
            "items.inventoryItem"
    })
    @Query("""
            SELECT po
            FROM PurchaseOrder po
            WHERE po.id = :id
            """)
    Optional<PurchaseOrder> findWithItemsById(
            @Param("id") Long id
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT po
            FROM PurchaseOrder po
            WHERE po.id = :id
            """)
    Optional<PurchaseOrder> findByIdForUpdate(
            @Param("id") Long id
    );
}
