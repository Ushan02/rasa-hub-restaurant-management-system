
package com.rasahub.product_inventory_service.repository;

import com.rasahub.product_inventory_service.entity.StockBalance;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StockBalanceRepository
        extends JpaRepository<StockBalance, Long> {

    Optional<StockBalance> findByStockLocation_LocationCodeAndInventoryItem_Id(
            String locationCode,
            Long inventoryItemId
    );

    List<StockBalance> findByStockLocation_LocationCode(
            String locationCode
    );

    List<StockBalance> findByInventoryItem_Id(
            Long inventoryItemId
    );

    boolean existsByStockLocation_LocationCodeAndInventoryItem_Id(
            String locationCode,
            Long inventoryItemId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT sb
            FROM StockBalance sb
            WHERE sb.stockLocation.locationCode = :locationCode
              AND sb.inventoryItem.id = :inventoryItemId
            """)
    Optional<StockBalance> findForUpdate(
            @Param("locationCode") String locationCode,
            @Param("inventoryItemId") Long inventoryItemId
    );
}
