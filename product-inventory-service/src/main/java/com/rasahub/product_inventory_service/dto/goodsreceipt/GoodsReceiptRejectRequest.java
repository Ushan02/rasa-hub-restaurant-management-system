
package com.rasahub.product_inventory_service.dto.goodsreceipt;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class GoodsReceiptRejectRequest {

    @NotBlank(
            message = "Rejection reason is required"
    )
    @Size(
            max = 500,
            message = "Rejection reason must not exceed 500 characters"
    )
    private String rejectionReason;

    public GoodsReceiptRejectRequest() {
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
}
