package com.rasahub.product_inventory_service.service;

import com.rasahub.product_inventory_service.dto.supplieritem.SupplierItemCreateRequest;
import com.rasahub.product_inventory_service.dto.supplieritem.SupplierItemResponse;
import com.rasahub.product_inventory_service.dto.supplieritem.SupplierItemUpdateRequest;


import java.util.List;

public interface SupplierItemService {

    SupplierItemResponse createSupplierItem(
            SupplierItemCreateRequest request
    );

    List<SupplierItemResponse> getSupplierItems(
            Boolean active
    );

    SupplierItemResponse getSupplierItemById(
            Long id
    );

    SupplierItemResponse getSupplierItemBySupplierAndInventoryItem(
            Long supplierId,
            Long inventoryItemID
    );

    List<SupplierItemResponse> getSupplierItemsBySupplier(
            Long supplierId,
            Boolean active
    );

    List<SupplierItemResponse> getSupplierItemsByInventoryItem(
            Long inventoryItemId
    );

    SupplierItemResponse updateSupplierItem(
            Long id,
            SupplierItemUpdateRequest request
    );

    SupplierItemResponse updateSupplierItemStatus(
            Long id,
            boolean active
    );

}
