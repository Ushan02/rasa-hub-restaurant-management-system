package com.rasahub.product_inventory_service.dto.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ProductCreateRequest {

    @NotBlank(message = "Product name is required")
    @Size(
            max = 150,
            message = "Product name must not exceed 150 characters"
    )
    private String name;

    @Size(
            max = 500,
            message = "Description must not exceed 500 characters"
    )
    private String description;

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    public ProductCreateRequest() {
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }
}