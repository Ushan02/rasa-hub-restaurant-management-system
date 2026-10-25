package com.rasahub.product_inventory_service.service.impl;

import com.rasahub.product_inventory_service.dto.menu.MenuItemResponse;
import com.rasahub.product_inventory_service.dto.menu.MenuVariantResponse;
import com.rasahub.product_inventory_service.entity.BranchProduct;
import com.rasahub.product_inventory_service.entity.Offer;
import com.rasahub.product_inventory_service.entity.Product;
import com.rasahub.product_inventory_service.entity.ProductVariant;
import com.rasahub.product_inventory_service.enums.OfferDiscountType;
import com.rasahub.product_inventory_service.exception.BusinessRuleException;
import com.rasahub.product_inventory_service.repository.BranchProductRepository;
import com.rasahub.product_inventory_service.repository.OfferRepository;
import com.rasahub.product_inventory_service.repository.ProductVariantRepository;
import com.rasahub.product_inventory_service.security.ProductUserPrincipal;
import com.rasahub.product_inventory_service.service.MenuService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class MenuServiceImpl
        implements MenuService {

    private final BranchProductRepository branchProductRepository;
    private final ProductVariantRepository productVariantRepository;
    private final OfferRepository offerRepository;

    public MenuServiceImpl(
            BranchProductRepository branchProductRepository,
            ProductVariantRepository productVariantRepository,
            OfferRepository offerRepository
    ) {
        this.branchProductRepository = branchProductRepository;
        this.productVariantRepository = productVariantRepository;
        this.offerRepository = offerRepository;
    }

    @Override
    public List<MenuItemResponse> getMenu(
            Long branchId,
            Long categoryId,
            String keyword
    ) {

        validateBranchId(branchId);

        validateBranchReadAccess(branchId);

        String normalizedKeyword =
                normalizeKeyword(keyword);

        List<BranchProduct> branchProducts =
                branchProductRepository.findMenuItems(
                        branchId,
                        categoryId,
                        normalizedKeyword
                );

        LocalDateTime currentTime =
                LocalDateTime.now();

        List<MenuItemResponse> menuItems =
                new ArrayList<>();

        for (BranchProduct branchProduct
                : branchProducts) {

            Product product =
                    branchProduct.getProduct();

            List<ProductVariant> activeVariants =
                    productVariantRepository
                            .findByProductIdAndActiveOrderByIdAsc(
                                    product.getId(),
                                    true
                            );

            if (activeVariants.isEmpty()) {
                continue;
            }

            List<MenuVariantResponse> variantResponses =
                    new ArrayList<>();

            for (ProductVariant variant
                    : activeVariants) {

                MenuVariantResponse variantResponse =
                        buildVariantResponse(
                                variant,
                                currentTime
                        );

                variantResponses.add(
                        variantResponse
                );
            }

            MenuItemResponse menuItem =
                    new MenuItemResponse(
                            product.getId(),
                            product.getProductCode(),
                            product.getName(),
                            product.getDescription(),
                            product.getImageUrl(),
                            product.getCategory()
                                    .getCategoryCode(),
                            product.getCategory()
                                    .getName(),
                            branchProduct.getBranchId(),
                            branchProduct.isAvailable(),
                            variantResponses
                    );

            menuItems.add(menuItem);
        }

        return menuItems;
    }

    private MenuVariantResponse buildVariantResponse(
            ProductVariant variant,
            LocalDateTime currentTime
    ) {

        BigDecimal originalPrice =
                variant.getPrice();

        List<Offer> currentOffers =
                offerRepository
                        .findCurrentActiveOffersForVariant(
                                variant.getId(),
                                currentTime
                        );

        if (currentOffers.isEmpty()) {

            return new MenuVariantResponse(
                    variant.getId(),
                    variant.getVariantCode(),
                    variant.getName().name(),
                    originalPrice,
                    null,
                    null,
                    null,
                    null,
                    originalPrice
            );
        }

        Offer offer =
                currentOffers.getFirst();

        BigDecimal finalPrice =
                calculateFinalPrice(
                        originalPrice,
                        offer
                );

        return new MenuVariantResponse(
                variant.getId(),
                variant.getVariantCode(),
                variant.getName().name(),
                originalPrice,
                offer.getOfferCode(),
                offer.getName(),
                offer.getDiscountType(),
                offer.getDiscountValue(),
                finalPrice
        );
    }

    private BigDecimal calculateFinalPrice(
            BigDecimal originalPrice,
            Offer offer
    ) {

        BigDecimal finalPrice;

        if (offer.getDiscountType()
                == OfferDiscountType.PERCENTAGE) {

            BigDecimal discountAmount =
                    originalPrice
                            .multiply(
                                    offer.getDiscountValue()
                            )
                            .divide(
                                    BigDecimal.valueOf(100),
                                    2,
                                    RoundingMode.HALF_UP
                            );

            finalPrice =
                    originalPrice.subtract(
                            discountAmount
                    );

        } else {

            finalPrice =
                    originalPrice.subtract(
                            offer.getDiscountValue()
                    );
        }

        return finalPrice.setScale(
                2,
                RoundingMode.HALF_UP
        );
    }

    private String normalizeKeyword(
            String keyword
    ) {

        if (keyword == null
                || keyword.isBlank()) {

            return null;
        }

        return keyword.trim();
    }

    private void validateBranchId(
            Long branchId
    ) {

        if (branchId == null) {

            throw new BusinessRuleException(
                    "Branch ID is required"
            );
        }

        if (branchId <= 0) {

            throw new BusinessRuleException(
                    "Branch ID must be positive"
            );
        }
    }

    private void validateBranchReadAccess(
            Long requestedBranchId
    ) {

        ProductUserPrincipal principal =
                getCurrentPrincipal();

        String role =
                principal.getRole();

        if ("OWNER".equals(role)
                || "MAIN_OFFICE_MANAGER".equals(role)
                || "CUSTOMER".equals(role)) {

            return;
        }

        if ("BRANCH_MANAGER".equals(role)
                || "CASHIER".equals(role)) {

            Long userBranchId =
                    principal.getBranchId();

            if (userBranchId == null) {

                throw new AccessDeniedException(
                        "Branch information is missing"
                );
            }

            if (!userBranchId.equals(
                    requestedBranchId
            )) {

                throw new AccessDeniedException(
                        "You can only access the menu of your own branch"
                );
            }

            return;
        }

        throw new AccessDeniedException(
                "You are not allowed to access the menu"
        );
    }

    private ProductUserPrincipal getCurrentPrincipal() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal()
                instanceof ProductUserPrincipal principal)) {

            throw new AccessDeniedException(
                    "Authenticated user is required"
            );
        }

        return principal;
    }
}