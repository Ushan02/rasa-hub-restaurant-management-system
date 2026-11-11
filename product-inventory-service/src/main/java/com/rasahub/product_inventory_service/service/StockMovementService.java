
package com.rasahub.product_inventory_service.service;

import com.rasahub.product_inventory_service.dto.stockmovement.StockMovementResponse;
import com.rasahub.product_inventory_service.enums.StockMovementType;

import org.springframework.data.domain.Page;

import java.util.List;

public interface StockMovementService {

    Page<StockMovementResponse> getMovementsByLocation(
            String locationCode,
            int page,
            int size
    );

    Page<StockMovementResponse> getMovementsByInventoryItem(
            Long inventoryItemId,
            int page,
            int size
    );

    Page<StockMovementResponse> getMovementsByLocationAndItem(
            String locationCode,
            Long inventoryItemId,
            int page,
            int size
    );

    List<StockMovementResponse> getMovementsByReference(
            String referenceType,
            String referenceId
    );

    Page<StockMovementResponse> getMovementsByType(
            StockMovementType movementType,
            int page,
            int size
    );
}
