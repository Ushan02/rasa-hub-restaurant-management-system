package com.rasahub.product_inventory_service.entity;

import com.rasahub.product_inventory_service.enums.MeasurementUnit;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "stock_reorder_levels",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_stock_reorder_levels_location_item",
                columnNames = {"location_code", "inventory_item_id"}
        )
)
public class StockReorderLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "location_code",
            referencedColumnName = "location_code",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_stock_reorder_levels_location")
    )
    private StockLocation stockLocation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "inventory_item_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_stock_reorder_levels_inventory_item")
    )
    private InventoryItem inventoryItem;

    @NotNull
    @DecimalMin(value = "0.000")
    @Digits(integer = 15, fraction = 3)
    @Column(name = "reorder_level", nullable = false, precision = 18, scale = 3)
    private BigDecimal reorderLevel = BigDecimal.ZERO;

    @DecimalMin(value = "0.000")
    @Digits(integer = 15, fraction = 3)
    @Column(name = "recommended_reorder_quantity", precision = 18, scale = 3)
    private BigDecimal recommendedReorderQuantity;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_by_account_id")
    private Long createdByAccountId;

    @Column(name = "updated_by_account_id")
    private Long updatedByAccountId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected StockReorderLevel() {
    }

    public StockReorderLevel(
            StockLocation stockLocation,
            InventoryItem inventoryItem,
            BigDecimal reorderLevel
    ) {
        this.stockLocation = stockLocation;
        this.inventoryItem = inventoryItem;
        setReorderLevel(reorderLevel);
        this.active = true;
    }

    @PrePersist
    protected void onCreate() {
        validate();
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        validate();
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public StockLocation getStockLocation() {
        return stockLocation;
    }

    public InventoryItem getInventoryItem() {
        return inventoryItem;
    }

    public BigDecimal getReorderLevel() {
        return reorderLevel;
    }

    public BigDecimal getRecommendedReorderQuantity() {
        return recommendedReorderQuantity;
    }

    public boolean isActive() {
        return active;
    }

    public Long getCreatedByAccountId() {
        return createdByAccountId;
    }

    public Long getUpdatedByAccountId() {
        return updatedByAccountId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setStockLocation(StockLocation stockLocation) {
        this.stockLocation = stockLocation;
    }

    public void setInventoryItem(InventoryItem inventoryItem) {
        this.inventoryItem = inventoryItem;
    }

    public void setReorderLevel(BigDecimal reorderLevel) {
        if (reorderLevel == null) {
            throw new IllegalArgumentException("Reorder level is required");
        }
        if (reorderLevel.signum() < 0) {
            throw new IllegalArgumentException("Reorder level cannot be negative");
        }
        if (reorderLevel.scale() > 3) {
            throw new IllegalArgumentException("Invalid reorder level precision");
        }
        if (inventoryItem != null && inventoryItem.getMeasurementUnit() == MeasurementUnit.PCS
                && reorderLevel.stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException("PCS reorder level must be a whole number");
        }
        this.reorderLevel = reorderLevel;
    }

    public void setRecommendedReorderQuantity(BigDecimal recommendedReorderQuantity) {
        if (recommendedReorderQuantity != null) {
            if (recommendedReorderQuantity.signum() < 0) {
                throw new IllegalArgumentException("Recommended reorder quantity cannot be negative");
            }
            if (recommendedReorderQuantity.scale() > 3) {
                throw new IllegalArgumentException("Invalid recommended reorder quantity precision");
            }
            if (inventoryItem != null && inventoryItem.getMeasurementUnit() == MeasurementUnit.PCS
                    && recommendedReorderQuantity.stripTrailingZeros().scale() > 0) {
                throw new IllegalArgumentException("PCS recommended reorder quantity must be a whole number");
            }
        }
        this.recommendedReorderQuantity = recommendedReorderQuantity;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setCreatedByAccountId(Long createdByAccountId) {
        this.createdByAccountId = createdByAccountId;
    }

    public void setUpdatedByAccountId(Long updatedByAccountId) {
        this.updatedByAccountId = updatedByAccountId;
    }

    private void validate() {
        if (stockLocation == null || !stockLocation.isActive()) {
            throw new IllegalArgumentException("Valid active stock location is required");
        }
        if (inventoryItem == null || !inventoryItem.isActive()) {
            throw new IllegalArgumentException("Valid active inventory item is required");
        }
        if (reorderLevel == null || reorderLevel.signum() < 0) {
            throw new IllegalArgumentException("Reorder level cannot be negative");
        }
        if (inventoryItem.getMeasurementUnit() == MeasurementUnit.PCS
                && reorderLevel.stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException("PCS reorder level must be a whole number");
        }
    }
}
