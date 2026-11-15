
package com.rasahub.product_inventory_service.repository;

import com.rasahub.product_inventory_service.dto.report.StockTransferReportResponse;
import com.rasahub.product_inventory_service.entity.StockTransfer;
import com.rasahub.product_inventory_service.enums.StockTransferStatus;
import com.rasahub.product_inventory_service.repository.LocationCountProjection;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface StockTransferRepository
        extends JpaRepository<StockTransfer, Long> {

    @Query("""
            SELECT transfer.destinationLocation.locationCode AS locationCode,
                   COUNT(transfer.id) AS recordCount
            FROM StockTransfer transfer
            WHERE transfer.status = :status
              AND transfer.receivedAt >= :from
              AND transfer.receivedAt < :until
            GROUP BY transfer.destinationLocation.locationCode
            """)
    List<LocationCountProjection> countTransfersByStatusAndReceiptPeriod(
            @Param("status") StockTransferStatus status,
            @Param("from") LocalDateTime from,
            @Param("until") LocalDateTime until
    );

    @Query(
            value = """
                    SELECT new com.rasahub.product_inventory_service.dto.report.StockTransferReportResponse(
                        transfer.id,
                        transfer.branchStockRequest.id,
                        source.locationCode,
                        source.locationName,
                        destination.locationCode,
                        destination.locationName,
                        transfer.status,
                        item.id,
                        item.itemCode,
                        item.itemName,
                        item.measurementUnit,
                        line.transferQuantity,
                        transfer.createdByAccountId,
                        transfer.dispatchedByAccountId,
                        transfer.dispatchedAt,
                        transfer.receivedByAccountId,
                        transfer.receivedAt,
                        transfer.createdAt
                    )
                    FROM StockTransferItem line
                    JOIN line.stockTransfer transfer
                    JOIN transfer.sourceLocation source
                    JOIN transfer.destinationLocation destination
                    JOIN line.inventoryItem item
                    WHERE (:locationCode IS NULL
                           OR source.locationCode = :locationCode
                           OR destination.locationCode = :locationCode)
                      AND (:status IS NULL OR transfer.status = :status)
                      AND (:from IS NULL OR transfer.createdAt >= :from)
                      AND (:until IS NULL OR transfer.createdAt < :until)
                    ORDER BY transfer.createdAt DESC, transfer.id DESC, item.id ASC
                    """,
            countQuery = """
                    SELECT COUNT(line)
                    FROM StockTransferItem line
                    JOIN line.stockTransfer transfer
                    WHERE (:locationCode IS NULL
                           OR transfer.sourceLocation.locationCode = :locationCode
                           OR transfer.destinationLocation.locationCode = :locationCode)
                      AND (:status IS NULL OR transfer.status = :status)
                      AND (:from IS NULL OR transfer.createdAt >= :from)
                      AND (:until IS NULL OR transfer.createdAt < :until)
                    """
    )
    Page<StockTransferReportResponse> findReportRows(
            @Param("locationCode") String locationCode,
            @Param("status") StockTransferStatus status,
            @Param("from") LocalDateTime from,
            @Param("until") LocalDateTime until,
            Pageable pageable
    );

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
