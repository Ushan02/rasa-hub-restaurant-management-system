package com.rasahub.product_inventory_service.controller;

import com.rasahub.product_inventory_service.dto.category.CategoryCreateRequest;
import com.rasahub.product_inventory_service.dto.category.CategoryResponse;
import com.rasahub.product_inventory_service.dto.category.CategoryStatusUpdateRequest;
import com.rasahub.product_inventory_service.dto.category.CategoryUpdateRequest;
import com.rasahub.product_inventory_service.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(
            CategoryService categoryService
    ) {
        this.categoryService = categoryService;
    }

    @PostMapping
    @PreAuthorize(
            "hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER')"
    )
    public ResponseEntity<CategoryResponse> createCategory(
            @Valid
            @RequestBody
            CategoryCreateRequest request
    ) {

        CategoryResponse response =
                categoryService.createCategory(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    @PreAuthorize(
            "hasAnyRole(" +
                    "'OWNER'," +
                    "'MAIN_OFFICE_MANAGER'," +
                    "'BRANCH_MANAGER'," +
                    "'CASHIER'" +
                    ")"
    )
    public ResponseEntity<List<CategoryResponse>>
    getCategories(
            @RequestParam(required = false)
            Boolean active
    ) {

        return ResponseEntity.ok(
                categoryService.getCategories(active)
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize(
            "hasAnyRole(" +
                    "'OWNER'," +
                    "'MAIN_OFFICE_MANAGER'," +
                    "'BRANCH_MANAGER'," +
                    "'CASHIER'" +
                    ")"
    )
    public ResponseEntity<CategoryResponse>
    getCategoryById(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                categoryService.getCategoryById(id)
        );
    }

    @GetMapping("/by-code/{categoryCode}")
    @PreAuthorize(
            "hasAnyRole(" +
                    "'OWNER'," +
                    "'MAIN_OFFICE_MANAGER'," +
                    "'BRANCH_MANAGER'," +
                    "'CASHIER'" +
                    ")"
    )
    public ResponseEntity<CategoryResponse>
    getCategoryByCode(
            @PathVariable String categoryCode
    ) {

        return ResponseEntity.ok(
                categoryService.getCategoryByCode(
                        categoryCode
                )
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize(
            "hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER')"
    )
    public ResponseEntity<CategoryResponse>
    updateCategory(
            @PathVariable Long id,
            @Valid
            @RequestBody
            CategoryUpdateRequest request
    ) {

        return ResponseEntity.ok(
                categoryService.updateCategory(
                        id,
                        request
                )
        );
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize(
            "hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER')"
    )
    public ResponseEntity<CategoryResponse>
    updateCategoryStatus(
            @PathVariable Long id,
            @Valid
            @RequestBody
            CategoryStatusUpdateRequest request
    ) {

        return ResponseEntity.ok(
                categoryService.updateCategoryStatus(
                        id,
                        request
                )
        );
    }
}