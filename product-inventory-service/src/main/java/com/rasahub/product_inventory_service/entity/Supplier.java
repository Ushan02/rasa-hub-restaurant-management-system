
package com.rasahub.product_inventory_service.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "suppliers",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_suppliers_supplier_code",
                        columnNames = "supplier_code"
                )
        }
)
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "supplier_code",
            nullable = false,
            updatable = false,
            length = 10
    )
    private String supplierCode;

    @NotBlank(message = "Supplier name is required")
    @Size(
            max = 150,
            message = "Supplier name must not exceed 150 characters"
    )
    @Column(
            name = "supplier_name",
            nullable = false,
            length = 150
    )
    private String supplierName;

    @NotBlank(message = "Phone number is required")
    @Size(
            min = 10,
            max = 10,
            message = "Phone number must contain exactly 10 digits"
    )
    @Pattern(
            regexp = "^(?:" +
                    "(?:070|071|072|075|076|077|078)[0-9]{7}" +
                    "|074[0-4][0-9]{6}" +
                    "|0(?:11|21|23|24|25|26|27|" +
                    "31|32|33|34|35|36|37|38|" +
                    "41|45|47|51|52|54|55|57|" +
                    "63|65|66|67|81|91)[0-9]{7}" +
                    ")$",
            message = "Invalid Sri Lankan mobile or landline number"
    )
    @Column(
            name = "phone_number",
            nullable = false,
            length = 10
    )
    private String phoneNumber;

    @Email(message = "Invalid email address")
    @Size(
            max = 150,
            message = "Email must not exceed 150 characters"
    )
    @Column(
            name = "email",
            length = 150
    )
    private String email;

    @Size(
            max = 500,
            message = "Address must not exceed 500 characters"
    )
    @Column(
            name = "address",
            length = 500
    )
    private String address;

    @Column(
            name = "active",
            nullable = false
    )
    private boolean active;

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

    protected Supplier() {
    }

    public Supplier(
            String supplierCode,
            String supplierName,
            String phoneNumber,
            String email,
            String address
    ) {
        this.supplierCode = supplierCode;
        this.supplierName = supplierName;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.address = address;
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

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
