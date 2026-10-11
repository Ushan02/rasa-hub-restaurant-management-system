package com.rasahub.product_inventory_service.dto.product;

import java.time.LocalDateTime;

public class ProductResponse {

    private Long id;

    private String productCode;

    private String name;

    private String description;

    private String imageUrl;

    private Long categoryId;

    private String categoryCode;

    private String categoryName;

    private boolean active;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public ProductResponse(
            Long id,
            String productCode,
            String name,
            String description,
            String imageUrl,
            Long categoryId,
            String categoryCode,
            String categoryName,
            boolean active,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.productCode = productCode;
        this.name = name;
        this.description = description;
        this.imageUrl = imageUrl;
        this.categoryId = categoryId;
        this.categoryCode = categoryCode;
        this.categoryName = categoryName;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getProductCode() {
        return productCode;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public String getCategoryName() {
        return categoryName;
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