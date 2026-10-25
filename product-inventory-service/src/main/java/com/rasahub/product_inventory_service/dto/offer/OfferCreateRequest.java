package com.rasahub.product_inventory_service.dto.offer;

import com.rasahub.product_inventory_service.enums.OfferDiscountType;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OfferCreateRequest {

    @NotBlank(message = "Offer name is required")
    @Size(
            max = 150,
            message = "Offer name must not exceed 150 characters"
    )
    private String name;

    @Size(
            max = 500,
            message = "Description must not exceed 500 characters"
    )
    private String description;

    @NotNull(message = "Product variant ID is required")
    @Positive(message = "Product variant ID must be positive")
    private Long productVariantId;

    @NotNull(message = "Discount type is required")
    private OfferDiscountType discountType;

    @NotNull(message = "Discount value is required")
    @Positive(message = "Discount value must be greater than zero")
    @Digits(
            integer = 8,
            fraction = 2,
            message = "Discount value must have at most 8 integer digits and 2 decimal places"
    )
    private BigDecimal discountValue;

    @NotNull(message = "Offer start time is required")
    private LocalDateTime startAt;

    @NotNull(message = "Offer end time is required")
    private LocalDateTime endAt;

    public OfferCreateRequest() {
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Long getProductVariantId() {
        return productVariantId;
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

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setProductVariantId(Long productVariantId) {
        this.productVariantId = productVariantId;
    }

    public void setDiscountType(
            OfferDiscountType discountType
    ) {
        this.discountType = discountType;
    }

    public void setDiscountValue(
            BigDecimal discountValue
    ) {
        this.discountValue = discountValue;
    }

    public void setStartAt(
            LocalDateTime startAt
    ) {
        this.startAt = startAt;
    }

    public void setEndAt(
            LocalDateTime endAt
    ) {
        this.endAt = endAt;
    }
}