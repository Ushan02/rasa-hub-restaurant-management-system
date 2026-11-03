
package com.rasahub.product_inventory_service.repository;

import com.rasahub.product_inventory_service.entity.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface InventoryItemRepository
        extends JpaRepository<InventoryItem, Long> {

    boolean existsByItemNameIgnoreCase(String itemName);

    boolean existsByItemNameIgnoreCaseAndIdNot(
            String itemName,
            Long id
    );

    boolean existsByItemCode(String itemCode);

    Optional<InventoryItem> findByItemCodeIgnoreCase(
            String itemCode
    );

    List<InventoryItem> findByActiveOrderByIdAsc(
            boolean active
    );

    @Query(
            value = "SELECT nextval('inventory_item_code_seq')",
            nativeQuery = true
    )
    Long getNextInventoryItemCodeSequenceValue();
}
