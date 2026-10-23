package com.rasahub.product_inventory_service.service.impl;

import com.rasahub.product_inventory_service.dto.branchproduct.BranchProductCreateRequest;
import com.rasahub.product_inventory_service.dto.branchproduct.BranchProductResponse;
import com.rasahub.product_inventory_service.dto.branchproduct.BranchProductStatusUpdateRequest;
import com.rasahub.product_inventory_service.entity.BranchProduct;
import com.rasahub.product_inventory_service.entity.Product;
import com.rasahub.product_inventory_service.exception.BusinessRuleException;
import com.rasahub.product_inventory_service.exception.ResourceNotFoundException;
import com.rasahub.product_inventory_service.repository.BranchProductRepository;
import com.rasahub.product_inventory_service.repository.ProductRepository;
import com.rasahub.product_inventory_service.service.BranchProductService;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class BranchProductServiceImpl
        implements BranchProductService {

    private final BranchProductRepository branchProductRepository;
    private final ProductRepository productRepository;

    public BranchProductServiceImpl(
            BranchProductRepository branchProductRepository,
            ProductRepository productRepository
    ) {
        this.branchProductRepository = branchProductRepository;
        this.productRepository = productRepository;
    }

    @Override
    public BranchProductResponse createBranchProduct(
            BranchProductCreateRequest request
    ) {

        Product product =
                findProductById(
                        request.getProductId()
                );

        validateProductIsActive(product);

        if (branchProductRepository
                .existsByBranchIdAndProductId(
                        request.getBranchId(),
                        product.getId()
                )) {

            throw new BusinessRuleException(
                    "Product is already assigned to this branch"
            );
        }

        BranchProduct branchProduct =
                new BranchProduct(
                        request.getBranchId(),
                        product
                );

        BranchProduct savedBranchProduct =
                branchProductRepository
                        .save(branchProduct);

        return toResponse(
                savedBranchProduct
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<BranchProductResponse> getBranchProducts(
            Boolean available,
            Long branchId,
            Long productId
    ) {

        if (productId != null) {
            validateProductExists(productId);
        }

        List<BranchProduct> branchProducts;

        if (branchId == null
                && productId == null
                && available == null) {

            branchProducts =
                    branchProductRepository
                            .findAll(
                                    Sort.by(
                                            Sort.Direction.ASC,
                                            "id"
                                    )
                            );

        } else if (branchId != null
                && productId != null) {

            branchProducts =
                    branchProductRepository
                            .findByBranchIdAndProductId(
                                    branchId,
                                    productId
                            )
                            .filter(
                                    branchProduct ->
                                            available == null
                                                    || branchProduct.isAvailable()
                                                    == available
                            )
                            .map(List::of)
                            .orElseGet(List::of);

        } else if (branchId != null) {

            if (available == null) {

                branchProducts =
                        branchProductRepository
                                .findByBranchIdOrderByIdAsc(
                                        branchId
                                );

            } else {

                branchProducts =
                        branchProductRepository
                                .findByBranchIdAndAvailableOrderByIdAsc(
                                        branchId,
                                        available
                                );
            }

        } else if (productId != null) {

            branchProducts =
                    branchProductRepository
                            .findByProductIdOrderByIdAsc(
                                    productId
                            );

            if (available != null) {

                branchProducts =
                        branchProducts
                                .stream()
                                .filter(
                                        branchProduct ->
                                                branchProduct.isAvailable()
                                                        == available
                                )
                                .toList();
            }

        } else {

            branchProducts =
                    branchProductRepository
                            .findByAvailableOrderByIdAsc(
                                    available
                            );
        }

        return branchProducts
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BranchProductResponse getBranchProductById(
            Long id
    ) {

        BranchProduct branchProduct =
                findBranchProductById(id);

        return toResponse(
                branchProduct
        );
    }

    @Override
    @Transactional(readOnly = true)
    public BranchProductResponse getBranchProductByBranchAndProduct(
            Long branchId,
            Long productId
    ) {

        validateProductExists(productId);

        BranchProduct branchProduct =
                branchProductRepository
                        .findByBranchIdAndProductId(
                                branchId,
                                productId
                        )
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Branch product not found"
                                        )
                        );

        return toResponse(
                branchProduct
        );
    }

    @Override
    public BranchProductResponse updateAvailability(
            Long id,
            BranchProductStatusUpdateRequest request
    ) {

        BranchProduct branchProduct =
                findBranchProductById(id);

        boolean requestedAvailability =
                request.getAvailable();

        if (branchProduct.isAvailable()
                == requestedAvailability) {

            String status =
                    requestedAvailability
                            ? "available"
                            : "unavailable";

            throw new BusinessRuleException(
                    "Branch product is already "
                            + status
            );
        }

        if (requestedAvailability) {

            validateProductIsActive(
                    branchProduct.getProduct()
            );
        }

        branchProduct.setAvailable(
                requestedAvailability
        );

        BranchProduct updatedBranchProduct =
                branchProductRepository
                        .save(branchProduct);

        return toResponse(
                updatedBranchProduct
        );
    }

    private BranchProduct findBranchProductById(
            Long id
    ) {

        return branchProductRepository
                .findById(id)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Branch product not found"
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
                    "Cannot assign or make available an inactive product"
            );
        }
    }

    private BranchProductResponse toResponse(
            BranchProduct branchProduct
    ) {

        Product product =
                branchProduct.getProduct();

        return new BranchProductResponse(
                branchProduct.getId(),
                branchProduct.getBranchId(),
                product.getProductCode(),
                product.getName(),
                branchProduct.isAvailable(),
                branchProduct.getCreatedAt(),
                branchProduct.getUpdatedAt()
        );
    }
}