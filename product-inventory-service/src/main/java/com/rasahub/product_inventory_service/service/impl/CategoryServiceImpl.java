package com.rasahub.product_inventory_service.service.impl;

import com.rasahub.product_inventory_service.dto.category.CategoryCreateRequest;
import com.rasahub.product_inventory_service.dto.category.CategoryResponse;
import com.rasahub.product_inventory_service.dto.category.CategoryStatusUpdateRequest;
import com.rasahub.product_inventory_service.dto.category.CategoryUpdateRequest;
import com.rasahub.product_inventory_service.entity.Category;
import com.rasahub.product_inventory_service.exception.BusinessRuleException;
import com.rasahub.product_inventory_service.exception.ResourceNotFoundException;
import com.rasahub.product_inventory_service.repository.CategoryRepository;
import com.rasahub.product_inventory_service.service.CategoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public CategoryResponse createCategory(CategoryCreateRequest request) {

        String name = request.getName().trim();
        String description = normalizeOptionalText(request.getDescription());

        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new BusinessRuleException(
                    "Category already exists"
            );
        }

        Long sequenceValue =
                categoryRepository.getNextCategoryCodeSequenceValue();

        String categoryCode =
                String.format("CAT%03d", sequenceValue);

        Category category = new Category(
                categoryCode,
                name,
                description
        );

        Category savedCategory =
                categoryRepository.save(category);

        return toResponse(savedCategory);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategories(Boolean active) {

        List<Category> categories;

        if (active == null) {
            categories = categoryRepository.findAll();
        } else {
            categories =
                    categoryRepository.findByActiveOrderByIdAsc(active);
        }

        return categories.stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {

        Category category = findCategoryById(id);

        return toResponse(category);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryByCode(String categoryCode) {

        String normalizedCode =
                categoryCode.trim().toUpperCase();

        Category category = categoryRepository
                .findByCategoryCodeIgnoreCase(normalizedCode)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Category not found"
                        )
                );

        return toResponse(category);
    }

    @Override
    public CategoryResponse updateCategory(
            Long id,
            CategoryUpdateRequest request
    ) {

        Category category = findCategoryById(id);

        String name = request.getName().trim();
        String description =
                normalizeOptionalText(request.getDescription());

        if (categoryRepository
                .existsByNameIgnoreCaseAndIdNot(name, id)) {

            throw new BusinessRuleException(
                    "Category already exists"
            );
        }

        category.setName(name);
        category.setDescription(description);

        Category updatedCategory =
                categoryRepository.save(category);

        return toResponse(updatedCategory);
    }

    @Override
    public CategoryResponse updateCategoryStatus(
            Long id,
            CategoryStatusUpdateRequest request
    ) {

        Category category = findCategoryById(id);

        boolean requestedStatus = request.getActive();

        if (category.isActive() == requestedStatus) {

            String statusText =
                    requestedStatus ? "active" : "inactive";

            throw new BusinessRuleException(
                    "Category is already " + statusText
            );
        }

        category.setActive(requestedStatus);

        Category updatedCategory =
                categoryRepository.save(category);

        return toResponse(updatedCategory);
    }

    private Category findCategoryById(Long id) {

        return categoryRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Category not found"
                        )
                );
    }

    private String normalizeOptionalText(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private CategoryResponse toResponse(Category category) {

        return new CategoryResponse(
                category.getId(),
                category.getCategoryCode(),
                category.getName(),
                category.getDescription(),
                category.isActive(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }
}