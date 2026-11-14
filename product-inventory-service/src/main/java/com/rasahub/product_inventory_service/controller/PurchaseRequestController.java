
package com.rasahub.product_inventory_service.controller;

import com.rasahub.product_inventory_service.dto.purchaserequest.PurchaseRequestCreateRequest;
import com.rasahub.product_inventory_service.dto.purchaserequest.PurchaseRequestRejectRequest;
import com.rasahub.product_inventory_service.dto.purchaserequest.PurchaseRequestResponse;

import com.rasahub.product_inventory_service.enums.PurchaseRequestStatus;
import com.rasahub.product_inventory_service.service.PurchaseRequestService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/purchase-requests")
public class PurchaseRequestController {

    private final PurchaseRequestService purchaseRequestService;

    public PurchaseRequestController(
            PurchaseRequestService purchaseRequestService
    ) {
        this.purchaseRequestService = purchaseRequestService;
    }

    @PostMapping
    @PreAuthorize("hasRole('STOCK_KEEPER')")
    public ResponseEntity<PurchaseRequestResponse> createPurchaseRequest(
            @Valid @RequestBody PurchaseRequestCreateRequest request
    ) {
        PurchaseRequestResponse response =
                purchaseRequestService.createPurchaseRequest(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    @PreAuthorize(
            "hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', 'ACCOUNTANT')"
    )
    public ResponseEntity<Page<PurchaseRequestResponse>>
    getAllPurchaseRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                purchaseRequestService.getAllPurchaseRequests(
                        page,
                        size
                )
        );
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('STOCK_KEEPER')")
    public ResponseEntity<Page<PurchaseRequestResponse>>
    getMyPurchaseRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                purchaseRequestService.getMyPurchaseRequests(
                        page,
                        size
                )
        );
    }

    @GetMapping("/status/{status}")
    @PreAuthorize(
            "hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', 'ACCOUNTANT')"
    )
    public ResponseEntity<Page<PurchaseRequestResponse>>
    getPurchaseRequestsByStatus(
            @PathVariable PurchaseRequestStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                purchaseRequestService.getPurchaseRequestsByStatus(
                        status,
                        page,
                        size
                )
        );
    }

    @GetMapping("/{purchaseRequestId}")
    @PreAuthorize(
            "hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', " +
                    "'ACCOUNTANT', 'STOCK_KEEPER')"
    )
    public ResponseEntity<PurchaseRequestResponse>
    getPurchaseRequestById(
            @PathVariable Long purchaseRequestId
    ) {
        return ResponseEntity.ok(
                purchaseRequestService.getPurchaseRequestById(
                        purchaseRequestId
                )
        );
    }

    @PatchMapping("/{purchaseRequestId}/approve")
    @PreAuthorize(
            "hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER')"
    )
    public ResponseEntity<PurchaseRequestResponse>
    approvePurchaseRequest(
            @PathVariable Long purchaseRequestId
    ) {
        return ResponseEntity.ok(
                purchaseRequestService.approvePurchaseRequest(
                        purchaseRequestId
                )
        );
    }

    @PatchMapping("/{purchaseRequestId}/reject")
    @PreAuthorize(
            "hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER')"
    )
    public ResponseEntity<PurchaseRequestResponse>
    rejectPurchaseRequest(
            @PathVariable Long purchaseRequestId,
            @Valid @RequestBody PurchaseRequestRejectRequest request
    ) {
        return ResponseEntity.ok(
                purchaseRequestService.rejectPurchaseRequest(
                        purchaseRequestId,
                        request
                )
        );
    }
}
