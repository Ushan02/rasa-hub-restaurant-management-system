package com.rasahub.product_inventory_service.dto.variant;

import com.rasahub.product_inventory_service.enums.ProductVariantName;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class ProductVariantCreateRequest {

    @NotNull(message = "Product ID is required")
    @Positive(message = "Product ID must be positive")
    private Long productId;

    @NotNull(message = "Variant name is required")
    private ProductVariantName name;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be greater than zero")
    @Digits(
            integer = 8,
            fraction = 2,
            message = "Price must have at most 8 integer digits and 2 decimal places"
    )
    private BigDecimal price;

    public ProductVariantCreateRequest() {
    }

    public Long getProductId() {
        return productId;
    }

    public ProductVariantName getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public void setName(ProductVariantName name) {
        this.name = name;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }
}