package com.rasahub.product_inventory_service.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "products",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_products_product_code",
                        columnNames = "product_code"
                )
        }
)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "product_code",
            nullable = false,
            unique = true,
            updatable = false,
            length = 20
    )
    private String productCode;

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

    @Column(
            name = "image_url",
            length = 500
    )
    private String imageUrl;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "category_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_products_category"
            )
    )
    private Category category;

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

    protected Product() {
    }

    public Product(
            String productCode,
            String name,
            String description,
            String imageUrl,
            Category category
    ) {
        this.productCode = productCode;
        this.name = name;
        this.description = description;
        this.imageUrl = imageUrl;
        this.category = category;
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

    public String getProductCode() {
        return productCode;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public Category getCategory() {
        return category;
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

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}