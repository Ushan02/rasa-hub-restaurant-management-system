
package com.rasahub.product_inventory_service.repository;

import com.rasahub.product_inventory_service.entity.SupplierItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupplierItemRepository
        extends JpaRepository<SupplierItem, Long> {

    boolean existsBySupplier_IdAndInventoryItem_Id(
            Long supplierId,
            Long inventoryItemId
    );

    Optional<SupplierItem> findBySupplier_IdAndInventoryItem_Id(
            Long supplierId,
            Long inventoryItemId
    );

    List<SupplierItem> findBySupplier_IdOrderByIdAsc(
            Long supplierId
    );

    List<SupplierItem> findByInventoryItem_IdOrderByIdAsc(
            Long inventoryItemId
    );
    List<SupplierItem> findByActiveOrderByIdAsc(
            boolean active
    );

    List<SupplierItem> findBySupplier_IdAndActiveOrderByIdAsc(
            Long supplierId,
            boolean active
    );
}
