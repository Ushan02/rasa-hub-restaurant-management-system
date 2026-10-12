package com.rasahub.product_inventory_service.service.impl;

import com.rasahub.product_inventory_service.exception.BusinessRuleException;
import com.rasahub.product_inventory_service.service.SupabaseStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.UUID;

@Service
public class SupabaseStorageServiceImpl
        implements SupabaseStorageService {

    private static final long MAX_IMAGE_SIZE =
            5L * 1024 * 1024;

    private final String supabaseUrl;
    private final String secretKey;
    private final String bucket;
    private final RestClient restClient;

    public SupabaseStorageServiceImpl(
            @Value("${supabase.url}")
            String supabaseUrl,

            @Value("${supabase.secret-key}")
            String secretKey,

            @Value("${supabase.storage.bucket}")
            String bucket
    ) {

        this.supabaseUrl =
                removeTrailingSlash(supabaseUrl);

        this.secretKey = secretKey;
        this.bucket = bucket;

        this.restClient =
                RestClient.builder()
                        .baseUrl(
                                this.supabaseUrl
                                        + "/storage/v1"
                        )
                        .defaultHeader(
                                "apikey",
                                this.secretKey
                        )
                        .build();
    }

    @Override
    public String uploadProductImage(
            String productCode,
            MultipartFile image
    ) {

        validateImage(image);

        String contentType =
                image.getContentType();

        String extension =
                getFileExtension(contentType);

        String objectPath =
                productCode.trim().toUpperCase(Locale.ROOT)
                        + "/"
                        + UUID.randomUUID()
                        + extension;

        try {

            restClient
                    .post()
                    .uri(
                            "/object/"
                                    + bucket
                                    + "/"
                                    + objectPath
                    )
                    .contentType(
                            MediaType.parseMediaType(
                                    contentType
                            )
                    )
                    .header(
                            "x-upsert",
                            "false"
                    )
                    .body(
                            image.getBytes()
                    )
                    .retrieve()
                    .toBodilessEntity();

            return buildPublicUrl(
                    objectPath
            );

        } catch (IOException exception) {

            throw new IllegalStateException(
                    "Could not read product image",
                    exception
            );

        } catch (RestClientResponseException exception) {

            throw new IllegalStateException(
                    "Failed to upload product image to Supabase",
                    exception
            );
        }
    }

    @Override
    public String replaceProductImage(
            String productCode,
            String oldImageUrl,
            MultipartFile newImage
    ) {

        String newImageUrl =
                uploadProductImage(
                        productCode,
                        newImage
                );

        if (oldImageUrl != null
                && !oldImageUrl.isBlank()) {

            deleteProductImage(
                    oldImageUrl
            );
        }

        return newImageUrl;
    }

    @Override
    public void deleteProductImage(
            String imageUrl
    ) {

        if (imageUrl == null
                || imageUrl.isBlank()) {
            return;
        }

        String objectPath =
                extractObjectPath(imageUrl);

        try {

            restClient
                    .delete()
                    .uri(
                            "/object/"
                                    + bucket
                                    + "/"
                                    + objectPath
                    )
                    .retrieve()
                    .toBodilessEntity();

        } catch (RestClientResponseException exception) {

            throw new IllegalStateException(
                    "Failed to delete product image from Supabase",
                    exception
            );
        }
    }

    private void validateImage(
            MultipartFile image
    ) {

        if (image == null
                || image.isEmpty()) {

            throw new BusinessRuleException(
                    "Product image is required"
            );
        }

        if (image.getSize() > MAX_IMAGE_SIZE) {

            throw new BusinessRuleException(
                    "Product image must not exceed 5 MB"
            );
        }

        String contentType =
                image.getContentType();

        if (contentType == null) {

            throw new BusinessRuleException(
                    "Invalid product image type"
            );
        }

        String normalizedContentType =
                contentType.toLowerCase(
                        Locale.ROOT
                );

        boolean supported =
                normalizedContentType.equals(
                        "image/jpeg"
                )
                        || normalizedContentType.equals(
                        "image/jpg"
                )
                        || normalizedContentType.equals(
                        "image/png"
                )
                        || normalizedContentType.equals(
                        "image/webp"
                );

        if (!supported) {

            throw new BusinessRuleException(
                    "Only JPG, PNG, and WEBP images are allowed"
            );
        }
    }

    private String getFileExtension(
            String contentType
    ) {

        String normalizedContentType =
                contentType.toLowerCase(
                        Locale.ROOT
                );

        return switch (normalizedContentType) {

            case "image/jpeg",
                 "image/jpg" -> ".jpg";

            case "image/png" -> ".png";

            case "image/webp" -> ".webp";

            default ->
                    throw new BusinessRuleException(
                            "Unsupported image type"
                    );
        };
    }

    private String buildPublicUrl(
            String objectPath
    ) {

        return supabaseUrl
                + "/storage/v1/object/public/"
                + bucket
                + "/"
                + objectPath;
    }

    private String extractObjectPath(
            String imageUrl
    ) {

        String publicUrlPrefix =
                supabaseUrl
                        + "/storage/v1/object/public/"
                        + bucket
                        + "/";

        if (!imageUrl.startsWith(
                publicUrlPrefix
        )) {

            throw new IllegalStateException(
                    "Invalid Supabase product image URL"
            );
        }

        return imageUrl.substring(
                publicUrlPrefix.length()
        );
    }

    private String removeTrailingSlash(
            String value
    ) {

        if (value.endsWith("/")) {

            return value.substring(
                    0,
                    value.length() - 1
            );
        }

        return value;
    }
}