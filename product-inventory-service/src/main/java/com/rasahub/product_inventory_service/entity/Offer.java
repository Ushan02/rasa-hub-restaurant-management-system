package com.rasahub.product_inventory_service.entity;

import com.rasahub.product_inventory_service.enums.OfferDiscountType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "offers",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_offers_offer_code",
                        columnNames = "offer_code"
                )
        }
)
public class Offer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "offer_code",
            nullable = false,
            unique = true,
            updatable = false,
            length = 20
    )
    private String offerCode;

    @Column(
            name = "name",
            nullable = false,
            length = 150
    )
    private String name;

    @Column(
            name = "description",
            length = 500
    )
    private String description;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "product_variant_id",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(
                    name = "fk_offers_product_variant"
            )
    )
    private ProductVariant productVariant;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "discount_type",
            nullable = false,
            length = 30
    )
    private OfferDiscountType discountType;

    @Column(
            name = "discount_value",
            nullable = false,
            precision = 10,
            scale = 2
    )
    private BigDecimal discountValue;

    @Column(
            name = "start_at",
            nullable = false
    )
    private LocalDateTime startAt;

    @Column(
            name = "end_at",
            nullable = false
    )
    private LocalDateTime endAt;

    @Column(
            name = "active",
            nullable = false
    )
    private boolean active;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;

    protected Offer() {
    }

    public Offer(
            String offerCode,
            String name,
            String description,
            ProductVariant productVariant,
            OfferDiscountType discountType,
            BigDecimal discountValue,
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {
        this.offerCode = offerCode;
        this.name = name;
        this.description = description;
        this.productVariant = productVariant;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.startAt = startAt;
        this.endAt = endAt;
        this.active = true;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getOfferCode() {
        return offerCode;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public ProductVariant getProductVariant() {
        return productVariant;
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

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public void setActive(boolean active) {
        this.active = active;
    }
}