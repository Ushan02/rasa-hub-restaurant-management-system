package com.rasahub.product_inventory_service.dto.branchproduct;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class BranchProductCreateRequest {

    @NotNull(message = "Branch ID is required")
    @Positive(message = "Branch ID must be positive")
    private Long branchId;

    @NotNull(message = "Product ID is required")
    @Positive(message = "Product ID must be positive")
    private Long productId;

    public BranchProductCreateRequest() {
    }

    public Long getBranchId() {
        return branchId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setBranchId(Long branchId) {
        this.branchId = branchId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }
}