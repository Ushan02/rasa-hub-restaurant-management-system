package com.rasahub.product_inventory_service.controller;

import com.rasahub.product_inventory_service.dto.variant.ProductVariantCreateRequest;
import com.rasahub.product_inventory_service.dto.variant.ProductVariantResponse;
import com.rasahub.product_inventory_service.dto.variant.ProductVariantStatusUpdateRequest;
import com.rasahub.product_inventory_service.dto.variant.ProductVariantUpdateRequest;
import com.rasahub.product_inventory_service.service.ProductVariantService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/product-variants")
public class ProductVariantController {

    private final ProductVariantService productVariantService;

    public ProductVariantController(
            ProductVariantService productVariantService
    ) {
        this.productVariantService =
                productVariantService;
    }

    @PostMapping
    @PreAuthorize(
            "hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER')"
    )
    public ResponseEntity<ProductVariantResponse> createVariant(
            @Valid
            @RequestBody
            ProductVariantCreateRequest request
    ) {

        ProductVariantResponse response =
                productVariantService
                        .createVariant(request);

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
    public ResponseEntity<List<ProductVariantResponse>>
    getVariants(
            @RequestParam(required = false)
            Boolean active,

            @RequestParam(required = false)
            Long productId
    ) {

        return ResponseEntity.ok(
                productVariantService
                        .getVariants(
                                active,
                                productId
                        )
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
    public ResponseEntity<ProductVariantResponse>
    getVariantById(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                productVariantService
                        .getVariantById(id)
        );
    }

    @GetMapping("/by-code/{variantCode}")
    @PreAuthorize(
            "hasAnyRole(" +
                    "'OWNER'," +
                    "'MAIN_OFFICE_MANAGER'," +
                    "'BRANCH_MANAGER'," +
                    "'CASHIER'" +
                    ")"
    )
    public ResponseEntity<ProductVariantResponse>
    getVariantByCode(
            @PathVariable
            String variantCode
    ) {

        return ResponseEntity.ok(
                productVariantService
                        .getVariantByCode(
                                variantCode
                        )
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize(
            "hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER')"
    )
    public ResponseEntity<ProductVariantResponse>
    updateVariant(
            @PathVariable Long id,

            @Valid
            @RequestBody
            ProductVariantUpdateRequest request
    ) {

        return ResponseEntity.ok(
                productVariantService
                        .updateVariant(
                                id,
                                request
                        )
        );
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize(
            "hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER')"
    )
    public ResponseEntity<ProductVariantResponse>
    updateVariantStatus(
            @PathVariable Long id,

            @Valid
            @RequestBody
            ProductVariantStatusUpdateRequest request
    ) {

        return ResponseEntity.ok(
                productVariantService
                        .updateVariantStatus(
                                id,
                                request
                        )
        );
    }
}