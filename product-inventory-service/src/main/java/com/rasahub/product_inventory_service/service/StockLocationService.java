
package com.rasahub.product_inventory_service.service;

import com.rasahub.product_inventory_service.dto.stocklocation.StockLocationResponse;
import com.rasahub.product_inventory_service.dto.stocklocation.StockLocationUpdateRequest;
import com.rasahub.product_inventory_service.enums.StockLocationType;

import java.util.List;

public interface StockLocationService {

    List<StockLocationResponse> getStockLocations(
            StockLocationType locationType,
            Boolean active
    );

    StockLocationResponse getStockLocationByCode(
            String locationCode
    );

    StockLocationResponse updateStockLocation(
            String locationCode,
            StockLocationUpdateRequest request
    );

    StockLocationResponse updateStockLocationStatus(
            String locationCode,
            boolean active
    );
}
