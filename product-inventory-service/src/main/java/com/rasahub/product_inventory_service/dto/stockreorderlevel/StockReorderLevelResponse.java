package com.rasahub.product_inventory_service.dto.stockreorderlevel;

import com.rasahub.product_inventory_service.entity.StockReorderLevel;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StockReorderLevelResponse(
        Long id,
        String locationCode,
        Long inventoryItemId,
        String itemName,
        BigDecimal currentQuantity,
        BigDecimal reorderLevel,
        BigDecimal recommendedReorderQuantity,
        boolean active,
        Long createdByAccountId,
        Long updatedByAccountId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static StockReorderLevelResponse from(
            StockReorderLevel config,
            BigDecimal currentQuantity
    ) {
        return new StockReorderLevelResponse(
                config.getId(),
                config.getStockLocation().getLocationCode(),
                config.getInventoryItem().getId(),
                config.getInventoryItem().getItemName(),
                currentQuantity,
                config.getReorderLevel(),
                config.getRecommendedReorderQuantity(),
                config.isActive(),
                config.getCreatedByAccountId(),
                config.getUpdatedByAccountId(),
                config.getCreatedAt(),
                config.getUpdatedAt()
        );
    }
}
