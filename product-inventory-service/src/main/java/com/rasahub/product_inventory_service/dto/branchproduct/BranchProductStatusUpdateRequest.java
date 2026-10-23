package com.rasahub.product_inventory_service.dto.branchproduct;

import jakarta.validation.constraints.NotNull;

public class BranchProductStatusUpdateRequest {

    @NotNull(message = "Available status is required")
    private Boolean available;

    public BranchProductStatusUpdateRequest() {
    }

    public Boolean getAvailable() {
        return available;
    }

    public void setAvailable(Boolean available) {
        this.available = available;
    }
}