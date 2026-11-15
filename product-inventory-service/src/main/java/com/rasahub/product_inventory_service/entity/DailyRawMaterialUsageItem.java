
package com.rasahub.product_inventory_service.entity;

import com.rasahub.product_inventory_service.enums.DailyRawMaterialUsageStatus;
import com.rasahub.product_inventory_service.enums.MeasurementUnit;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Entity
@Table(
        name = "daily_raw_material_usage_items",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_daily_usage_inventory_item",
                        columnNames = {
                                "daily_raw_material_usage_id",
                                "inventory_item_id"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_daily_usage_item_inventory",
                        columnList = "inventory_item_id"
                )
        }
)
public class DailyRawMaterialUsageItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "daily_raw_material_usage_id",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(
                    name = "fk_daily_usage_item_usage"
            )
    )
    private DailyRawMaterialUsage dailyRawMaterialUsage;

    @NotNull
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "inventory_item_id",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(
                    name = "fk_daily_usage_item_inventory"
            )
    )
    private InventoryItem inventoryItem;

    @NotNull
    @DecimalMin(
            value = "0.000",
            inclusive = false,
            message = "Used quantity must be greater than zero"
    )
    @Digits(
            integer = 15,
            fraction = 3,
            message = "Maximum 15 integer digits and 3 decimal places allowed"
    )
    @Column(
            name = "used_quantity",
            nullable = false,
            precision = 18,
            scale = 3
    )
    private BigDecimal usedQuantity;

    @Version
    @Column(
            name = "version",
            nullable = false
    )
    private Long version;

    protected DailyRawMaterialUsageItem() {
    }

    public DailyRawMaterialUsageItem(
            DailyRawMaterialUsage dailyRawMaterialUsage,
            InventoryItem inventoryItem,
            BigDecimal usedQuantity
    ) {
        if (dailyRawMaterialUsage == null) {
            throw new IllegalArgumentException(
                    "Daily raw material usage is required"
            );
        }

        if (dailyRawMaterialUsage.getStatus()
                != DailyRawMaterialUsageStatus.DRAFT) {
            throw new IllegalStateException(
                    "Items can only be added to DRAFT usage records"
            );
        }

        if (inventoryItem == null
                || inventoryItem.getId() == null
                || !inventoryItem.isActive()) {
            throw new IllegalArgumentException(
                    "A saved, active inventory item is required"
            );
        }

        validateQuantity(inventoryItem, usedQuantity);

        this.dailyRawMaterialUsage = dailyRawMaterialUsage;
        this.inventoryItem = inventoryItem;
        this.usedQuantity = usedQuantity;
    }

    public void attachToUsage(
            DailyRawMaterialUsage dailyRawMaterialUsage
    ) {
        if (dailyRawMaterialUsage == null) {
            throw new IllegalArgumentException(
                    "Daily raw material usage is required"
            );
        }

        this.dailyRawMaterialUsage = dailyRawMaterialUsage;
    }

    public void changeUsedQuantity(BigDecimal newQuantity) {
        if (dailyRawMaterialUsage.getStatus()
                != DailyRawMaterialUsageStatus.DRAFT) {
            throw new IllegalStateException(
                    "Only DRAFT usage quantities can be changed"
            );
        }

        validateQuantity(inventoryItem, newQuantity);

        this.usedQuantity = newQuantity;
    }

    private void validateQuantity(
            InventoryItem item,
            BigDecimal quantity
    ) {
        if (quantity == null) {
            throw new IllegalArgumentException(
                    "Used quantity is required"
            );
        }

        if (quantity.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Used quantity must be greater than zero"
            );
        }

        if (quantity.scale() > 3
                || quantity.precision() - quantity.scale() > 15) {
            throw new IllegalArgumentException(
                    "Invalid used quantity precision"
            );
        }

        if (item.getMeasurementUnit() == MeasurementUnit.PCS
                && quantity.stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException(
                    "PCS usage quantity must be a whole number"
            );
        }
    }

    @PrePersist
    protected void onCreate() {
        if (dailyRawMaterialUsage == null
                || inventoryItem == null
                || inventoryItem.getId() == null) {
            throw new IllegalStateException(
                    "Daily usage and inventory item are required"
            );
        }

        if (dailyRawMaterialUsage.getStatus()
                != DailyRawMaterialUsageStatus.DRAFT) {
            throw new IllegalStateException(
                    "New items must belong to a DRAFT usage record"
            );
        }

        validateQuantity(inventoryItem, usedQuantity);
    }

    @PreUpdate
    protected void onUpdate() {
        if (dailyRawMaterialUsage.getStatus()
                != DailyRawMaterialUsageStatus.DRAFT) {
            throw new IllegalStateException(
                    "Posted usage items cannot be modified"
            );
        }

        validateQuantity(inventoryItem, usedQuantity);
    }

    public Long getId() {
        return id;
    }

    public DailyRawMaterialUsage getDailyRawMaterialUsage() {
        return dailyRawMaterialUsage;
    }

    public InventoryItem getInventoryItem() {
        return inventoryItem;
    }

    public BigDecimal getUsedQuantity() {
        return usedQuantity;
    }

    public Long getVersion() {
        return version;
    }
}
