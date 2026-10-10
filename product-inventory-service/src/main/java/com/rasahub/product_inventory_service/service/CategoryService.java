package com.rasahub.product_inventory_service.service;

import com.rasahub.product_inventory_service.dto.category.CategoryCreateRequest;
import com.rasahub.product_inventory_service.dto.category.CategoryResponse;
import com.rasahub.product_inventory_service.dto.category.CategoryStatusUpdateRequest;
import com.rasahub.product_inventory_service.dto.category.CategoryUpdateRequest;

import java.util.List;

public interface CategoryService {

    CategoryResponse createCategory(CategoryCreateRequest request);

    List<CategoryResponse> getCategories(Boolean active);

    CategoryResponse getCategoryById(Long id);

    CategoryResponse getCategoryByCode(String categoryCode);

    CategoryResponse updateCategory(
            Long id,
            CategoryUpdateRequest request
    );

    CategoryResponse updateCategoryStatus(
            Long id,
            CategoryStatusUpdateRequest request
    );
}