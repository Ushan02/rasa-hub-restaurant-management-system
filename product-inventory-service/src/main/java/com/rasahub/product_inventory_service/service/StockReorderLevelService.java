package com.rasahub.product_inventory_service.service;

import com.rasahub.product_inventory_service.entity.StockReorderLevel;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface StockReorderLevelService {

    StockReorderLevel createConfiguration(
            String locationCode,
            Long inventoryItemId,
            BigDecimal reorderLevel,
            BigDecimal recommendedReorderQuantity,
            Long createdByAccountId
    );

    StockReorderLevel updateConfiguration(
            String locationCode,
            Long inventoryItemId,
            BigDecimal reorderLevel,
            BigDecimal recommendedReorderQuantity,
            Long updatedByAccountId
    );

    StockReorderLevel deactivateConfiguration(
            String locationCode,
            Long inventoryItemId,
            Long updatedByAccountId
    );

    List<StockReorderLevel> getConfigurationsByLocation(String locationCode);

    Optional<StockReorderLevel> getConfiguration(String locationCode, Long inventoryItemId);
}
