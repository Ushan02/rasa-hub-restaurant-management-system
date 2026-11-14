
package com.rasahub.product_inventory_service.dto.purchaserequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public class PurchaseRequestCreateRequest {

    @Size(
            max = 300,
            message = "Remarks must not exceed 300 characters"
    )
    private String remarks;

    @Valid
    @NotEmpty(
            message = "Purchase request must contain at least one item"
    )
    private List<PurchaseRequestItemRequest> items;

    public PurchaseRequestCreateRequest() {
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public List<PurchaseRequestItemRequest> getItems() {
        return items;
    }

    public void setItems(List<PurchaseRequestItemRequest> items) {
        this.items = items;
    }
}
