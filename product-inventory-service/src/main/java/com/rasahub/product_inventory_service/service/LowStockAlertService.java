package com.rasahub.product_inventory_service.service;

import com.rasahub.product_inventory_service.dto.lowstock.LowStockAlertResponse;
import com.rasahub.product_inventory_service.dto.lowstock.LowStockSummaryResponse;

import java.util.List;

public interface LowStockAlertService {

    List<LowStockAlertResponse> getAlertsByLocation(String locationCode);

    List<LowStockAlertResponse> getAlertsByItem(Long inventoryItemId);

    LowStockAlertResponse acknowledgeAlert(Long alertId, Long accountId, String remarks);

    LowStockAlertResponse resolveAlert(Long alertId, String remarks);

    LowStockSummaryResponse getSummaryByLocation(String locationCode);
}
