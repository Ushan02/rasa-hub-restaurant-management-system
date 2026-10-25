package com.rasahub.product_inventory_service.dto.menu;

import java.util.List;

public class MenuItemResponse {

    private final Long productId;

    private final String productCode;

    private final String productName;

    private final String description;

    private final String imageUrl;

    private final String categoryCode;

    private final String categoryName;

    private final Long branchId;

    private final boolean available;

    private final List<MenuVariantResponse> variants;

    public MenuItemResponse(
            Long productId,
            String productCode,
            String productName,
            String description,
            String imageUrl,
            String categoryCode,
            String categoryName,
            Long branchId,
            boolean available,
            List<MenuVariantResponse> variants
    ) {
        this.productId = productId;
        this.productCode = productCode;
        this.productName = productName;
        this.description = description;
        this.imageUrl = imageUrl;
        this.categoryCode = categoryCode;
        this.categoryName = categoryName;
        this.branchId = branchId;
        this.available = available;
        this.variants = variants;
    }

    public Long getProductId() {
        return productId;
    }

    public String getProductCode() {
        return productCode;
    }

    public String getProductName() {
        return productName;
    }

    public String getDescription() {
        return description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public Long getBranchId() {
        return branchId;
    }

    public boolean isAvailable() {
        return available;
    }

    public List<MenuVariantResponse> getVariants() {
        return variants;
    }
}