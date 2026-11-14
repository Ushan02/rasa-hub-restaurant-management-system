
package com.rasahub.product_inventory_service.repository;

import com.rasahub.product_inventory_service.entity.PurchaseOrderItem;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseOrderItemRepository
        extends JpaRepository<PurchaseOrderItem, Long> {

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
