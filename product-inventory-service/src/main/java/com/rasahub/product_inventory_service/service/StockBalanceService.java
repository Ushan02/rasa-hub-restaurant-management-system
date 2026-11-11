
package com.rasahub.product_inventory_service.service;

import com.rasahub.product_inventory_service.dto.stockbalance.StockBalanceMinimumLevelUpdateRequest;
import com.rasahub.product_inventory_service.dto.stockbalance.StockBalanceResponse;

import java.util.List;

public interface StockBalanceService {

    List<StockBalanceResponse> getBalancesByLocation(
            String locationCode
    );

    List<StockBalanceResponse> getBalancesByInventoryItem(
            Long inventoryItemId
    );

    StockBalanceResponse getBalance(
            String locationCode,
            Long inventoryItemId
    );

    List<StockBalanceResponse> getLowStockBalances(
            String locationCode
    );

    StockBalanceResponse updateMinimumStockLevel(
            String locationCode,
            Long inventoryItemId,
            StockBalanceMinimumLevelUpdateRequest request
    );
}
