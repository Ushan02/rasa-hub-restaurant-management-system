
package com.rasahub.product_inventory_service.entity;

import com.rasahub.product_inventory_service.enums.StockLocationType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_locations")
public class StockLocation {

    @Id
    @NotBlank(message = "Location code is required")
    @Size(
            max = 5,
            message = "Location code must not exceed 5 characters"
    )
    @Pattern(
            regexp = "^(Main|BR[0-9]{3})$",
            message = "Invalid stock location code"
    )
    @Column(
            name = "location_code",
            nullable = false,
            length = 5,
            updatable = false
    )
    private String locationCode;

    @NotBlank(message = "Location name is required")
    @Size(
            max = 150,
            message = "Location name must not exceed 150 characters"
    )
    @Column(
            name = "location_name",
            nullable = false,
            length = 150
    )
    private String locationName;

    @NotNull(message = "Location type is required")
    @Enumerated(EnumType.STRING)
    @Column(
            name = "location_type",
            nullable = false,
            length = 20,
            updatable = false
    )
    private StockLocationType locationType;

    @Column(name = "active", nullable = false)
    private boolean active = true;

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

    protected StockLocation() {
    }

    public StockLocation(
            String locationCode,
            String locationName,
            StockLocationType locationType
    ) {
        this.locationCode = locationCode;
        this.locationName = locationName;
        this.locationType = locationType;
        this.active = true;

        validateLocationType();
    }

    private void validateLocationType() {

        if (locationType == null) {
            throw new IllegalArgumentException(
                    "Location type is required"
            );
        }

        if (locationCode == null
                || !locationCode.matches("^(Main|BR[0-9]{3})$")) {
            throw new IllegalArgumentException(
                    "Invalid stock location code"
            );
        }

        if ("Main".equals(locationCode)) {

            if (locationType != StockLocationType.MAIN_WAREHOUSE) {
                throw new IllegalArgumentException(
                        "Main must be a MAIN_WAREHOUSE"
                );
            }

        } else {

            if (locationType != StockLocationType.BRANCH) {
                throw new IllegalArgumentException(
                        "Branch locations must have BRANCH type"
                );
            }
        }
    }

    @PrePersist
    protected void onCreate() {
        validateLocationType();

        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        validateLocationType();
        this.updatedAt = LocalDateTime.now();
    }

    public String getLocationCode() {
        return locationCode;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public StockLocationType getLocationType() {
        return locationType;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
