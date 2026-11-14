
package com.rasahub.product_inventory_service.service;

import com.rasahub.product_inventory_service.dto.goodsreceipt.GoodsReceiptCreateRequest;
import com.rasahub.product_inventory_service.dto.goodsreceipt.GoodsReceiptRejectRequest;
import com.rasahub.product_inventory_service.dto.goodsreceipt.GoodsReceiptResponse;

import com.rasahub.product_inventory_service.enums.GoodsReceiptStatus;

import org.springframework.data.domain.Page;

public interface GoodsReceiptService {

    GoodsReceiptResponse createGoodsReceipt(
            GoodsReceiptCreateRequest request
    );

    GoodsReceiptResponse submitGoodsReceipt(
            Long goodsReceiptId
    );

    GoodsReceiptResponse confirmGoodsReceipt(
            Long goodsReceiptId
    );

    GoodsReceiptResponse rejectGoodsReceipt(
            Long goodsReceiptId,
            GoodsReceiptRejectRequest request
    );

    GoodsReceiptResponse getGoodsReceiptById(
            Long goodsReceiptId
    );

    Page<GoodsReceiptResponse> getAllGoodsReceipts(
            int page,
            int size
    );

    Page<GoodsReceiptResponse> getGoodsReceiptsByStatus(
            GoodsReceiptStatus status,
            int page,
            int size
    );

    Page<GoodsReceiptResponse> getGoodsReceiptsByPurchaseOrder(
            Long purchaseOrderId,
            int page,
            int size
    );

    Page<GoodsReceiptResponse> getMyGoodsReceipts(
            int page,
            int size
    );
}
