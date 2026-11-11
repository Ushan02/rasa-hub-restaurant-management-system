
package com.rasahub.product_inventory_service.repository;

import com.rasahub.product_inventory_service.entity.StockMovement;
import com.rasahub.product_inventory_service.enums.StockMovementType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockMovementRepository
        extends JpaRepository<StockMovement, Long> {

    @EntityGraph(attributePaths = {
            "stockLocation",
            "inventoryItem"
    })
    Page<StockMovement>
    findByStockLocation_LocationCodeOrderByCreatedAtDescIdDesc(
            String locationCode,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "stockLocation",
            "inventoryItem"
    })
    Page<StockMovement>
    findByInventoryItem_IdOrderByCreatedAtDescIdDesc(
            Long inventoryItemId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "stockLocation",
            "inventoryItem"
    })
    Page<StockMovement>
    findByStockLocation_LocationCodeAndInventoryItem_IdOrderByCreatedAtDescIdDesc(
            String locationCode,
            Long inventoryItemId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "stockLocation",
            "inventoryItem"
    })
    List<StockMovement>
    findByReferenceTypeAndReferenceIdOrderByIdAsc(
            String referenceType,
            String referenceId
    );

    @EntityGraph(attributePaths = {
            "stockLocation",
            "inventoryItem"
    })
    Page<StockMovement>
    findByMovementTypeOrderByCreatedAtDescIdDesc(
            StockMovementType movementType,
            Pageable pageable
    );
}
