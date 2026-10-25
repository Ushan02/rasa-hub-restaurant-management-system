package com.rasahub.product_inventory_service.dto.offer;

import com.rasahub.product_inventory_service.enums.OfferDiscountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OfferResponse {

    private final Long id;
    private final String offerCode;

    private final String productCode;
    private final String productName;

    private final String variantCode;
    private final String variantName;

    private final String name;
    private final String description;

    private final OfferDiscountType discountType;
    private final BigDecimal discountValue;

    private final LocalDateTime startAt;
    private final LocalDateTime endAt;

    private final boolean active;

    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public OfferResponse(
            Long id,
            String offerCode,
            String productCode,
            String productName,
            String variantCode,
            String variantName,
            String name,
            String description,
            OfferDiscountType discountType,
            BigDecimal discountValue,
            LocalDateTime startAt,
            LocalDateTime endAt,
            boolean active,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.offerCode = offerCode;
        this.productCode = productCode;
        this.productName = productName;
        this.variantCode = variantCode;
        this.variantName = variantName;
        this.name = name;
        this.description = description;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.startAt = startAt;
        this.endAt = endAt;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getOfferCode() {
        return offerCode;
    }

    public String getProductCode() {
        return productCode;
    }

    public String getProductName() {
        return productName;
    }

    public String getVariantCode() {
        return variantCode;
    }

    public String getVariantName() {
        return variantName;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public OfferDiscountType getDiscountType() {
        return discountType;
    }

    public BigDecimal getDiscountValue() {
        return discountValue;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public boolean isActive() {
        return active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}