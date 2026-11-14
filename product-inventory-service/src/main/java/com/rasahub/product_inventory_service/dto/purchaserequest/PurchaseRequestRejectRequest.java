
package com.rasahub.product_inventory_service.dto.purchaserequest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PurchaseRequestRejectRequest {

    @NotBlank(message = "Rejection reason is required")
    @Size(
            max = 500,
            message = "Rejection reason must not exceed 500 characters"
    )
    private String rejectionReason;

    public PurchaseRequestRejectRequest() {
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
}
