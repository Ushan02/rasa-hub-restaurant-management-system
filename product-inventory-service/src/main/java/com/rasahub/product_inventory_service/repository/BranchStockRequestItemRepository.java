
package com.rasahub.product_inventory_service.repository;

import com.rasahub.product_inventory_service.entity.BranchStockRequestItem;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BranchStockRequestItemRepository
        extends JpaRepository<BranchStockRequestItem, Long> {

    @EntityGraph(attributePaths = "inventoryItem")
    List<BranchStockRequestItem>
    findByBranchStockRequest_IdOrderByIdAsc(
            Long branchStockRequestId
    );

    boolean existsByBranchStockRequest_IdAndInventoryItem_Id(
            Long branchStockRequestId,
            Long inventoryItemId
    );

    long countByBranchStockRequest_Id(
            Long branchStockRequestId
    );
}
