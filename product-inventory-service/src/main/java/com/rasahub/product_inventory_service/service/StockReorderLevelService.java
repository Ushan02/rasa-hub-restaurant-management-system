package com.rasahub.product_inventory_service.service;

import com.rasahub.product_inventory_service.dto.stockreorderlevel.StockReorderLevelResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface StockReorderLevelService {

    StockReorderLevelResponse createConfiguration(
            String locationCode,
            Long inventoryItemId,
            BigDecimal reorderLevel,
            BigDecimal recommendedReorderQuantity,
            Long createdByAccountId
    );

    StockReorderLevelResponse updateConfiguration(
            String locationCode,
            Long inventoryItemId,
            BigDecimal reorderLevel,
            BigDecimal recommendedReorderQuantity,
            Long updatedByAccountId
    );

    StockReorderLevelResponse deactivateConfiguration(
            String locationCode,
            Long inventoryItemId,
            Long updatedByAccountId
    );

    StockReorderLevelResponse reactivateConfiguration(
            String locationCode,
            Long inventoryItemId,
            Long updatedByAccountId
    );

    List<StockReorderLevelResponse> getConfigurationsByLocation(String locationCode);

    Optional<StockReorderLevelResponse> getConfiguration(String locationCode, Long inventoryItemId);
}
