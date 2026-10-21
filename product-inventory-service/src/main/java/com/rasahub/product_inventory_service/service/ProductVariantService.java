package com.rasahub.product_inventory_service.service;

import com.rasahub.product_inventory_service.dto.variant.ProductVariantCreateRequest;
import com.rasahub.product_inventory_service.dto.variant.ProductVariantResponse;
import com.rasahub.product_inventory_service.dto.variant.ProductVariantStatusUpdateRequest;
import com.rasahub.product_inventory_service.dto.variant.ProductVariantUpdateRequest;

import java.util.List;

public interface ProductVariantService {

    ProductVariantResponse createVariant(
            ProductVariantCreateRequest request
    );

    List<ProductVariantResponse> getVariants(
            Boolean active,
            Long productId
    );

    ProductVariantResponse getVariantById(
            Long id
    );

    ProductVariantResponse getVariantByCode(
            String variantCode
    );

    ProductVariantResponse updateVariant(
            Long id,
            ProductVariantUpdateRequest request
    );

    ProductVariantResponse updateVariantStatus(
            Long id,
            ProductVariantStatusUpdateRequest request
    );
}