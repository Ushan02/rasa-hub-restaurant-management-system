
package com.rasahub.product_inventory_service.repository;

import com.rasahub.product_inventory_service.entity.GoodsReceiptItem;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GoodsReceiptItemRepository
        extends JpaRepository<GoodsReceiptItem, Long> {

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
