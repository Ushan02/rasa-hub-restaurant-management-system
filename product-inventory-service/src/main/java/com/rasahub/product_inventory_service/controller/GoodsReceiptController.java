
package com.rasahub.product_inventory_service.controller;

import com.rasahub.product_inventory_service.dto.goodsreceipt.GoodsReceiptCreateRequest;
import com.rasahub.product_inventory_service.dto.goodsreceipt.GoodsReceiptRejectRequest;
import com.rasahub.product_inventory_service.dto.goodsreceipt.GoodsReceiptResponse;

import com.rasahub.product_inventory_service.enums.GoodsReceiptStatus;
import com.rasahub.product_inventory_service.service.GoodsReceiptService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/goods-receipts")
public class GoodsReceiptController {

    private final GoodsReceiptService goodsReceiptService;

    public GoodsReceiptController(
            GoodsReceiptService goodsReceiptService
    ) {
        this.goodsReceiptService = goodsReceiptService;
    }

    @PostMapping
    @PreAuthorize("hasRole('STOCK_KEEPER')")
    public ResponseEntity<GoodsReceiptResponse> createGoodsReceipt(
            @Valid @RequestBody GoodsReceiptCreateRequest request
    ) {
        GoodsReceiptResponse response =
                goodsReceiptService.createGoodsReceipt(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PatchMapping("/{goodsReceiptId}/submit")
    @PreAuthorize("hasRole('STOCK_KEEPER')")
    public ResponseEntity<GoodsReceiptResponse> submitGoodsReceipt(
            @PathVariable Long goodsReceiptId
    ) {
        return ResponseEntity.ok(
                goodsReceiptService.submitGoodsReceipt(goodsReceiptId)
        );
    }

    @PatchMapping("/{goodsReceiptId}/confirm")
    @PreAuthorize(
            "hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER')"
    )
    public ResponseEntity<GoodsReceiptResponse> confirmGoodsReceipt(
            @PathVariable Long goodsReceiptId
    ) {
        return ResponseEntity.ok(
                goodsReceiptService.confirmGoodsReceipt(goodsReceiptId)
        );
    }

    @PatchMapping("/{goodsReceiptId}/reject")
    @PreAuthorize(
            "hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER')"
    )
    public ResponseEntity<GoodsReceiptResponse> rejectGoodsReceipt(
            @PathVariable Long goodsReceiptId,
            @Valid @RequestBody GoodsReceiptRejectRequest request
    ) {
        return ResponseEntity.ok(
                goodsReceiptService.rejectGoodsReceipt(
                        goodsReceiptId,
                        request
                )
        );
    }

    @GetMapping
    @PreAuthorize(
            "hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', 'ACCOUNTANT')"
    )
    public ResponseEntity<Page<GoodsReceiptResponse>> getAllGoodsReceipts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                goodsReceiptService.getAllGoodsReceipts(
                        page,
                        size
                )
        );
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('STOCK_KEEPER')")
    public ResponseEntity<Page<GoodsReceiptResponse>> getMyGoodsReceipts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                goodsReceiptService.getMyGoodsReceipts(
                        page,
                        size
                )
        );
    }

    @GetMapping("/status/{status}")
    @PreAuthorize(
            "hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', 'ACCOUNTANT')"
    )
    public ResponseEntity<Page<GoodsReceiptResponse>>
    getGoodsReceiptsByStatus(
            @PathVariable GoodsReceiptStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                goodsReceiptService.getGoodsReceiptsByStatus(
                        status,
                        page,
                        size
                )
        );
    }

    @GetMapping("/purchase-order/{purchaseOrderId}")
    @PreAuthorize(
            "hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', 'ACCOUNTANT')"
    )
    public ResponseEntity<Page<GoodsReceiptResponse>>
    getGoodsReceiptsByPurchaseOrder(
            @PathVariable Long purchaseOrderId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                goodsReceiptService.getGoodsReceiptsByPurchaseOrder(
                        purchaseOrderId,
                        page,
                        size
                )
        );
    }

    @GetMapping("/{goodsReceiptId}")
    @PreAuthorize(
            "hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', " +
                    "'ACCOUNTANT', 'STOCK_KEEPER')"
    )
    public ResponseEntity<GoodsReceiptResponse> getGoodsReceiptById(
            @PathVariable Long goodsReceiptId
    ) {
        return ResponseEntity.ok(
                goodsReceiptService.getGoodsReceiptById(goodsReceiptId)
        );
    }
}
