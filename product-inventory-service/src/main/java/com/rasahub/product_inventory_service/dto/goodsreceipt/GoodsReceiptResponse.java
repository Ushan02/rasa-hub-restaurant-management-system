
package com.rasahub.product_inventory_service.dto.goodsreceipt;

import com.rasahub.product_inventory_service.entity.GoodsReceiptNote;
import com.rasahub.product_inventory_service.entity.PurchaseOrder;
import com.rasahub.product_inventory_service.entity.Supplier;
import com.rasahub.product_inventory_service.entity.StockLocation;

import com.rasahub.product_inventory_service.enums.GoodsReceiptStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class GoodsReceiptResponse {

    private final Long id;
    private final Long purchaseOrderId;

    private final Long supplierId;
    private final String supplierCode;
    private final String supplierName;

    private final String receivingLocationCode;
    private final String receivingLocationName;

    private final Long receivedByAccountId;
    private final LocalDate receivedDate;
    private final String deliveryReference;

    private final GoodsReceiptStatus status;

    private final Long reviewedByAccountId;
    private final LocalDateTime reviewedAt;

    private final String remarks;
    private final String rejectionReason;

    private final List<GoodsReceiptItemResponse> items;

    private final Long version;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public GoodsReceiptResponse(GoodsReceiptNote grn) {

        PurchaseOrder purchaseOrder =
                grn.getPurchaseOrder();

        Supplier supplier =
                purchaseOrder.getSupplier();

        StockLocation receivingLocation =
                grn.getReceivingLocation();

        this.id = grn.getId();

        this.purchaseOrderId =
                purchaseOrder.getId();

        this.supplierId =
                supplier.getId();

        this.supplierCode =
                supplier.getSupplierCode();

        this.supplierName =
                supplier.getSupplierName();

        this.receivingLocationCode =
                receivingLocation.getLocationCode();

        this.receivingLocationName =
                receivingLocation.getLocationName();

        this.receivedByAccountId =
                grn.getReceivedByAccountId();

        this.receivedDate =
                grn.getReceivedDate();

        this.deliveryReference =
                grn.getDeliveryReference();

        this.status =
                grn.getStatus();

        this.reviewedByAccountId =
                grn.getReviewedByAccountId();

        this.reviewedAt =
                grn.getReviewedAt();

        this.remarks =
                grn.getRemarks();

        this.rejectionReason =
                grn.getRejectionReason();

        this.items = grn.getItems()
                .stream()
                .map(GoodsReceiptItemResponse::new)
                .toList();

        this.version =
                grn.getVersion();

        this.createdAt =
                grn.getCreatedAt();

        this.updatedAt =
                grn.getUpdatedAt();
    }

    public Long getId() {
        return id;
    }

    public Long getPurchaseOrderId() {
        return purchaseOrderId;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public String getSupplierCode() {
        return supplierCode;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public String getReceivingLocationCode() {
        return receivingLocationCode;
    }

    public String getReceivingLocationName() {
        return receivingLocationName;
    }

    public Long getReceivedByAccountId() {
        return receivedByAccountId;
    }

    public LocalDate getReceivedDate() {
        return receivedDate;
    }

    public String getDeliveryReference() {
        return deliveryReference;
    }

    public GoodsReceiptStatus getStatus() {
        return status;
    }

    public Long getReviewedByAccountId() {
        return reviewedByAccountId;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public String getRemarks() {
        return remarks;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public List<GoodsReceiptItemResponse> getItems() {
        return items;
    }

    public Long getVersion() {
        return version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
