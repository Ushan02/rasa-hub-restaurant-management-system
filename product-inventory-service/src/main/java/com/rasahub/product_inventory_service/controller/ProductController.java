package com.rasahub.product_inventory_service.controller;

import com.rasahub.product_inventory_service.dto.product.ProductCreateRequest;
import com.rasahub.product_inventory_service.dto.product.ProductResponse;
import com.rasahub.product_inventory_service.dto.product.ProductStatusUpdateRequest;
import com.rasahub.product_inventory_service.dto.product.ProductUpdateRequest;
import com.rasahub.product_inventory_service.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(
            ProductService productService
    ) {
        this.productService = productService;
    }

    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize(
            "hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER')"
    )
    public ResponseEntity<ProductResponse> createProduct(
            @Valid
            @RequestPart("product")
            ProductCreateRequest request,

            @RequestPart("image")
            MultipartFile image
    ) {

        ProductResponse response =
                productService.createProduct(
                        request,
                        image
                );

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
    public ResponseEntity<List<ProductResponse>>
    getProducts(
            @RequestParam(required = false)
            Boolean active,

            @RequestParam(required = false)
            Long categoryId
    ) {

        return ResponseEntity.ok(
                productService.getProducts(
                        active,
                        categoryId
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
    public ResponseEntity<ProductResponse>
    getProductById(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                productService.getProductById(
                        id
                )
        );
    }

    @GetMapping("/by-code/{productCode}")
    @PreAuthorize(
            "hasAnyRole(" +
                    "'OWNER'," +
                    "'MAIN_OFFICE_MANAGER'," +
                    "'BRANCH_MANAGER'," +
                    "'CASHIER'" +
                    ")"
    )
    public ResponseEntity<ProductResponse>
    getProductByCode(
            @PathVariable String productCode
    ) {

        return ResponseEntity.ok(
                productService.getProductByCode(
                        productCode
                )
        );
    }

    @PutMapping(
            value = "/{id}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize(
            "hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER')"
    )
    public ResponseEntity<ProductResponse>
    updateProduct(
            @PathVariable Long id,

            @Valid
            @RequestPart("product")
            ProductUpdateRequest request,

            @RequestPart(
                    value = "image",
                    required = false
            )
            MultipartFile image
    ) {

        return ResponseEntity.ok(
                productService.updateProduct(
                        id,
                        request,
                        image
                )
        );
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize(
            "hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER')"
    )
    public ResponseEntity<ProductResponse>
    updateProductStatus(
            @PathVariable Long id,

            @Valid
            @RequestBody
            ProductStatusUpdateRequest request
    ) {

        return ResponseEntity.ok(
                productService.updateProductStatus(
                        id,
                        request
                )
        );
    }
}