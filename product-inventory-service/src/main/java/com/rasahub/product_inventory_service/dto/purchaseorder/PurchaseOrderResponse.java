
package com.rasahub.product_inventory_service.dto.purchaseorder;

import com.rasahub.product_inventory_service.entity.PurchaseOrder;
import com.rasahub.product_inventory_service.enums.PurchaseOrderStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class PurchaseOrderResponse {

    private final Long id;
    private final Long purchaseRequestId;

    private final Long supplierId;
    private final String supplierCode;
    private final String supplierName;
    private final String destinationLocationCode;
    private final String destinationLocationName;
    private final Long createdByAccountId;
    private final PurchaseOrderStatus status;
    private final LocalDate expectedDeliveryDate;
    private final String remarks;
    private final String cancellationReason;
    private final List<PurchaseOrderItemResponse> items;
    private final BigDecimal totalAmount;
    private final Long version;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public PurchaseOrderResponse(PurchaseOrder purchaseOrder) {

        this.id = purchaseOrder.getId();

        this.purchaseRequestId =
                purchaseOrder.getPurchaseRequest().getId();

        this.supplierId =
                purchaseOrder.getSupplier().getId();

        this.supplierCode =
                purchaseOrder.getSupplier().getSupplierCode();

        this.supplierName =
                purchaseOrder.getSupplier().getSupplierName();

        this.destinationLocationCode =
                purchaseOrder.getDestinationLocation()
                        .getLocationCode();

        this.destinationLocationName =
                purchaseOrder.getDestinationLocation()
                        .getLocationName();

        this.createdByAccountId =
                purchaseOrder.getCreatedByAccountId();

        this.status = purchaseOrder.getStatus();

        this.expectedDeliveryDate =
                purchaseOrder.getExpectedDeliveryDate();

        this.remarks = purchaseOrder.getRemarks();

        this.cancellationReason =
                purchaseOrder.getCancellationReason();

        this.items = purchaseOrder.getItems()
                .stream()
                .map(PurchaseOrderItemResponse::new)
                .toList();

        this.totalAmount = purchaseOrder.getTotalAmount();

        this.version = purchaseOrder.getVersion();

        this.createdAt = purchaseOrder.getCreatedAt();

        this.updatedAt = purchaseOrder.getUpdatedAt();
    }

    public Long getId() {
        return id;
    }

    public Long getPurchaseRequestId() {
        return purchaseRequestId;
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

    public String getDestinationLocationCode() {
        return destinationLocationCode;
    }

    public String getDestinationLocationName() {
        return destinationLocationName;
    }

    public Long getCreatedByAccountId() {
        return createdByAccountId;
    }

    public PurchaseOrderStatus getStatus() {
        return status;
    }

    public LocalDate getExpectedDeliveryDate() {
        return expectedDeliveryDate;
    }

    public String getRemarks() {
        return remarks;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public List<PurchaseOrderItemResponse> getItems() {
        return items;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
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
