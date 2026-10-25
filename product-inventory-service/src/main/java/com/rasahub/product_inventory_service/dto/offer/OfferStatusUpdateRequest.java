package com.rasahub.product_inventory_service.dto.offer;

import jakarta.validation.constraints.NotNull;

public class OfferStatusUpdateRequest {

    @NotNull(message = "Active status is required")
    private Boolean active;

    public OfferStatusUpdateRequest() {
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}