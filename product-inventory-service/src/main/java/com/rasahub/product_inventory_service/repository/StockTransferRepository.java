
package com.rasahub.product_inventory_service.repository;

import com.rasahub.product_inventory_service.entity.StockTransfer;
import com.rasahub.product_inventory_service.enums.StockTransferStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.Optional;

public interface StockTransferRepository
        extends JpaRepository<StockTransfer, Long> {

    @EntityGraph(attributePaths = {
            "sourceLocation",
            "destinationLocation"
    })
    Page<StockTransfer> findAllByOrderByCreatedAtDescIdDesc(
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "sourceLocation",
            "destinationLocation"
    })
    Page<StockTransfer> findByStatusOrderByCreatedAtDescIdDesc(
            StockTransferStatus status,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "sourceLocation",
            "destinationLocation"
    })
    Page<StockTransfer>
    findByDestinationLocation_LocationCodeOrderByCreatedAtDescIdDesc(
            String destinationLocationCode,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "sourceLocation",
            "destinationLocation"
    })
    Page<StockTransfer>
    findByDestinationLocation_LocationCodeAndStatusOrderByCreatedAtDescIdDesc(
            String destinationLocationCode,
            StockTransferStatus status,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "sourceLocation",
            "destinationLocation",
            "items",
            "items.inventoryItem"
    })
    @Query("SELECT t FROM StockTransfer t WHERE t.id = :id")
    Optional<StockTransfer> findWithItemsById(
            @Param("id") Long id
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM StockTransfer t WHERE t.id = :id")
    Optional<StockTransfer> findByIdForUpdate(
            @Param("id") Long id
    );

    @Query("""
            SELECT SUM(ti.transferQuantity)
            FROM StockTransferItem ti
            JOIN ti.stockTransfer t
            WHERE t.branchStockRequest.id = :requestId
              AND ti.inventoryItem.id = :inventoryItemId
              AND t.status = :status
            """)
    BigDecimal sumQuantityForRequestItemAndStatus(
            @Param("requestId") Long requestId,
            @Param("inventoryItemId") Long inventoryItemId,
            @Param("status") StockTransferStatus status
    );
}
