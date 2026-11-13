
package com.rasahub.product_inventory_service.entity;

import com.rasahub.product_inventory_service.enums.MeasurementUnit;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Entity
@Table(
        name = "purchase_request_items",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_pr_items_request_inventory",
                        columnNames = {
                                "purchase_request_id",
                                "inventory_item_id"
                        }
                )
        }
)
public class PurchaseRequestItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "purchase_request_id",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(
                    name = "fk_pr_items_request"
            )
    )
    private PurchaseRequest purchaseRequest;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "inventory_item_id",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(
                    name = "fk_pr_items_inventory_item"
            )
    )
    private InventoryItem inventoryItem;

    @NotNull(
            message = "Requested quantity is required"
    )
    @DecimalMin(
            value = "0.000",
            inclusive = false,
            message = "Requested quantity must be greater than zero"
    )
    @Digits(
            integer = 15,
            fraction = 3,
            message = "Maximum 15 integer digits and 3 decimal places allowed"
    )
    @Column(
            name = "requested_quantity",
            nullable = false,
            precision = 18,
            scale = 3
    )
    private BigDecimal requestedQuantity;

    protected PurchaseRequestItem() {
    }

    public PurchaseRequestItem(
            PurchaseRequest purchaseRequest,
            InventoryItem inventoryItem,
            BigDecimal requestedQuantity
    ) {
        if (purchaseRequest == null) {
            throw new IllegalArgumentException(
                    "Purchase request is required"
            );
        }

        if (inventoryItem == null
                || inventoryItem.getId() == null
                || !inventoryItem.isActive()) {
            throw new IllegalArgumentException(
                    "A saved, active inventory item is required"
            );
        }

        validateQuantity(
                inventoryItem,
                requestedQuantity
        );

        this.purchaseRequest = purchaseRequest;
        this.inventoryItem = inventoryItem;
        this.requestedQuantity = requestedQuantity;
    }

    private void validateQuantity(
            InventoryItem inventoryItem,
            BigDecimal quantity
    ) {
        if (quantity == null) {
            throw new IllegalArgumentException(
                    "Requested quantity is required"
            );
        }

        if (quantity.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Requested quantity must be greater than zero"
            );
        }

        if (quantity.scale() > 3) {
            throw new IllegalArgumentException(
                    "Maximum 3 decimal places allowed"
            );
        }

        if (quantity.precision() - quantity.scale() > 15) {
            throw new IllegalArgumentException(
                    "Maximum 15 integer digits allowed"
            );
        }

        if (inventoryItem.getMeasurementUnit()
                == MeasurementUnit.PCS
                && quantity.stripTrailingZeros().scale() > 0) {

            throw new IllegalArgumentException(
                    "PCS quantity must be a whole number"
            );
        }
    }

    @PrePersist
    protected void onCreate() {
        if (purchaseRequest == null
                || inventoryItem == null
                || inventoryItem.getId() == null
                || !inventoryItem.isActive()) {
            throw new IllegalStateException(
                    "Valid purchase request and active inventory item are required"
            );
        }

        validateQuantity(inventoryItem, requestedQuantity);
    }

    public Long getId() {
        return id;
    }

    public PurchaseRequest getPurchaseRequest() {
        return purchaseRequest;
    }

    public InventoryItem getInventoryItem() {
        return inventoryItem;
    }

    public BigDecimal getRequestedQuantity() {
        return requestedQuantity;
    }
}
