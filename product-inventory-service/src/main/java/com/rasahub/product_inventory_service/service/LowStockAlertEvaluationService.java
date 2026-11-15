package com.rasahub.product_inventory_service.service;

import com.rasahub.product_inventory_service.entity.LowStockAlert;

import java.util.List;

public interface LowStockAlertEvaluationService extends LowStockAlertService {

    LowStockAlert evaluateItem(String locationCode, Long inventoryItemId);

    List<LowStockAlert> evaluateLocation(String locationCode);
}
