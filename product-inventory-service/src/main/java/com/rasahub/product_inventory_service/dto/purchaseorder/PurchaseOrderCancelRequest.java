
package com.rasahub.product_inventory_service.dto.purchaseorder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PurchaseOrderCancelRequest {

    @NotBlank(
            message = "Cancellation reason is required"
    )
    @Size(
            max = 300,
            message = "Cancellation reason must not exceed 300 characters"
    )
    private String cancellationReason;

    public PurchaseOrderCancelRequest() {
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(
            String cancellationReason
    ) {
        this.cancellationReason = cancellationReason;
    }
}
