package com.rasahub.product_inventory_service.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "product_variants",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_product_variants_variant_code",
                        columnNames = "variant_code"
                ),
                @UniqueConstraint(
                        name = "uk_product_variants_product_name",
                        columnNames = {
                                "product_id",
                                "name"
                        }
                )
        }
)
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "variant_code",
            nullable = false,
            unique = true,
            updatable = false,
            length = 20
    )
    private String variantCode;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "product_id",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(
                    name = "fk_product_variants_product"
            )
    )
    private Product product;

    @Column(
            name = "name",
            nullable = false,
            length = 100
    )
    private String name;

    @Column(
            name = "price",
            nullable = false,
            precision = 10,
            scale = 2
    )
    private BigDecimal price;

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

    protected ProductVariant() {
    }

    public ProductVariant(
            String variantCode,
            Product product,
            String name,
            BigDecimal price
    ) {
        this.variantCode = variantCode;
        this.product = product;
        this.name = name;
        this.price = price;
        this.active = true;
    }

    @PrePersist
    protected void onCreate() {

        LocalDateTime now =
                LocalDateTime.now();

        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {

        this.updatedAt =
                LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getVariantCode() {
        return variantCode;
    }

    public Product getProduct() {
        return product;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
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

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}