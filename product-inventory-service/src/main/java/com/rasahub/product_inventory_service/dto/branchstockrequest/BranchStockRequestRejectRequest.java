
package com.rasahub.product_inventory_service.dto.branchstockrequest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class BranchStockRequestRejectRequest {

    @NotBlank(message = "Rejection reason is required")
    @Size(
            max = 500,
            message = "Rejection reason must not exceed 500 characters"
    )
    private String rejectionReason;

    public BranchStockRequestRejectRequest() {
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
}
