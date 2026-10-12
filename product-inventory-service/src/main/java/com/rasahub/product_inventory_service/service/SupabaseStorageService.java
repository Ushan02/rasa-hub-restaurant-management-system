package com.rasahub.product_inventory_service.service;

import org.springframework.web.multipart.MultipartFile;

public interface SupabaseStorageService {

    String uploadProductImage(
            String productCode,
            MultipartFile image
    );

    String replaceProductImage(
            String productCode,
            String oldImageUrl,
            MultipartFile newImage
    );

    void deleteProductImage(
            String imageUrl
    );
}