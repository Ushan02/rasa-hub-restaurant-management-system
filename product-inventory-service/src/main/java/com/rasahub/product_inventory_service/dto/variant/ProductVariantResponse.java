package com.rasahub.product_inventory_service.dto.variant;

import com.rasahub.product_inventory_service.enums.ProductVariantName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ProductVariantResponse {

    private final Long id;
    private final String variantCode;

    private final String productCode;
    private final String productName;

    private final ProductVariantName name;
    private final BigDecimal price;
    private final boolean active;

    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public ProductVariantResponse(
            Long id,
            String variantCode,
            String productCode,
            String productName,
            ProductVariantName name,
            BigDecimal price,
            boolean active,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.variantCode = variantCode;
        this.productCode = productCode;
        this.productName = productName;
        this.name = name;
        this.price = price;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getVariantCode() {
        return variantCode;
    }

    public String getProductCode() {
        return productCode;
    }

    public String getProductName() {
        return productName;
    }

    public ProductVariantName getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public boolean isActive() {
        return active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}