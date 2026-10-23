package com.rasahub.product_inventory_service.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "branch_products",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_branch_products_branch_product",
                        columnNames = {
                                "branch_id",
                                "product_id"
                        }
                )
        }
)
public class BranchProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "branch_id",
            nullable = false,
            updatable = false
    )
    private Long branchId;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "product_id",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(
                    name = "fk_branch_products_product"
            )
    )
    private Product product;

    @Column(
            name = "available",
            nullable = false
    )
    private boolean available;

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

    protected BranchProduct() {
    }

    public BranchProduct(
            Long branchId,
            Product product
    ) {
        this.branchId = branchId;
        this.product = product;
        this.available = true;
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

    public Long getBranchId() {
        return branchId;
    }

    public Product getProduct() {
        return product;
    }

    public boolean isAvailable() {
        return available;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
}