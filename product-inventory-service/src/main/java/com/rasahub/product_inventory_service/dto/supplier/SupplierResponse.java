
package com.rasahub.product_inventory_service.dto.supplier;

import com.rasahub.product_inventory_service.entity.Supplier;

import java.time.LocalDateTime;

public class SupplierResponse {

    private final Long id;
    private final String supplierCode;
    private final String supplierName;
    private final String phoneNumber;
    private final String email;
    private final String address;
    private final boolean active;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public SupplierResponse(Supplier supplier) {
        this.id = supplier.getId();
        this.supplierCode = supplier.getSupplierCode();
        this.supplierName = supplier.getSupplierName();
        this.phoneNumber = supplier.getPhoneNumber();
        this.email = supplier.getEmail();
        this.address = supplier.getAddress();
        this.active = supplier.isActive();
        this.createdAt = supplier.getCreatedAt();
        this.updatedAt = supplier.getUpdatedAt();
    }

    public Long getId() {
        return id;
    }

    public String getSupplierCode() {
        return supplierCode;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public String getAddress() {
        return address;
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
