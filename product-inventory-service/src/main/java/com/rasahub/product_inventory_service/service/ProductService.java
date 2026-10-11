package com.rasahub.product_inventory_service.service;

import com.rasahub.product_inventory_service.dto.product.ProductCreateRequest;
import com.rasahub.product_inventory_service.dto.product.ProductResponse;
import com.rasahub.product_inventory_service.dto.product.ProductStatusUpdateRequest;
import com.rasahub.product_inventory_service.dto.product.ProductUpdateRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProductService {

    ProductResponse createProduct(
            ProductCreateRequest request,
            MultipartFile image
    );

    List<ProductResponse> getProducts(
            Boolean active,
            Long categoryId
    );

    ProductResponse getProductById(
            Long id
    );

    ProductResponse getProductByCode(
            String productCode
    );

    ProductResponse updateProduct(
            Long id,
            ProductUpdateRequest request,
            MultipartFile image
    );

    ProductResponse updateProductStatus(
            Long id,
            ProductStatusUpdateRequest request
    );
}