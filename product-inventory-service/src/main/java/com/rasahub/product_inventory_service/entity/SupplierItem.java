
package com.rasahub.product_inventory_service.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "supplier_items",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_supplier_items_supplier_inventory",
                        columnNames = {
                                "supplier_id",
                                "inventory_item_id"
                        }
                )
        }
)
public class SupplierItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "supplier_id",
            nullable = false
    )
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "inventory_item_id",
            nullable = false
    )
    private InventoryItem inventoryItem;

    @DecimalMin(value = "0.00", inclusive = false)
    @Column(
            name = "unit_price",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal unitPrice;

    @DecimalMin(value = "0.00", inclusive = false)
    @Column(
            name = "minimum_order_quantity",
            nullable = false,
            precision = 15,
            scale = 3
    )
    private BigDecimal minimumOrderQuantity;

    @Min(0)
    @Column(name = "lead_time_days")
    private Integer leadTimeDays;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected SupplierItem() {
    }

    public SupplierItem(
            Supplier supplier,
            InventoryItem inventoryItem,
            BigDecimal unitPrice,
            BigDecimal minimumOrderQuantity,
            Integer leadTimeDays
    ) {
        this.supplier = supplier;
        this.inventoryItem = inventoryItem;
        this.unitPrice = unitPrice;
        this.minimumOrderQuantity = minimumOrderQuantity;
        this.leadTimeDays = leadTimeDays;
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

    public Supplier getSupplier() {
        return supplier;
    }

    public InventoryItem getInventoryItem() {
        return inventoryItem;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getMinimumOrderQuantity() {
        return minimumOrderQuantity;
    }

    public Integer getLeadTimeDays() {
        return leadTimeDays;
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

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public void setMinimumOrderQuantity(
            BigDecimal minimumOrderQuantity
    ) {
        this.minimumOrderQuantity = minimumOrderQuantity;
    }

    public void setLeadTimeDays(Integer leadTimeDays) {
        this.leadTimeDays = leadTimeDays;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
