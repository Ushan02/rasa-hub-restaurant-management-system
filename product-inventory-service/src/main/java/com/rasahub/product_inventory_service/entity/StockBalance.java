
package com.rasahub.product_inventory_service.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "stock_balances",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_stock_balances_location_item",
                        columnNames = {
                                "location_code",
                                "inventory_item_id"
                        }
                )
        }
)
public class StockBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "location_code",
            referencedColumnName = "location_code",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(
                    name = "fk_stock_balances_location"
            )
    )
    private StockLocation stockLocation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "inventory_item_id",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(
                    name = "fk_stock_balances_inventory_item"
            )
    )
    private InventoryItem inventoryItem;

    @NotNull
    @DecimalMin("0.000")
    @Digits(integer = 15, fraction = 3)
    @Column(
            name = "quantity_on_hand",
            nullable = false,
            precision = 18,
            scale = 3
    )
    private BigDecimal quantityOnHand = BigDecimal.ZERO;

    @NotNull
    @DecimalMin("0.000")
    @Digits(integer = 15, fraction = 3)
    @Column(
            name = "minimum_stock_level",
            nullable = false,
            precision = 18,
            scale = 3
    )
    private BigDecimal minimumStockLevel = BigDecimal.ZERO;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected StockBalance() {
    }

    public StockBalance(
            StockLocation stockLocation,
            InventoryItem inventoryItem
    ) {
        if (stockLocation == null || inventoryItem == null) {
            throw new IllegalArgumentException(
                    "Stock location and inventory item are required"
            );
        }

        this.stockLocation = stockLocation;
        this.inventoryItem = inventoryItem;
        this.quantityOnHand = BigDecimal.ZERO;
        this.minimumStockLevel = BigDecimal.ZERO;
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

    public StockLocation getStockLocation() {
        return stockLocation;
    }

    public InventoryItem getInventoryItem() {
        return inventoryItem;
    }

    public BigDecimal getQuantityOnHand() {
        return quantityOnHand;
    }

    public BigDecimal getMinimumStockLevel() {
        return minimumStockLevel;
    }

    public Long getVersion() {
        return version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public boolean isLowStock() {
        return quantityOnHand.compareTo(
                minimumStockLevel
        ) < 0;
    }

    public void setMinimumStockLevel(
            BigDecimal minimumStockLevel
    ) {
        validateNonNegative(minimumStockLevel);
        this.minimumStockLevel = minimumStockLevel;
    }

    public void increaseQuantity(BigDecimal quantity) {
        validatePositive(quantity);
        this.quantityOnHand =
                this.quantityOnHand.add(quantity);
    }

    public void decreaseQuantity(BigDecimal quantity) {
        validatePositive(quantity);

        if (this.quantityOnHand.compareTo(quantity) < 0) {
            throw new IllegalStateException(
                    "Insufficient stock quantity"
            );
        }

        this.quantityOnHand =
                this.quantityOnHand.subtract(quantity);
    }

    private void validatePositive(BigDecimal quantity) {
        validateNonNegative(quantity);

        if (quantity.signum() == 0) {
            throw new IllegalArgumentException(
                    "Quantity must be greater than zero"
            );
        }
    }

    private void validateNonNegative(BigDecimal quantity) {
        if (quantity == null
                || quantity.signum() < 0
                || quantity.scale() > 3
                || quantity.precision() - quantity.scale() > 15) {

            throw new IllegalArgumentException(
                    "Invalid stock quantity"
            );
        }
    }
}
