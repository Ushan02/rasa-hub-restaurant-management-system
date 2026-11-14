
package com.rasahub.product_inventory_service.repository;

import com.rasahub.product_inventory_service.entity.GoodsReceiptNote;
import com.rasahub.product_inventory_service.enums.GoodsReceiptStatus;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface GoodsReceiptNoteRepository
        extends JpaRepository<GoodsReceiptNote, Long> {

    @EntityGraph(attributePaths = {
            "purchaseOrder",
            "receivingLocation"
    })
    Page<GoodsReceiptNote> findAllByOrderByCreatedAtDescIdDesc(
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "purchaseOrder",
            "receivingLocation"
    })
    Page<GoodsReceiptNote> findByStatusOrderByCreatedAtDescIdDesc(
            GoodsReceiptStatus status,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "purchaseOrder",
            "receivingLocation"
    })
    Page<GoodsReceiptNote>
    findByPurchaseOrder_IdOrderByCreatedAtDescIdDesc(
            Long purchaseOrderId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "purchaseOrder",
            "receivingLocation"
    })
    Page<GoodsReceiptNote>
    findByReceivedByAccountIdOrderByCreatedAtDescIdDesc(
            Long receivedByAccountId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "purchaseOrder",
            "purchaseOrder.supplier",
            "receivingLocation",
            "items",
            "items.inventoryItem"
    })
    @Query("""
            SELECT grn
            FROM GoodsReceiptNote grn
            WHERE grn.id = :id
            """)
    Optional<GoodsReceiptNote> findWithItemsById(
            @Param("id") Long id
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT grn
            FROM GoodsReceiptNote grn
            WHERE grn.id = :id
            """)
    Optional<GoodsReceiptNote> findByIdForUpdate(
            @Param("id") Long id
    );

    boolean existsByPurchaseOrder_IdAndStatus(
            Long purchaseOrderId,
            GoodsReceiptStatus status
    );
}
