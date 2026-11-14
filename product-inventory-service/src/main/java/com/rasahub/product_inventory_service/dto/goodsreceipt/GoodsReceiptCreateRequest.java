
package com.rasahub.product_inventory_service.dto.goodsreceipt;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public class GoodsReceiptCreateRequest {

    @NotNull(message = "Purchase order ID is required")
    @Positive(message = "Purchase order ID must be positive")
    private Long purchaseOrderId;

    @NotNull(message = "Received date is required")
    @PastOrPresent(
            message = "Received date cannot be in the future"
    )
    private LocalDate receivedDate;

    @Size(
            max = 100,
            message = "Delivery reference must not exceed 100 characters"
    )
    private String deliveryReference;

    @Size(
            max = 300,
            message = "Remarks must not exceed 300 characters"
    )
    private String remarks;

    @Valid
    @NotEmpty(
            message = "Goods receipt must contain at least one item"
    )
    private List<
            @NotNull(message = "Goods receipt item cannot be null")
                    GoodsReceiptItemRequest
            > items;

    public GoodsReceiptCreateRequest() {
    }

    public Long getPurchaseOrderId() {
        return purchaseOrderId;
    }

    public void setPurchaseOrderId(Long purchaseOrderId) {
        this.purchaseOrderId = purchaseOrderId;
    }

    public LocalDate getReceivedDate() {
        return receivedDate;
    }

    public void setReceivedDate(LocalDate receivedDate) {
        this.receivedDate = receivedDate;
    }

    public String getDeliveryReference() {
        return deliveryReference;
    }

    public void setDeliveryReference(String deliveryReference) {
        this.deliveryReference = deliveryReference;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public List<GoodsReceiptItemRequest> getItems() {
        return items;
    }

    public void setItems(List<GoodsReceiptItemRequest> items) {
        this.items = items;
    }
}
