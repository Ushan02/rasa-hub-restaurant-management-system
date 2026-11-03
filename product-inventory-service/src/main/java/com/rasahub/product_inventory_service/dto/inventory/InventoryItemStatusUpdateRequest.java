
package com.rasahub.product_inventory_service.dto.inventory;

import jakarta.validation.constraints.NotNull;

public class InventoryItemStatusUpdateRequest {

    @NotNull(message = "Inventory item status is required")
    private Boolean active;

    public InventoryItemStatusUpdateRequest() {
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
