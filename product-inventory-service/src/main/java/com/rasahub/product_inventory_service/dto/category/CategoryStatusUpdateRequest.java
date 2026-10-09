package com.rasahub.product_inventory_service.dto.category;

import jakarta.validation.constraints.NotNull;

public class CategoryStatusUpdateRequest {

    @NotNull(message = "Category status is required")
    private Boolean active;

    public CategoryStatusUpdateRequest() {
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}