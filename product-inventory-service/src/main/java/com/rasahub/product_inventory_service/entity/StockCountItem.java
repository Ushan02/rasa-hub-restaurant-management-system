package com.rasahub.product_inventory_service.entity;

import com.rasahub.product_inventory_service.enums.MeasurementUnit;
import com.rasahub.product_inventory_service.enums.StockCountStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Entity
@Table(
        name = "stock_count_items",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_stock_count_inventory_item",
                columnNames = {"stock_count_id", "inventory_item_id"}
        ),
        indexes = @Index(
                name = "idx_stock_count_item_inventory",
                columnList = "inventory_item_id"
        )
)
public class StockCountItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "stock_count_id",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_stock_count_item_count")
    )
    private StockCount stockCount;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "inventory_item_id",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_stock_count_item_inventory")
    )
    private InventoryItem inventoryItem;

    @NotNull
    @DecimalMin("0.000")
    @Digits(integer = 15, fraction = 3)
    @Column(name = "system_quantity", nullable = false, precision = 18, scale = 3)
    private BigDecimal systemQuantity;

    @Column(name = "system_balance_version")
    private Long systemBalanceVersion;

    @DecimalMin("0.000")
    @Digits(integer = 15, fraction = 3)
    @Column(name = "physical_quantity", precision = 18, scale = 3)
    private BigDecimal physicalQuantity;

    @Size(max = 300)
    @Column(name = "item_remarks", length = 300)
    private String itemRemarks;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected StockCountItem() {
    }

    public StockCountItem(
            StockCount stockCount,
            InventoryItem inventoryItem,
            BigDecimal systemQuantity,
            Long systemBalanceVersion,
            BigDecimal physicalQuantity,
            String itemRemarks
    ) {
        if (stockCount == null
                || stockCount.getStatus() != StockCountStatus.DRAFT) {
            throw new IllegalArgumentException(
                    "Item must belong to a DRAFT stock count"
            );
        }
        validateInventoryItem(inventoryItem);
        validateSystemQuantity(systemQuantity);
        validateBalanceVersion(systemBalanceVersion);
        validatePhysicalQuantity(inventoryItem, physicalQuantity);
        validateRemarks(itemRemarks);

        this.stockCount = stockCount;
        this.inventoryItem = inventoryItem;
        this.systemQuantity = systemQuantity;
        this.systemBalanceVersion = systemBalanceVersion;
        this.physicalQuantity = physicalQuantity;
        this.itemRemarks = normalizeText(itemRemarks);
    }

    public void updatePhysicalQuantity(
            BigDecimal quantity,
            String remarks
    ) {
        if (stockCount.getStatus() != StockCountStatus.DRAFT) {
            throw new IllegalStateException(
                    "Only DRAFT stock count items can be modified"
            );
        }
        validatePhysicalQuantity(inventoryItem, quantity);
        validateRemarks(remarks);
        physicalQuantity = quantity;
        itemRemarks = normalizeText(remarks);
    }

    private void validateInventoryItem(InventoryItem item) {
        if (item == null || item.getId() == null || !item.isActive()) {
            throw new IllegalArgumentException(
                    "A saved, active inventory item is required"
            );
        }
    }

    private void validateSystemQuantity(BigDecimal quantity) {
        if (quantity == null || quantity.signum() < 0
                || quantity.scale() > 3
                || quantity.precision() - quantity.scale() > 15) {
            throw new IllegalArgumentException(
                    "Invalid system stock quantity"
            );
        }
    }

    private void validateBalanceVersion(Long value) {
        if (value != null && value < 0) {
            throw new IllegalArgumentException(
                    "Invalid stock balance snapshot version"
            );
        }
    }

    private void validatePhysicalQuantity(
            InventoryItem item,
            BigDecimal quantity
    ) {
        if (quantity == null) {
            return;
        }
        if (quantity.signum() < 0 || quantity.scale() > 3
                || quantity.precision() - quantity.scale() > 15) {
            throw new IllegalArgumentException(
                    "Physical quantity must be non-negative with at most 15 integer digits and 3 decimals"
            );
        }
        if (item.getMeasurementUnit() == MeasurementUnit.PCS
                && quantity.stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException(
                    "PCS physical quantity must be a whole number"
            );
        }
    }

    private void validateRemarks(String value) {
        if (value != null && value.trim().length() > 300) {
            throw new IllegalArgumentException(
                    "Item remarks must not exceed 300 characters"
            );
        }
    }

    private String normalizeText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    @PrePersist
    protected void onCreate() {
        validateInventoryItem(inventoryItem);
        validateSystemQuantity(systemQuantity);
        validateBalanceVersion(systemBalanceVersion);
        validatePhysicalQuantity(inventoryItem, physicalQuantity);
        validateRemarks(itemRemarks);
        if (stockCount == null
                || stockCount.getStatus() != StockCountStatus.DRAFT) {
            throw new IllegalStateException(
                    "New stock count items must belong to a DRAFT count"
            );
        }
    }

    @PreUpdate
    protected void onUpdate() {
        if (stockCount.getStatus() != StockCountStatus.DRAFT) {
            throw new IllegalStateException(
                    "Submitted stock count items cannot be modified"
            );
        }
        validatePhysicalQuantity(inventoryItem, physicalQuantity);
        validateRemarks(itemRemarks);
    }

    public Long getId() {
        return id;
    }

    public StockCount getStockCount() {
        return stockCount;
    }

    public InventoryItem getInventoryItem() {
        return inventoryItem;
    }

    public BigDecimal getSystemQuantity() {
        return systemQuantity;
    }

    public Long getSystemBalanceVersion() {
        return systemBalanceVersion;
    }

    public BigDecimal getPhysicalQuantity() {
        return physicalQuantity;
    }

    public BigDecimal getVarianceQuantity() {
        return physicalQuantity == null
                ? null
                : physicalQuantity.subtract(systemQuantity);
    }

    public String getItemRemarks() {
        return itemRemarks;
    }

    public Long getVersion() {
        return version;
    }
}
