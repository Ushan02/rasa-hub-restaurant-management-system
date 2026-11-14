
package com.rasahub.product_inventory_service.service;

import com.rasahub.product_inventory_service.dto.purchaseorder.PurchaseOrderCreateRequest;
import com.rasahub.product_inventory_service.dto.purchaseorder.PurchaseOrderCancelRequest;
import com.rasahub.product_inventory_service.dto.purchaseorder.PurchaseOrderResponse;

import com.rasahub.product_inventory_service.enums.PurchaseOrderStatus;

import org.springframework.data.domain.Page;

public interface PurchaseOrderService {

    PurchaseOrderResponse createPurchaseOrder(
            PurchaseOrderCreateRequest request
    );

    PurchaseOrderResponse getPurchaseOrderById(
            Long purchaseOrderId
    );

    Page<PurchaseOrderResponse> getAllPurchaseOrders(
            int page,
            int size
    );

    Page<PurchaseOrderResponse> getPurchaseOrdersByStatus(
            PurchaseOrderStatus status,
            int page,
            int size
    );

    Page<PurchaseOrderResponse> getPurchaseOrdersBySupplier(
            Long supplierId,
            int page,
            int size
    );

    Page<PurchaseOrderResponse> getMyPurchaseOrders(
            int page,
            int size
    );

    PurchaseOrderResponse getPurchaseOrderByPurchaseRequestId(
            Long purchaseRequestId
    );

    PurchaseOrderResponse cancelPurchaseOrder(
            Long purchaseOrderId,
            PurchaseOrderCancelRequest request
    );
}
