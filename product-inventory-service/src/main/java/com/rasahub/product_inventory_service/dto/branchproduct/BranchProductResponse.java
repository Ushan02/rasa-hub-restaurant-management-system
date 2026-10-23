package com.rasahub.product_inventory_service.dto.branchproduct;

import java.time.LocalDateTime;

public class BranchProductResponse {

    private final Long id;

    private final Long branchId;

    private final String productCode;
    private final String productName;

    private final boolean available;

    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public BranchProductResponse(
            Long id,
            Long branchId,
            String productCode,
            String productName,
            boolean available,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.branchId = branchId;
        this.productCode = productCode;
        this.productName = productName;
        this.available = available;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getBranchId() {
        return branchId;
    }

    public String getProductCode() {
        return productCode;
    }

    public String getProductName() {
        return productName;
    }

    public boolean isAvailable() {
        return available;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}