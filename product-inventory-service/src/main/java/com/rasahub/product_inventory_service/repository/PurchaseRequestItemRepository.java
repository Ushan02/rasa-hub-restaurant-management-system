
package com.rasahub.product_inventory_service.repository;

import com.rasahub.product_inventory_service.entity.PurchaseRequestItem;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseRequestItemRepository
        extends JpaRepository<PurchaseRequestItem, Long> {

    List<PurchaseRequestItem> findByPurchaseRequest_IdOrderByIdAsc(
            Long purchaseRequestId
    );

    boolean existsByPurchaseRequest_IdAndInventoryItem_Id(
            Long purchaseRequestId,
            Long inventoryItemId
    );

    long countByPurchaseRequest_Id(
            Long purchaseRequestId
    );
}
