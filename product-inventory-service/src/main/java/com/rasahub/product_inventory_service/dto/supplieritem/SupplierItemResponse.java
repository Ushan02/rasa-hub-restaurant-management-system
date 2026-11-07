
package com.rasahub.product_inventory_service.dto.supplieritem;

import com.rasahub.product_inventory_service.entity.SupplierItem;
import com.rasahub.product_inventory_service.enums.MeasurementUnit;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SupplierItemResponse {

    private final Long id;

    private final Long supplierId;
    private final String supplierCode;
    private final String supplierName;

    private final Long inventoryItemId;
    private final String inventoryItemCode;
    private final String inventoryItemName;
    private final MeasurementUnit measurementUnit;

    private final BigDecimal unitPrice;
    private final BigDecimal minimumOrderQuantity;
    private final Integer leadTimeDays;

    private final boolean active;

    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public SupplierItemResponse(SupplierItem supplierItem) {

        this.id = supplierItem.getId();

        this.supplierId = supplierItem.getSupplier().getId();
        this.supplierCode = supplierItem.getSupplier().getSupplierCode();
        this.supplierName = supplierItem.getSupplier().getSupplierName();

        this.inventoryItemId = supplierItem.getInventoryItem().getId();
        this.inventoryItemCode = supplierItem.getInventoryItem().getItemCode();
        this.inventoryItemName = supplierItem.getInventoryItem().getItemName();
        this.measurementUnit = supplierItem.getInventoryItem().getMeasurementUnit();

        this.unitPrice = supplierItem.getUnitPrice();
        this.minimumOrderQuantity = supplierItem.getMinimumOrderQuantity();
        this.leadTimeDays = supplierItem.getLeadTimeDays();

        this.active = supplierItem.isActive();

        this.createdAt = supplierItem.getCreatedAt();
        this.updatedAt = supplierItem.getUpdatedAt();
    }

    public Long getId() {
        return id;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public String getSupplierCode() {
        return supplierCode;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public Long getInventoryItemId() {
        return inventoryItemId;
    }

    public String getInventoryItemCode() {
        return inventoryItemCode;
    }

    public String getInventoryItemName() {
        return inventoryItemName;
    }

    public MeasurementUnit getMeasurementUnit() {
        return measurementUnit;
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
}
