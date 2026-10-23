package com.rasahub.product_inventory_service.service;

import com.rasahub.product_inventory_service.dto.branchproduct.BranchProductCreateRequest;
import com.rasahub.product_inventory_service.dto.branchproduct.BranchProductResponse;
import com.rasahub.product_inventory_service.dto.branchproduct.BranchProductStatusUpdateRequest;

import java.util.List;

public interface BranchProductService {

    BranchProductResponse createBranchProduct(
            BranchProductCreateRequest request
    );

    List<BranchProductResponse> getBranchProducts(
            Boolean available,
            Long branchId,
            Long productId
    );

    BranchProductResponse getBranchProductById(
            Long id
    );

    BranchProductResponse getBranchProductByBranchAndProduct(
            Long branchId,
            Long productId
    );

    BranchProductResponse updateAvailability(
            Long id,
            BranchProductStatusUpdateRequest request
    );
}