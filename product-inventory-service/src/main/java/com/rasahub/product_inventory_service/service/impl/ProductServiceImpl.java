package com.rasahub.product_inventory_service.service.impl;

import com.rasahub.product_inventory_service.dto.product.ProductCreateRequest;
import com.rasahub.product_inventory_service.dto.product.ProductResponse;
import com.rasahub.product_inventory_service.dto.product.ProductStatusUpdateRequest;
import com.rasahub.product_inventory_service.dto.product.ProductUpdateRequest;
import com.rasahub.product_inventory_service.entity.Category;
import com.rasahub.product_inventory_service.entity.Product;
import com.rasahub.product_inventory_service.exception.BusinessRuleException;
import com.rasahub.product_inventory_service.exception.ResourceNotFoundException;
import com.rasahub.product_inventory_service.repository.CategoryRepository;
import com.rasahub.product_inventory_service.repository.ProductRepository;
import com.rasahub.product_inventory_service.service.ProductService;
import com.rasahub.product_inventory_service.service.SupabaseStorageService;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SupabaseStorageService supabaseStorageService;

    public ProductServiceImpl(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            SupabaseStorageService supabaseStorageService
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.supabaseStorageService = supabaseStorageService;
    }

    @Override
    public ProductResponse createProduct(
            ProductCreateRequest request,
            MultipartFile image
    ) {

        String name =
                normalizeRequiredText(request.getName());

        String description =
                normalizeOptionalText(request.getDescription());

        if (productRepository.existsByNameIgnoreCase(name)) {

            throw new BusinessRuleException(
                    "Product already exists"
            );
        }

        Category category =
                findCategoryById(request.getCategoryId());

        validateCategoryIsActive(category);

        Long sequenceValue =
                productRepository
                        .getNextProductCodeSequenceValue();

        String productCode =
                String.format(
                        "P%03d",
                        sequenceValue
                );

        String imageUrl =
                supabaseStorageService
                        .uploadProductImage(
                                productCode,
                                image
                        );

        Product product =
                new Product(
                        productCode,
                        name,
                        description,
                        imageUrl,
                        category
                );

        try {

            Product savedProduct =
                    productRepository.save(product);

            return toResponse(savedProduct);

        } catch (RuntimeException exception) {

            try {
                supabaseStorageService
                        .deleteProductImage(imageUrl);
            } catch (RuntimeException ignored) {
                // Keep the original database exception.
            }

            throw exception;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getProducts(
            Boolean active,
            Long categoryId
    ) {

        List<Product> products;

        if (active == null && categoryId == null) {

            products =
                    productRepository.findAll(
                            Sort.by(
                                    Sort.Direction.ASC,
                                    "id"
                            )
                    );

        } else if (active != null
                && categoryId == null) {

            products =
                    productRepository
                            .findByActiveOrderByIdAsc(
                                    active
                            );

        } else if (active == null) {

            validateCategoryExists(categoryId);

            products =
                    productRepository
                            .findByCategoryIdOrderByIdAsc(
                                    categoryId
                            );

        } else {

            validateCategoryExists(categoryId);

            products =
                    productRepository
                            .findByCategoryIdAndActiveOrderByIdAsc(
                                    categoryId,
                                    active
                            );
        }

        return products
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(
            Long id
    ) {

        Product product =
                findProductById(id);

        return toResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductByCode(
            String productCode
    ) {

        if (productCode == null
                || productCode.isBlank()) {

            throw new BusinessRuleException(
                    "Product code is required"
            );
        }

        String normalizedProductCode =
                productCode
                        .trim()
                        .toUpperCase(Locale.ROOT);

        Product product =
                productRepository
                        .findByProductCodeIgnoreCase(
                                normalizedProductCode
                        )
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Product not found"
                                        )
                        );

        return toResponse(product);
    }

    @Override
    public ProductResponse updateProduct(
            Long id,
            ProductUpdateRequest request,
            MultipartFile image
    ) {

        Product product =
                findProductById(id);

        String name =
                normalizeRequiredText(request.getName());

        String description =
                normalizeOptionalText(request.getDescription());

        if (productRepository
                .existsByNameIgnoreCaseAndIdNot(
                        name,
                        id
                )) {

            throw new BusinessRuleException(
                    "Product already exists"
            );
        }

        Category category =
                findCategoryById(
                        request.getCategoryId()
                );

        validateCategoryIsActive(category);

        product.setName(name);
        product.setDescription(description);
        product.setCategory(category);

        if (image != null
                && !image.isEmpty()) {

            String newImageUrl =
                    supabaseStorageService
                            .replaceProductImage(
                                    product.getProductCode(),
                                    product.getImageUrl(),
                                    image
                            );

            product.setImageUrl(
                    newImageUrl
            );
        }

        Product updatedProduct =
                productRepository.save(product);

        return toResponse(updatedProduct);
    }

    @Override
    public ProductResponse updateProductStatus(
            Long id,
            ProductStatusUpdateRequest request
    ) {

        Product product =
                findProductById(id);

        boolean requestedStatus =
                request.getActive();

        if (product.isActive()
                == requestedStatus) {

            String status =
                    requestedStatus
                            ? "active"
                            : "inactive";

            throw new BusinessRuleException(
                    "Product is already " + status
            );
        }

        product.setActive(
                requestedStatus
        );

        Product updatedProduct =
                productRepository.save(product);

        return toResponse(updatedProduct);
    }

    private Product findProductById(
            Long id
    ) {

        return productRepository
                .findById(id)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Product not found"
                                )
                );
    }

    private Category findCategoryById(
            Long categoryId
    ) {

        return categoryRepository
                .findById(categoryId)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Category not found"
                                )
                );
    }

    private void validateCategoryExists(
            Long categoryId
    ) {

        if (!categoryRepository
                .existsById(categoryId)) {

            throw new ResourceNotFoundException(
                    "Category not found"
            );
        }
    }

    private void validateCategoryIsActive(
            Category category
    ) {

        if (!category.isActive()) {

            throw new BusinessRuleException(
                    "Cannot create or update product under an inactive category"
            );
        }
    }

    private String normalizeRequiredText(
            String value
    ) {

        return value.trim();
    }

    private String normalizeOptionalText(
            String value
    ) {

        if (value == null) {
            return null;
        }

        String normalized =
                value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }

    private ProductResponse toResponse(
            Product product
    ) {

        Category category =
                product.getCategory();

        return new ProductResponse(
                product.getId(),
                product.getProductCode(),
                product.getName(),
                product.getDescription(),
                product.getImageUrl(),
                category.getId(),
                category.getCategoryCode(),
                category.getName(),
                product.isActive(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}