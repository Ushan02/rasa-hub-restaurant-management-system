
package com.rasahub.product_inventory_service.repository;

import com.rasahub.product_inventory_service.dto.report.PurchaseOrderReportResponse;
import com.rasahub.product_inventory_service.dto.report.SupplierPurchaseReportResponse;
import com.rasahub.product_inventory_service.entity.PurchaseOrderItem;
import com.rasahub.product_inventory_service.enums.PurchaseOrderStatus;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PurchaseOrderItemRepository
        extends JpaRepository<PurchaseOrderItem, Long> {

    @Query(
            value = """
                    SELECT new com.rasahub.product_inventory_service.dto.report.PurchaseOrderReportResponse(
                        purchaseOrder.id,
                        purchaseOrder.purchaseRequest.id,
                        supplier.id,
                        supplier.supplierCode,
                        supplier.supplierName,
                        location.locationCode,
                        location.locationName,
                        purchaseOrder.status,
                        purchaseOrder.expectedDeliveryDate,
                        item.id,
                        item.itemCode,
                        item.itemName,
                        item.measurementUnit,
                        line.orderedQuantity,
                        line.receivedQuantity,
                        line.unitPrice,
                        purchaseOrder.createdByAccountId,
                        purchaseOrder.createdAt
                    )
                    FROM PurchaseOrderItem line
                    JOIN line.purchaseOrder purchaseOrder
                    JOIN purchaseOrder.supplier supplier
                    JOIN purchaseOrder.destinationLocation location
                    JOIN line.inventoryItem item
                    WHERE (:locationCode IS NULL OR location.locationCode = :locationCode)
                      AND (:supplierId IS NULL OR supplier.id = :supplierId)
                      AND (:status IS NULL OR purchaseOrder.status = :status)
                      AND (:from IS NULL OR purchaseOrder.createdAt >= :from)
                      AND (:until IS NULL OR purchaseOrder.createdAt < :until)
                    ORDER BY purchaseOrder.createdAt DESC,
                             purchaseOrder.id DESC, item.id ASC
                    """,
            countQuery = """
                    SELECT COUNT(line)
                    FROM PurchaseOrderItem line
                    JOIN line.purchaseOrder purchaseOrder
                    WHERE (:locationCode IS NULL
                           OR purchaseOrder.destinationLocation.locationCode = :locationCode)
                      AND (:supplierId IS NULL
                           OR purchaseOrder.supplier.id = :supplierId)
                      AND (:status IS NULL OR purchaseOrder.status = :status)
                      AND (:from IS NULL OR purchaseOrder.createdAt >= :from)
                      AND (:until IS NULL OR purchaseOrder.createdAt < :until)
                    """
    )
    Page<PurchaseOrderReportResponse> findReportRows(
            @Param("locationCode") String locationCode,
            @Param("supplierId") Long supplierId,
            @Param("status") PurchaseOrderStatus status,
            @Param("from") LocalDateTime from,
            @Param("until") LocalDateTime until,
            Pageable pageable
    );

    @Query(
            value = """
                    SELECT new com.rasahub.product_inventory_service.dto.report.SupplierPurchaseReportResponse(
                        supplier.id,
                        supplier.supplierCode,
                        supplier.supplierName,
                        purchaseOrder.id,
                        purchaseOrder.createdAt,
                        location.locationCode,
                        item.id,
                        item.itemCode,
                        item.itemName,
                        item.measurementUnit,
                        line.orderedQuantity,
                        COALESCE(SUM(receiptItem.acceptedQuantity), 0)
                    )
                    FROM PurchaseOrderItem line
                    JOIN line.purchaseOrder purchaseOrder
                    JOIN purchaseOrder.supplier supplier
                    JOIN purchaseOrder.destinationLocation location
                    JOIN line.inventoryItem item
                    LEFT JOIN GoodsReceiptItem receiptItem
                      ON receiptItem.inventoryItem = item
                     AND receiptItem.goodsReceiptNote.purchaseOrder = purchaseOrder
                     AND receiptItem.goodsReceiptNote.status =
                         com.rasahub.product_inventory_service.enums.GoodsReceiptStatus.CONFIRMED
                    WHERE (:locationCode IS NULL OR location.locationCode = :locationCode)
                      AND (:supplierId IS NULL OR supplier.id = :supplierId)
                      AND (:status IS NULL OR purchaseOrder.status = :status)
                      AND (:from IS NULL OR purchaseOrder.createdAt >= :from)
                      AND (:until IS NULL OR purchaseOrder.createdAt < :until)
                    GROUP BY supplier.id, supplier.supplierCode, supplier.supplierName,
                             purchaseOrder.id, purchaseOrder.createdAt,
                             location.locationCode, item.id, item.itemCode,
                             item.itemName, item.measurementUnit,
                             line.orderedQuantity
                    ORDER BY purchaseOrder.createdAt DESC,
                             purchaseOrder.id DESC, item.id ASC
                    """,
            countQuery = """
                    SELECT COUNT(line)
                    FROM PurchaseOrderItem line
                    JOIN line.purchaseOrder purchaseOrder
                    WHERE (:locationCode IS NULL
                           OR purchaseOrder.destinationLocation.locationCode = :locationCode)
                      AND (:supplierId IS NULL
                           OR purchaseOrder.supplier.id = :supplierId)
                      AND (:status IS NULL OR purchaseOrder.status = :status)
                      AND (:from IS NULL OR purchaseOrder.createdAt >= :from)
                      AND (:until IS NULL OR purchaseOrder.createdAt < :until)
                    """
    )
    Page<SupplierPurchaseReportResponse> findSupplierPurchaseRows(
            @Param("locationCode") String locationCode,
            @Param("supplierId") Long supplierId,
            @Param("status") PurchaseOrderStatus status,
            @Param("from") LocalDateTime from,
            @Param("until") LocalDateTime until,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "inventoryItem")
    List<PurchaseOrderItem> findByPurchaseOrder_IdOrderByIdAsc(
            Long purchaseOrderId
    );

    boolean existsByPurchaseOrder_IdAndInventoryItem_Id(
            Long purchaseOrderId,
            Long inventoryItemId
    );

    long countByPurchaseOrder_Id(
            Long purchaseOrderId
    );
}
