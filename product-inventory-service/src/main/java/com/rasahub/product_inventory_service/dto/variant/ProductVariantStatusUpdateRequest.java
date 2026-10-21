package com.rasahub.product_inventory_service.dto.variant;

import jakarta.validation.constraints.NotNull;

public class ProductVariantStatusUpdateRequest {

    @NotNull(message = "Active status is required")
    private Boolean active;

    public ProductVariantStatusUpdateRequest() {
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}