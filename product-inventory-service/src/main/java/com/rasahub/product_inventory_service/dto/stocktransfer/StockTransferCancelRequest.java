
package com.rasahub.product_inventory_service.dto.stocktransfer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class StockTransferCancelRequest {

    @NotBlank(message = "Cancellation reason is required")
    @Size(
            max = 300,
            message = "Cancellation reason must not exceed 300 characters"
    )
    private String cancellationReason;

    public StockTransferCancelRequest() {
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }
}
