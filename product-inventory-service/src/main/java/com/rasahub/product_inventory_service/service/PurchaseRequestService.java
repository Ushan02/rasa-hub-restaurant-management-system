
package com.rasahub.product_inventory_service.service;

import com.rasahub.product_inventory_service.dto.purchaserequest.PurchaseRequestCreateRequest;
import com.rasahub.product_inventory_service.dto.purchaserequest.PurchaseRequestRejectRequest;
import com.rasahub.product_inventory_service.dto.purchaserequest.PurchaseRequestResponse;
import com.rasahub.product_inventory_service.enums.PurchaseRequestStatus;

import org.springframework.data.domain.Page;

public interface PurchaseRequestService {

    PurchaseRequestResponse createPurchaseRequest(
            PurchaseRequestCreateRequest request
    );

    PurchaseRequestResponse getPurchaseRequestById(
            Long purchaseRequestId
    );

    Page<PurchaseRequestResponse> getAllPurchaseRequests(
            int page,
            int size
    );

    Page<PurchaseRequestResponse> getPurchaseRequestsByStatus(
            PurchaseRequestStatus status,
            int page,
            int size
    );

    Page<PurchaseRequestResponse> getMyPurchaseRequests(
            int page,
            int size
    );

    PurchaseRequestResponse approvePurchaseRequest(
            Long purchaseRequestId
    );

    PurchaseRequestResponse rejectPurchaseRequest(
            Long purchaseRequestId,
            PurchaseRequestRejectRequest request
    );
}
