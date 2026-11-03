
package com.rasahub.product_inventory_service.entity;

import com.rasahub.product_inventory_service.enums.InventoryItemType;
import com.rasahub.product_inventory_service.enums.MeasurementUnit;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "inventory_items",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_inventory_items_item_code",
                columnNames = "item_code"
        )
)
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_code", nullable = false, updatable = false, length = 10)
    private String itemCode;

    @Column(name = "item_name", nullable = false, length = 150)
    private String itemName;

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 30)
    private InventoryItemType itemType;

    @Enumerated(EnumType.STRING)
    @Column(name = "measurement_unit", nullable = false, length = 10)
    private MeasurementUnit measurementUnit;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected InventoryItem() {
    }

    public InventoryItem(
            String itemCode,
            String itemName,
            String description,
            InventoryItemType itemType,
            MeasurementUnit measurementUnit
    ) {
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.description = description;
        this.itemType = itemType;
        this.measurementUnit = measurementUnit;
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
    public String getItemCode() {
        return itemCode;
    }
    public String getItemName() {
        return itemName;
    }
    public String getDescription() {
        return description;
    }
    public InventoryItemType getItemType() {
        return itemType;
    }
    public MeasurementUnit getMeasurementUnit() {
        return measurementUnit;
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
    public void setItemName(String itemName) {
        this.itemName = itemName;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public void setItemType(InventoryItemType itemType) {
        this.itemType = itemType;
    }
    public void setMeasurementUnit(MeasurementUnit measurementUnit) {
        this.measurementUnit = measurementUnit;
    }
    public void setActive(boolean active) {
        this.active = active;
    }
}
