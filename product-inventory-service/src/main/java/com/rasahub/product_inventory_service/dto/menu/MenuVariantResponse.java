package com.rasahub.product_inventory_service.dto.menu;

import com.rasahub.product_inventory_service.enums.OfferDiscountType;

import java.math.BigDecimal;

public class MenuVariantResponse {

    private final Long id;

    private final String variantCode;

    private final String variantName;

    private final BigDecimal originalPrice;

    private final String offerCode;

    private final String offerName;

    private final OfferDiscountType discountType;

    private final BigDecimal discountValue;

    private final BigDecimal finalPrice;

    public MenuVariantResponse(
            Long id,
            String variantCode,
            String variantName,
            BigDecimal originalPrice,
            String offerCode,
            String offerName,
            OfferDiscountType discountType,
            BigDecimal discountValue,
            BigDecimal finalPrice
    ) {
        this.id = id;
        this.variantCode = variantCode;
        this.variantName = variantName;
        this.originalPrice = originalPrice;
        this.offerCode = offerCode;
        this.offerName = offerName;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.finalPrice = finalPrice;
    }

    public Long getId() {
        return id;
    }

    public String getVariantCode() {
        return variantCode;
    }

    public String getVariantName() {
        return variantName;
    }

    public BigDecimal getOriginalPrice() {
        return originalPrice;
    }

    public String getOfferCode() {
        return offerCode;
    }

    public String getOfferName() {
        return offerName;
    }

    public OfferDiscountType getDiscountType() {
        return discountType;
    }

    public BigDecimal getDiscountValue() {
        return discountValue;
    }

    public BigDecimal getFinalPrice() {
        return finalPrice;
    }
}