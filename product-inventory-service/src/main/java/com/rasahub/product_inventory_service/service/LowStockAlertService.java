package com.rasahub.product_inventory_service.service;

import com.rasahub.product_inventory_service.entity.LowStockAlert;

import java.util.List;

public interface LowStockAlertService {

    List<LowStockAlert> getAlertsByLocation(String locationCode);

    List<LowStockAlert> getAlertsByItem(Long inventoryItemId);

    LowStockAlert acknowledgeAlert(Long alertId, Long accountId, String remarks);

    LowStockAlert resolveAlert(Long alertId, String remarks);
}
