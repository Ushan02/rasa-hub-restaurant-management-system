
package com.rasahub.product_inventory_service.service;

import com.rasahub.product_inventory_service.dto.inventory.InventoryItemCreateRequest;
import com.rasahub.product_inventory_service.dto.inventory.InventoryItemUpdateRequest;
import com.rasahub.product_inventory_service.dto.inventory.InventoryItemResponse;

import java.util.List;

public interface InventoryItemService {

    InventoryItemResponse createInventoryItem(
            InventoryItemCreateRequest request
    );

    List<InventoryItemResponse> getInventoryItems(
            Boolean active
    );

    InventoryItemResponse getInventoryItemById(
            Long id
    );

    InventoryItemResponse getInventoryItemByCode(
            String itemCode
    );

    InventoryItemResponse updateInventoryItem(
            Long id,
            InventoryItemUpdateRequest request
    );

    InventoryItemResponse updateInventoryItemStatus(
            Long id,
            boolean active
    );
}
