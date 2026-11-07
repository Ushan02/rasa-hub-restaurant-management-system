
package com.rasahub.product_inventory_service.dto.supplieritem;

import jakarta.validation.constraints.NotNull;

public class SupplierItemStatusUpdateRequest {

    @NotNull(message = "Supplier item status is required")
    private Boolean active;

    public SupplierItemStatusUpdateRequest() {
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
