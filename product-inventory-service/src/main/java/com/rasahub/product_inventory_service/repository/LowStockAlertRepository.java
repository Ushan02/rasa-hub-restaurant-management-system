package com.rasahub.product_inventory_service.repository;

import com.rasahub.product_inventory_service.entity.LowStockAlert;
import com.rasahub.product_inventory_service.enums.AlertStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LowStockAlertRepository extends JpaRepository<LowStockAlert, Long> {

    @Override
    @EntityGraph(attributePaths = {"stockLocation", "inventoryItem"})
    Optional<LowStockAlert> findById(Long id);

    @EntityGraph(attributePaths = {"stockLocation", "inventoryItem"})
    List<LowStockAlert> findByStockLocation_LocationCodeOrderByDetectedAtDesc(
            String locationCode
    );

    @EntityGraph(attributePaths = {"stockLocation", "inventoryItem"})
    List<LowStockAlert> findByInventoryItem_IdOrderByDetectedAtDesc(
            Long inventoryItemId
    );

    @EntityGraph(attributePaths = {"stockLocation", "inventoryItem"})
    List<LowStockAlert> findByStockLocation_LocationCodeAndInventoryItem_IdOrderByDetectedAtDesc(
            String locationCode,
            Long inventoryItemId
    );

    Optional<LowStockAlert> findTopByStockLocation_LocationCodeAndInventoryItem_IdAndStatusInOrderByDetectedAtDesc(
            String locationCode,
            Long inventoryItemId,
            Collection<AlertStatus> statuses
    );

    boolean existsByStockLocation_LocationCodeAndInventoryItem_IdAndStatusIn(
            String locationCode,
            Long inventoryItemId,
            Collection<AlertStatus> statuses
    );

    long countByStockLocation_LocationCodeAndStatusIn(
            String locationCode,
            Collection<AlertStatus> statuses
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"stockLocation", "inventoryItem"})
    @Query("""
            SELECT la
            FROM LowStockAlert la
            WHERE la.stockLocation.locationCode = :locationCode
              AND la.inventoryItem.id = :inventoryItemId
              AND la.status IN :statuses
            ORDER BY la.detectedAt DESC
            """)
    List<LowStockAlert> findActiveForUpdate(
            @Param("locationCode") String locationCode,
            @Param("inventoryItemId") Long inventoryItemId,
            @Param("statuses") Collection<AlertStatus> statuses
    );
}
