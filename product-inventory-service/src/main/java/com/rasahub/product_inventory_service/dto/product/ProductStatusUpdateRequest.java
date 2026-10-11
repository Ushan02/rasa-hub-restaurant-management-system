package com.rasahub.product_inventory_service.dto.product;

import jakarta.validation.constraints.NotNull;

public class ProductStatusUpdateRequest {

    @NotNull(message = "Active status is required")
    private Boolean active;

    public ProductStatusUpdateRequest() {
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}