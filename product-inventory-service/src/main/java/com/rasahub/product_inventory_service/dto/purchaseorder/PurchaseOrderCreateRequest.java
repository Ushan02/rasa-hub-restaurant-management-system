
package com.rasahub.product_inventory_service.dto.purchaseorder;

import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public class PurchaseOrderCreateRequest {

    @NotNull(message = "Purchase request ID is required")
    @Positive(message = "Purchase request ID must be positive")
    private Long purchaseRequestId;

    @NotNull(message = "Supplier ID is required")
    @Positive(message = "Supplier ID must be positive")
    private Long supplierId;

    @NotNull(message = "Expected delivery date is required")
    @FutureOrPresent(
            message = "Expected delivery date cannot be in the past"
    )
    private LocalDate expectedDeliveryDate;

    @Size(
            max = 300,
            message = "Remarks must not exceed 300 characters"
    )
    private String remarks;

    @Valid
    @NotEmpty(
            message = "Purchase order must contain at least one item"
    )
    private List<
            @NotNull(message = "Purchase order item cannot be null")
                    PurchaseOrderItemRequest
            > items;

    public PurchaseOrderCreateRequest() {
    }

    public Long getPurchaseRequestId() {
        return purchaseRequestId;
    }

    public void setPurchaseRequestId(Long purchaseRequestId) {
        this.purchaseRequestId = purchaseRequestId;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public LocalDate getExpectedDeliveryDate() {
        return expectedDeliveryDate;
    }

    public void setExpectedDeliveryDate(
            LocalDate expectedDeliveryDate
    ) {
        this.expectedDeliveryDate = expectedDeliveryDate;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public List<PurchaseOrderItemRequest> getItems() {
        return items;
    }

    public void setItems(
            List<PurchaseOrderItemRequest> items
    ) {
        this.items = items;
    }
}
