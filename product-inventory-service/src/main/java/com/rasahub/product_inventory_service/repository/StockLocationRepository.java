package com.rasahub.product_inventory_service.repository;

import com.rasahub.product_inventory_service.entity.StockLocation;
import com.rasahub.product_inventory_service.enums.StockLocationType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StockLocationRepository  extends JpaRepository<StockLocation, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT location
            FROM StockLocation location
            WHERE location.locationCode = :locationCode
            """)
    Optional<StockLocation> findByIdForUpdate(
            @Param("locationCode") String locationCode
    );

    List<StockLocation> findByActiveOrderByLocationCodeAsc(
            boolean active
    );

    List<StockLocation> findByLocationTypeOrderByLocationCodeAsc(
            StockLocationType locationType
    );

    List<StockLocation> findByLocationTypeAndActiveOrderByLocationCodeAsc(
            StockLocationType locationType,
            boolean active
    );
}
