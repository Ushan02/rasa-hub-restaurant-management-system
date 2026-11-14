
package com.rasahub.product_inventory_service.controller;

import com.rasahub.product_inventory_service.dto.purchaseorder.PurchaseOrderCancelRequest;
import com.rasahub.product_inventory_service.dto.purchaseorder.PurchaseOrderCreateRequest;
import com.rasahub.product_inventory_service.dto.purchaseorder.PurchaseOrderResponse;

import com.rasahub.product_inventory_service.enums.PurchaseOrderStatus;
import com.rasahub.product_inventory_service.service.PurchaseOrderService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/purchase-orders")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    public PurchaseOrderController(
            PurchaseOrderService purchaseOrderService
    ) {
        this.purchaseOrderService = purchaseOrderService;
    }

    @PostMapping
    @PreAuthorize(
            "hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER')"
    )
    public ResponseEntity<PurchaseOrderResponse> createPurchaseOrder(
            @Valid @RequestBody PurchaseOrderCreateRequest request
    ) {
        PurchaseOrderResponse response =
                purchaseOrderService.createPurchaseOrder(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    @PreAuthorize(
            "hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', " +
                    "'ACCOUNTANT', 'STOCK_KEEPER')"
    )
    public ResponseEntity<Page<PurchaseOrderResponse>>
    getAllPurchaseOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                purchaseOrderService.getAllPurchaseOrders(
                        page,
                        size
                )
        );
    }

    @GetMapping("/my")
    @PreAuthorize(
            "hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER')"
    )
    public ResponseEntity<Page<PurchaseOrderResponse>>
    getMyPurchaseOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                purchaseOrderService.getMyPurchaseOrders(
                        page,
                        size
                )
        );
    }

    @GetMapping("/status/{status}")
    @PreAuthorize(
            "hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', " +
                    "'ACCOUNTANT', 'STOCK_KEEPER')"
    )
    public ResponseEntity<Page<PurchaseOrderResponse>>
    getPurchaseOrdersByStatus(
            @PathVariable PurchaseOrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                purchaseOrderService.getPurchaseOrdersByStatus(
                        status,
                        page,
                        size
                )
        );
    }

    @GetMapping("/supplier/{supplierId}")
    @PreAuthorize(
            "hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', " +
                    "'ACCOUNTANT', 'STOCK_KEEPER')"
    )
    public ResponseEntity<Page<PurchaseOrderResponse>>
    getPurchaseOrdersBySupplier(
            @PathVariable Long supplierId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                purchaseOrderService.getPurchaseOrdersBySupplier(
                        supplierId,
                        page,
                        size
                )
        );
    }

    @GetMapping("/purchase-request/{purchaseRequestId}")
    @PreAuthorize(
            "hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', " +
                    "'ACCOUNTANT', 'STOCK_KEEPER')"
    )
    public ResponseEntity<PurchaseOrderResponse>
    getPurchaseOrderByPurchaseRequestId(
            @PathVariable Long purchaseRequestId
    ) {
        return ResponseEntity.ok(
                purchaseOrderService
                        .getPurchaseOrderByPurchaseRequestId(
                                purchaseRequestId
                        )
        );
    }

    @GetMapping("/{purchaseOrderId}")
    @PreAuthorize(
            "hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', " +
                    "'ACCOUNTANT', 'STOCK_KEEPER')"
    )
    public ResponseEntity<PurchaseOrderResponse>
    getPurchaseOrderById(
            @PathVariable Long purchaseOrderId
    ) {
        return ResponseEntity.ok(
                purchaseOrderService.getPurchaseOrderById(
                        purchaseOrderId
                )
        );
    }

    @PatchMapping("/{purchaseOrderId}/cancel")
    @PreAuthorize(
            "hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER')"
    )
    public ResponseEntity<PurchaseOrderResponse>
    cancelPurchaseOrder(
            @PathVariable Long purchaseOrderId,
            @Valid @RequestBody PurchaseOrderCancelRequest request
    ) {
        return ResponseEntity.ok(
                purchaseOrderService.cancelPurchaseOrder(
                        purchaseOrderId,
                        request
                )
        );
    }
}
