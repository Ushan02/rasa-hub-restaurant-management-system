
package com.rasahub.product_inventory_service.repository;

import com.rasahub.product_inventory_service.dto.report.GoodsReceivedReportResponse;
import com.rasahub.product_inventory_service.entity.GoodsReceiptItem;
import com.rasahub.product_inventory_service.enums.GoodsReceiptStatus;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface GoodsReceiptItemRepository
        extends JpaRepository<GoodsReceiptItem, Long> {

    @Query(
            value = """
                    SELECT new com.rasahub.product_inventory_service.dto.report.GoodsReceivedReportResponse(
                        receipt.id,
                        purchaseOrder.id,
                        supplier.id,
                        supplier.supplierName,
                        location.locationCode,
                        location.locationName,
                        receipt.status,
                        receipt.receivedDate,
                        item.id,
                        item.itemCode,
                        item.itemName,
                        item.measurementUnit,
                        line.deliveredQuantity,
                        line.acceptedQuantity,
                        line.rejectedQuantity,
                        receipt.receivedByAccountId,
                        receipt.reviewedByAccountId,
                        receipt.reviewedAt,
                        receipt.createdAt
                    )
                    FROM GoodsReceiptItem line
                    JOIN line.goodsReceiptNote receipt
                    JOIN receipt.purchaseOrder purchaseOrder
                    JOIN purchaseOrder.supplier supplier
                    JOIN receipt.receivingLocation location
                    JOIN line.inventoryItem item
                    WHERE (:locationCode IS NULL OR location.locationCode = :locationCode)
                      AND (:supplierId IS NULL OR supplier.id = :supplierId)
                      AND (:status IS NULL OR receipt.status = :status)
                      AND (:startDate IS NULL OR receipt.receivedDate >= :startDate)
                      AND (:endDate IS NULL OR receipt.receivedDate <= :endDate)
                    ORDER BY receipt.receivedDate DESC, receipt.id DESC, item.id ASC
                    """,
            countQuery = """
                    SELECT COUNT(line)
                    FROM GoodsReceiptItem line
                    JOIN line.goodsReceiptNote receipt
                    JOIN receipt.purchaseOrder purchaseOrder
                    WHERE (:locationCode IS NULL
                           OR receipt.receivingLocation.locationCode = :locationCode)
                      AND (:supplierId IS NULL
                           OR purchaseOrder.supplier.id = :supplierId)
                      AND (:status IS NULL OR receipt.status = :status)
                      AND (:startDate IS NULL OR receipt.receivedDate >= :startDate)
                      AND (:endDate IS NULL OR receipt.receivedDate <= :endDate)
                    """
    )
    Page<GoodsReceivedReportResponse> findReportRows(
            @Param("locationCode") String locationCode,
            @Param("supplierId") Long supplierId,
            @Param("status") GoodsReceiptStatus status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "inventoryItem")
    List<GoodsReceiptItem> findByGoodsReceiptNote_IdOrderByIdAsc(
            Long goodsReceiptNoteId
    );

    boolean existsByGoodsReceiptNote_IdAndInventoryItem_Id(
            Long goodsReceiptNoteId,
            Long inventoryItemId
    );

    long countByGoodsReceiptNote_Id(
            Long goodsReceiptNoteId
    );
}
