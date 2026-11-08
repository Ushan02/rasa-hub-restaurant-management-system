package com.rasahub.product_inventory_service.repository;

import com.rasahub.product_inventory_service.entity.StockLocation;
import com.rasahub.product_inventory_service.enums.StockLocationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockLocationRepository  extends JpaRepository<StockLocation, String> {

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
