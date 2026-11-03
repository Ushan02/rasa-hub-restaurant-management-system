
package com.rasahub.product_inventory_service.dto.supplier;

import jakarta.validation.constraints.NotNull;

public class SupplierStatusUpdateRequest {

    @NotNull(message = "Supplier status is required")
    private Boolean active;

    public SupplierStatusUpdateRequest() {
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
