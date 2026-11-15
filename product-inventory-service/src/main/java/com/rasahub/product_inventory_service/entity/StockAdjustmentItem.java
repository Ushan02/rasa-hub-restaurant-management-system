package com.rasahub.product_inventory_service.entity;

import com.rasahub.product_inventory_service.enums.MeasurementUnit;
import com.rasahub.product_inventory_service.enums.StockAdjustmentStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Entity
@Table(
        name = "stock_adjustment_items",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_stock_adjustment_inventory_item",
                        columnNames = {
                                "stock_adjustment_id",
                                "inventory_item_id"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_stock_adjustment_item_inventory",
                        columnList = "inventory_item_id"
                )
        }
)
public class StockAdjustmentItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "stock_adjustment_id",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(
                    name = "fk_stock_adjustment_item_adjustment"
            )
    )
    private StockAdjustment stockAdjustment;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "inventory_item_id",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(
                    name = "fk_stock_adjustment_item_inventory"
            )
    )
    private InventoryItem inventoryItem;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "source_stock_count_item_id",
            unique = true,
            foreignKey = @ForeignKey(
                    name = "fk_stock_adjustment_item_count_item"
            )
    )
    private StockCountItem sourceStockCountItem;

    @NotNull
    @DecimalMin(
            value = "0.000",
            inclusive = false,
            message = "Adjustment quantity must be greater than zero"
    )
    @Digits(
            integer = 15,
            fraction = 3,
            message = "Maximum 15 integer digits and 3 decimal places allowed"
    )
    @Column(
            name = "adjustment_quantity",
            nullable = false,
            precision = 18,
            scale = 3
    )
    private BigDecimal adjustmentQuantity;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected StockAdjustmentItem() {
    }

    public StockAdjustmentItem(
            StockAdjustment stockAdjustment,
            InventoryItem inventoryItem,
            BigDecimal adjustmentQuantity
    ) {
        this(stockAdjustment, inventoryItem, adjustmentQuantity, null);
    }

    public StockAdjustmentItem(
            StockAdjustment stockAdjustment,
            InventoryItem inventoryItem,
            BigDecimal adjustmentQuantity,
            StockCountItem sourceStockCountItem
    ) {
        if (stockAdjustment == null
                || stockAdjustment.getStatus()
                != StockAdjustmentStatus.DRAFT) {
            throw new IllegalArgumentException(
                    "Item must belong to a DRAFT stock adjustment"
            );
        }
        validateInventoryItem(inventoryItem);
        validateQuantity(inventoryItem, adjustmentQuantity);
        if ((stockAdjustment.getSourceStockCount() == null)
                != (sourceStockCountItem == null)) {
            throw new IllegalArgumentException(
                    "Count-linked adjustment items require their source count item"
            );
        }
        if (sourceStockCountItem != null
                && (sourceStockCountItem.getStockCount() == null
                || !sourceStockCountItem.getStockCount().getId()
                        .equals(stockAdjustment.getSourceStockCount().getId())
                || !sourceStockCountItem.getInventoryItem().getId()
                        .equals(inventoryItem.getId()))) {
            throw new IllegalArgumentException(
                    "Source count item does not match the stock adjustment"
            );
        }

        this.stockAdjustment = stockAdjustment;
        this.inventoryItem = inventoryItem;
        this.adjustmentQuantity = adjustmentQuantity;
        this.sourceStockCountItem = sourceStockCountItem;
    }

    public void attachTo(StockAdjustment adjustment) {
        if (adjustment == null
                || adjustment.getStatus() != StockAdjustmentStatus.DRAFT) {
            throw new IllegalArgumentException(
                    "Item must belong to a DRAFT stock adjustment"
            );
        }
        this.stockAdjustment = adjustment;
    }

    public void changeQuantity(BigDecimal quantity) {
        if (stockAdjustment.getStatus() != StockAdjustmentStatus.DRAFT) {
            throw new IllegalStateException(
                    "Only DRAFT adjustment quantities can be changed"
            );
        }
        validateQuantity(inventoryItem, quantity);
        adjustmentQuantity = quantity;
    }

    private void validateInventoryItem(InventoryItem item) {
        if (item == null || item.getId() == null || !item.isActive()) {
            throw new IllegalArgumentException(
                    "A saved, active inventory item is required"
            );
        }
    }

    private void validateQuantity(
            InventoryItem item,
            BigDecimal quantity
    ) {
        if (quantity == null || quantity.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Adjustment quantity must be greater than zero"
            );
        }
        if (quantity.scale() > 3
                || quantity.precision() - quantity.scale() > 15) {
            throw new IllegalArgumentException(
                    "Adjustment quantity supports at most 15 integer digits and 3 decimals"
            );
        }
        if (item.getMeasurementUnit() == MeasurementUnit.PCS
                && quantity.stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException(
                    "PCS adjustment quantity must be a whole number"
            );
        }
    }

    @PrePersist
    protected void onCreate() {
        validateInventoryItem(inventoryItem);
        if (stockAdjustment == null
                || stockAdjustment.getStatus()
                != StockAdjustmentStatus.DRAFT) {
            throw new IllegalStateException(
                    "New adjustment items must belong to a DRAFT adjustment"
            );
        }
        validateQuantity(inventoryItem, adjustmentQuantity);
    }

    @PreUpdate
    protected void onUpdate() {
        if (stockAdjustment.getStatus() != StockAdjustmentStatus.DRAFT) {
            throw new IllegalStateException(
                    "Posted or submitted adjustment items cannot be modified"
            );
        }
        validateInventoryItem(inventoryItem);
        validateQuantity(inventoryItem, adjustmentQuantity);
    }

    public Long getId() {
        return id;
    }

    public StockAdjustment getStockAdjustment() {
        return stockAdjustment;
    }

    public InventoryItem getInventoryItem() {
        return inventoryItem;
    }

    public StockCountItem getSourceStockCountItem() {
        return sourceStockCountItem;
    }

    public BigDecimal getAdjustmentQuantity() {
        return adjustmentQuantity;
    }

    public Long getVersion() {
        return version;
    }
}
