
package com.rasahub.product_inventory_service.entity;

import com.rasahub.product_inventory_service.enums.MeasurementUnit;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Entity
@Table(
        name = "branch_stock_request_items",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_bsr_item_request_inventory",
                        columnNames = {
                                "branch_stock_request_id",
                                "inventory_item_id"
                        }
                )
        }
)
public class BranchStockRequestItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "branch_stock_request_id",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(
                    name = "fk_bsr_item_request"
            )
    )
    private BranchStockRequest branchStockRequest;

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
                    name = "fk_bsr_item_inventory"
            )
    )
    private InventoryItem inventoryItem;

    @NotNull
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
            updatable = false,
            precision = 18,
            scale = 3
    )
    private BigDecimal requestedQuantity;

    @NotNull
    @DecimalMin(
            value = "0.000",
            message = "Received quantity cannot be negative"
    )
    @Digits(
            integer = 15,
            fraction = 3,
            message = "Maximum 15 integer digits and 3 decimal places allowed"
    )
    @Column(
            name = "received_quantity",
            nullable = false,
            precision = 18,
            scale = 3
    )
    private BigDecimal receivedQuantity = BigDecimal.ZERO;

    @Version
    @Column(
            name = "version",
            nullable = false
    )
    private Long version;

    protected BranchStockRequestItem() {
    }

    public BranchStockRequestItem(
            BranchStockRequest branchStockRequest,
            InventoryItem inventoryItem,
            BigDecimal requestedQuantity
    ) {
        if (branchStockRequest == null) {
            throw new IllegalArgumentException(
                    "Branch stock request is required"
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
                requestedQuantity,
                false
        );

        this.branchStockRequest = branchStockRequest;
        this.inventoryItem = inventoryItem;
        this.requestedQuantity = requestedQuantity;
        this.receivedQuantity = BigDecimal.ZERO;
    }

    public void recordReceivedQuantity(
            BigDecimal quantity
    ) {
        validateQuantity(
                inventoryItem,
                quantity,
                false
        );

        BigDecimal updatedQuantity =
                receivedQuantity.add(quantity);

        validateQuantity(
                inventoryItem,
                updatedQuantity,
                true
        );

        if (updatedQuantity.compareTo(requestedQuantity) > 0) {
            throw new IllegalStateException(
                    "Received quantity cannot exceed requested quantity"
            );
        }

        this.receivedQuantity = updatedQuantity;
    }

    private void validateQuantity(
            InventoryItem item,
            BigDecimal quantity,
            boolean allowZero
    ) {
        if (quantity == null) {
            throw new IllegalArgumentException(
                    "Quantity is required"
            );
        }

        if (allowZero) {
            if (quantity.signum() < 0) {
                throw new IllegalArgumentException(
                        "Quantity cannot be negative"
                );
            }
        } else {
            if (quantity.signum() <= 0) {
                throw new IllegalArgumentException(
                        "Quantity must be greater than zero"
                );
            }
        }

        if (quantity.scale() > 3
                || quantity.precision() - quantity.scale() > 15) {
            throw new IllegalArgumentException(
                    "Invalid quantity precision"
            );
        }

        if (item.getMeasurementUnit() == MeasurementUnit.PCS
                && quantity.stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException(
                    "PCS quantity must be a whole number"
            );
        }
    }

    private void validateReceivedQuantity() {

        validateQuantity(
                inventoryItem,
                receivedQuantity,
                true
        );

        if (receivedQuantity.compareTo(requestedQuantity) > 0) {
            throw new IllegalStateException(
                    "Received quantity cannot exceed requested quantity"
            );
        }
    }

    @PrePersist
    protected void onCreate() {

        if (branchStockRequest == null
                || inventoryItem == null
                || inventoryItem.getId() == null) {
            throw new IllegalStateException(
                    "Branch request and inventory item are required"
            );
        }

        validateQuantity(
                inventoryItem,
                requestedQuantity,
                false
        );

        validateReceivedQuantity();

        if (receivedQuantity.signum() != 0) {
            throw new IllegalStateException(
                    "New request items must start with zero received quantity"
            );
        }
    }

    @PreUpdate
    protected void onUpdate() {
        validateReceivedQuantity();
    }

    @Transient
    public BigDecimal getOutstandingQuantity() {
        return requestedQuantity.subtract(receivedQuantity);
    }

    @Transient
    public boolean isFullyFulfilled() {
        return receivedQuantity.compareTo(requestedQuantity) == 0;
    }

    public Long getId() {
        return id;
    }

    public BranchStockRequest getBranchStockRequest() {
        return branchStockRequest;
    }

    public InventoryItem getInventoryItem() {
        return inventoryItem;
    }

    public BigDecimal getRequestedQuantity() {
        return requestedQuantity;
    }

    public BigDecimal getReceivedQuantity() {
        return receivedQuantity;
    }

    public Long getVersion() {
        return version;
    }
}
