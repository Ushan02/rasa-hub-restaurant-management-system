
package com.rasahub.product_inventory_service.dto.stocklocation;

import jakarta.validation.constraints.NotNull;

public class StockLocationStatusUpdateRequest {

    @NotNull(message = "Stock location status is required")
    private Boolean active;

    public StockLocationStatusUpdateRequest() {
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
