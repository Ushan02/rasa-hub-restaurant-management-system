package com.rasahub.product_inventory_service.controller;

import com.rasahub.product_inventory_service.dto.branchproduct.BranchProductCreateRequest;
import com.rasahub.product_inventory_service.dto.branchproduct.BranchProductResponse;
import com.rasahub.product_inventory_service.dto.branchproduct.BranchProductStatusUpdateRequest;
import com.rasahub.product_inventory_service.service.BranchProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/branch-products")
public class BranchProductController {

    private final BranchProductService branchProductService;

    public BranchProductController(
            BranchProductService branchProductService
    ) {
        this.branchProductService = branchProductService;
    }

    @PostMapping
    @PreAuthorize(
            "hasAnyRole(" +
                    "'OWNER'," +
                    "'MAIN_OFFICE_MANAGER'," +
                    "'BRANCH_MANAGER'" +
                    ")"
    )
    public ResponseEntity<BranchProductResponse> createBranchProduct(
            @Valid
            @RequestBody
            BranchProductCreateRequest request
    ) {

        BranchProductResponse response =
                branchProductService
                        .createBranchProduct(request);

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
    public ResponseEntity<List<BranchProductResponse>> getBranchProducts(
            @RequestParam(required = false)
            Boolean available,

            @RequestParam(required = false)
            Long branchId,

            @RequestParam(required = false)
            Long productId
    ) {

        return ResponseEntity.ok(
                branchProductService
                        .getBranchProducts(
                                available,
                                branchId,
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
    public ResponseEntity<BranchProductResponse> getBranchProductById(
            @PathVariable
            Long id
    ) {

        return ResponseEntity.ok(
                branchProductService
                        .getBranchProductById(id)
        );
    }

    @GetMapping(
            "/branch/{branchId}/product/{productId}"
    )
    @PreAuthorize(
            "hasAnyRole(" +
                    "'OWNER'," +
                    "'MAIN_OFFICE_MANAGER'," +
                    "'BRANCH_MANAGER'," +
                    "'CASHIER'" +
                    ")"
    )
    public ResponseEntity<BranchProductResponse>
    getBranchProductByBranchAndProduct(
            @PathVariable
            Long branchId,

            @PathVariable
            Long productId
    ) {

        return ResponseEntity.ok(
                branchProductService
                        .getBranchProductByBranchAndProduct(
                                branchId,
                                productId
                        )
        );
    }

    @PatchMapping("/{id}/availability")
    @PreAuthorize(
            "hasAnyRole(" +
                    "'OWNER'," +
                    "'MAIN_OFFICE_MANAGER'," +
                    "'BRANCH_MANAGER'" +
                    ")"
    )
    public ResponseEntity<BranchProductResponse> updateAvailability(
            @PathVariable
            Long id,

            @Valid
            @RequestBody
            BranchProductStatusUpdateRequest request
    ) {

        return ResponseEntity.ok(
                branchProductService
                        .updateAvailability(
                                id,
                                request
                        )
        );
    }
}