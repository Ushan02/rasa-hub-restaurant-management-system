package com.rasahub.product_inventory_service.service.impl;

import com.rasahub.product_inventory_service.dto.variant.ProductVariantCreateRequest;
import com.rasahub.product_inventory_service.dto.variant.ProductVariantResponse;
import com.rasahub.product_inventory_service.dto.variant.ProductVariantStatusUpdateRequest;
import com.rasahub.product_inventory_service.dto.variant.ProductVariantUpdateRequest;
import com.rasahub.product_inventory_service.entity.Product;
import com.rasahub.product_inventory_service.entity.ProductVariant;
import com.rasahub.product_inventory_service.enums.ProductVariantName;
import com.rasahub.product_inventory_service.exception.BusinessRuleException;
import com.rasahub.product_inventory_service.exception.ResourceNotFoundException;
import com.rasahub.product_inventory_service.repository.ProductRepository;
import com.rasahub.product_inventory_service.repository.ProductVariantRepository;
import com.rasahub.product_inventory_service.service.ProductVariantService;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class ProductVariantServiceImpl
        implements ProductVariantService {

    private final ProductVariantRepository productVariantRepository;
    private final ProductRepository productRepository;

    public ProductVariantServiceImpl(
            ProductVariantRepository productVariantRepository,
            ProductRepository productRepository
    ) {
        this.productVariantRepository =
                productVariantRepository;

        this.productRepository =
                productRepository;
    }

    @Override
    public ProductVariantResponse createVariant(
            ProductVariantCreateRequest request
    ) {

        Product product =
                findProductById(
                        request.getProductId()
                );

        validateProductIsActive(product);

        ProductVariantName variantName =
                request.getName();

        if (productVariantRepository
                .existsByProductIdAndName(
                        product.getId(),
                        variantName
                )) {

            throw new BusinessRuleException(
                    "Product variant already exists"
            );
        }

        validateVariantCombination(
                product.getId(),
                variantName,
                null
        );

        Long sequenceValue =
                productVariantRepository
                        .getNextVariantCodeSequenceValue();

        String variantCode =
                String.format(
                        "V%03d",
                        sequenceValue
                );

        ProductVariant variant =
                new ProductVariant(
                        variantCode,
                        product,
                        variantName,
                        request.getPrice()
                );

        ProductVariant savedVariant =
                productVariantRepository
                        .save(variant);

        return toResponse(
                savedVariant
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductVariantResponse> getVariants(
            Boolean active,
            Long productId
    ) {

        List<ProductVariant> variants;

        if (active == null
                && productId == null) {

            variants =
                    productVariantRepository
                            .findAll(
                                    Sort.by(
                                            Sort.Direction.ASC,
                                            "id"
                                    )
                            );

        } else if (active != null
                && productId == null) {

            variants =
                    productVariantRepository
                            .findByActiveOrderByIdAsc(
                                    active
                            );

        } else if (active == null) {

            validateProductExists(
                    productId
            );

            variants =
                    productVariantRepository
                            .findByProductIdOrderByIdAsc(
                                    productId
                            );

        } else {

            validateProductExists(
                    productId
            );

            variants =
                    productVariantRepository
                            .findByProductIdAndActiveOrderByIdAsc(
                                    productId,
                                    active
                            );
        }

        return variants
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductVariantResponse getVariantById(
            Long id
    ) {

        ProductVariant variant =
                findVariantById(id);

        return toResponse(
                variant
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ProductVariantResponse getVariantByCode(
            String variantCode
    ) {

        if (variantCode == null
                || variantCode.isBlank()) {

            throw new BusinessRuleException(
                    "Variant code is required"
            );
        }

        String normalizedVariantCode =
                variantCode
                        .trim()
                        .toUpperCase(
                                Locale.ROOT
                        );

        ProductVariant variant =
                productVariantRepository
                        .findByVariantCodeIgnoreCase(
                                normalizedVariantCode
                        )
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Product variant not found"
                                        )
                        );

        return toResponse(
                variant
        );
    }

    @Override
    public ProductVariantResponse updateVariant(
            Long id,
            ProductVariantUpdateRequest request
    ) {

        ProductVariant variant =
                findVariantById(id);

        Product product =
                variant.getProduct();

        validateProductIsActive(
                product
        );

        ProductVariantName requestedName =
                request.getName();

        if (productVariantRepository
                .existsByProductIdAndNameAndIdNot(
                        product.getId(),
                        requestedName,
                        id
                )) {

            throw new BusinessRuleException(
                    "Product variant already exists"
            );
        }

        if (variant.isActive()) {

            validateVariantCombination(
                    product.getId(),
                    requestedName,
                    variant.getId()
            );
        }

        variant.setName(
                requestedName
        );

        variant.setPrice(
                request.getPrice()
        );

        ProductVariant updatedVariant =
                productVariantRepository
                        .save(variant);

        return toResponse(
                updatedVariant
        );
    }

    @Override
    public ProductVariantResponse updateVariantStatus(
            Long id,
            ProductVariantStatusUpdateRequest request
    ) {

        ProductVariant variant =
                findVariantById(id);

        boolean requestedStatus =
                request.getActive();

        if (variant.isActive()
                == requestedStatus) {

            String status =
                    requestedStatus
                            ? "active"
                            : "inactive";

            throw new BusinessRuleException(
                    "Product variant is already "
                            + status
            );
        }

        if (requestedStatus) {

            Product product =
                    variant.getProduct();

            validateProductIsActive(
                    product
            );

            validateVariantCombination(
                    product.getId(),
                    variant.getName(),
                    variant.getId()
            );
        }

        variant.setActive(
                requestedStatus
        );

        ProductVariant updatedVariant =
                productVariantRepository
                        .save(variant);

        return toResponse(
                updatedVariant
        );
    }

    private ProductVariant findVariantById(
            Long id
    ) {

        return productVariantRepository
                .findById(id)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Product variant not found"
                                )
                );
    }

    private Product findProductById(
            Long productId
    ) {

        return productRepository
                .findById(productId)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Product not found"
                                )
                );
    }

    private void validateProductExists(
            Long productId
    ) {

        if (!productRepository
                .existsById(productId)) {

            throw new ResourceNotFoundException(
                    "Product not found"
            );
        }
    }

    private void validateProductIsActive(
            Product product
    ) {

        if (!product.isActive()) {

            throw new BusinessRuleException(
                    "Cannot create, update, or activate a variant for an inactive product"
            );
        }
    }

    private void validateVariantCombination(
            Long productId,
            ProductVariantName requestedName,
            Long excludedVariantId
    ) {

        List<ProductVariant> activeVariants =
                productVariantRepository
                        .findByProductIdAndActiveOrderByIdAsc(
                                productId,
                                true
                        );

        List<ProductVariant> otherActiveVariants =
                activeVariants
                        .stream()
                        .filter(
                                variant ->
                                        excludedVariantId == null
                                                || !variant.getId()
                                                .equals(excludedVariantId)
                        )
                        .toList();

        boolean hasStandard =
                otherActiveVariants
                        .stream()
                        .anyMatch(
                                variant ->
                                        variant.getName()
                                                == ProductVariantName.STANDARD
                        );

        boolean hasSizedVariant =
                otherActiveVariants
                        .stream()
                        .anyMatch(
                                variant ->
                                        variant.getName()
                                                == ProductVariantName.REGULAR
                                                || variant.getName()
                                                == ProductVariantName.LARGE
                        );

        if (requestedName
                == ProductVariantName.STANDARD
                && hasSizedVariant) {

            throw new BusinessRuleException(
                    "STANDARD variant cannot be combined with REGULAR or LARGE variants"
            );
        }

        if ((requestedName
                == ProductVariantName.REGULAR
                || requestedName
                == ProductVariantName.LARGE)
                && hasStandard) {

            throw new BusinessRuleException(
                    "REGULAR or LARGE variant cannot be combined with STANDARD variant"
            );
        }
    }

    private ProductVariantResponse toResponse(
            ProductVariant variant
    ) {

        Product product =
                variant.getProduct();

        return new ProductVariantResponse(
                variant.getId(),
                variant.getVariantCode(),
                product.getProductCode(),
                product.getName(),
                variant.getName(),
                variant.getPrice(),
                variant.isActive(),
                variant.getCreatedAt(),
                variant.getUpdatedAt()
        );
    }
}