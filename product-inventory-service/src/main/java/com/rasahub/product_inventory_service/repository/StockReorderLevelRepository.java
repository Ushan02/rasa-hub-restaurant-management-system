package com.rasahub.product_inventory_service.repository;

import com.rasahub.product_inventory_service.entity.StockReorderLevel;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StockReorderLevelRepository extends JpaRepository<StockReorderLevel, Long> {

    Optional<StockReorderLevel> findByStockLocation_LocationCodeAndInventoryItem_Id(
            String locationCode,
            Long inventoryItemId
    );

    List<StockReorderLevel> findByStockLocation_LocationCodeAndActiveTrue(
            String locationCode
    );

    List<StockReorderLevel> findByActiveTrue();

    boolean existsByStockLocation_LocationCodeAndInventoryItem_Id(
            String locationCode,
            Long inventoryItemId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT srl
            FROM StockReorderLevel srl
            WHERE srl.stockLocation.locationCode = :locationCode
              AND srl.inventoryItem.id = :inventoryItemId
            """)
    Optional<StockReorderLevel> findForUpdate(
            @Param("locationCode") String locationCode,
            @Param("inventoryItemId") Long inventoryItemId
    );
}
