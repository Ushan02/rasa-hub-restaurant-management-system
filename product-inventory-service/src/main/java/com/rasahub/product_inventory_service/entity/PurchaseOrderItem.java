
package com.rasahub.product_inventory_service.entity;

import com.rasahub.product_inventory_service.enums.MeasurementUnit;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(
        name = "purchase_order_items",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_po_items_order_inventory",
                        columnNames = {
                                "purchase_order_id",
                                "inventory_item_id"
                        }
                )
        }
)
public class PurchaseOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "purchase_order_id",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(
                    name = "fk_po_items_order"
            )
    )
    private PurchaseOrder purchaseOrder;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "inventory_item_id",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(
                    name = "fk_po_items_inventory_item"
            )
    )
    private InventoryItem inventoryItem;

    @NotNull
    @DecimalMin(
            value = "0.000",
            inclusive = false,
            message = "Ordered quantity must be greater than zero"
    )
    @Digits(
            integer = 15,
            fraction = 3,
            message = "Maximum 15 integer digits and 3 decimals allowed"
    )
    @Column(
            name = "ordered_quantity",
            nullable = false,
            updatable = false,
            precision = 18,
            scale = 3
    )
    private BigDecimal orderedQuantity;

    @NotNull
    @DecimalMin(
            value = "0.00",
            inclusive = false,
            message = "Unit price must be greater than zero"
    )
    @Digits(
            integer = 13,
            fraction = 2,
            message = "Maximum 13 integer digits and 2 decimals allowed"
    )
    @Column(
            name = "unit_price",
            nullable = false,
            updatable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal unitPrice;

    @NotNull
    @DecimalMin(
            value = "0.000",
            message = "Received quantity cannot be negative"
    )
    @Digits(
            integer = 15,
            fraction = 3,
            message = "Maximum 15 integer digits and 3 decimals allowed"
    )
    @Column(
            name = "received_quantity",
            nullable = false,
            precision = 18,
            scale = 3
    )
    private BigDecimal receivedQuantity = BigDecimal.ZERO;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected PurchaseOrderItem() {
    }

    public PurchaseOrderItem(
            PurchaseOrder purchaseOrder,
            InventoryItem inventoryItem,
            BigDecimal orderedQuantity,
            BigDecimal unitPrice
    ) {
        if (purchaseOrder == null) {
            throw new IllegalArgumentException(
                    "Purchase order is required"
            );
        }

        if (inventoryItem == null
                || inventoryItem.getId() == null
                || !inventoryItem.isActive()) {
            throw new IllegalArgumentException(
                    "A saved, active inventory item is required"
            );
        }

        validateQuantity(inventoryItem, orderedQuantity);
        validateUnitPrice(unitPrice);

        PurchaseRequestItem requestedItem =
                purchaseOrder.getPurchaseRequest()
                        .getItems()
                        .stream()
                        .filter(item ->
                                item.getInventoryItem()
                                        .getId()
                                        .equals(inventoryItem.getId())
                        )
                        .findFirst()
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Item is not in the approved PR"
                                )
                        );

        if (requestedItem.getRequestedQuantity()
                .compareTo(orderedQuantity) != 0) {
            throw new IllegalArgumentException(
                    "Ordered quantity must match approved PR quantity"
            );
        }

        this.purchaseOrder = purchaseOrder;
        this.inventoryItem = inventoryItem;
        this.orderedQuantity = orderedQuantity;
        this.unitPrice = unitPrice;
        this.receivedQuantity = BigDecimal.ZERO;
    }

    public void recordAcceptedQuantity(BigDecimal quantity) {

        validateQuantity(inventoryItem, quantity);

        BigDecimal updatedQuantity =
                receivedQuantity.add(quantity);

        if (updatedQuantity.compareTo(orderedQuantity) > 0) {
            throw new IllegalStateException(
                    "Received quantity cannot exceed ordered quantity"
            );
        }

        this.receivedQuantity = updatedQuantity;
    }

    private void validateQuantity(
            InventoryItem item,
            BigDecimal quantity
    ) {
        if (quantity == null
                || quantity.signum() <= 0
                || quantity.scale() > 3
                || quantity.precision() - quantity.scale() > 15) {
            throw new IllegalArgumentException(
                    "Invalid quantity"
            );
        }

        if (item.getMeasurementUnit() == MeasurementUnit.PCS
                && quantity.stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException(
                    "PCS quantity must be a whole number"
            );
        }
    }

    private void validateUnitPrice(BigDecimal price) {

        if (price == null
                || price.signum() <= 0
                || price.scale() > 2
                || price.precision() - price.scale() > 13) {
            throw new IllegalArgumentException(
                    "Invalid unit price"
            );
        }
    }

    private void validateReceivedQuantity() {

        if (receivedQuantity == null
                || receivedQuantity.signum() < 0
                || receivedQuantity.scale() > 3
                || receivedQuantity.precision()
                - receivedQuantity.scale() > 15
                || receivedQuantity.compareTo(orderedQuantity) > 0) {
            throw new IllegalStateException(
                    "Invalid received quantity"
            );
        }

        if (inventoryItem.getMeasurementUnit() == MeasurementUnit.PCS
                && receivedQuantity.stripTrailingZeros().scale() > 0) {
            throw new IllegalStateException(
                    "PCS received quantity must be a whole number"
            );
        }
    }

    @PrePersist
    protected void onCreate() {

        if (purchaseOrder == null
                || inventoryItem == null
                || inventoryItem.getId() == null) {
            throw new IllegalStateException(
                    "Purchase order and inventory item are required"
            );
        }

        validateQuantity(inventoryItem, orderedQuantity);
        validateUnitPrice(unitPrice);
        validateReceivedQuantity();

        if (receivedQuantity.signum() != 0) {
            throw new IllegalStateException(
                    "New PO items must start with zero received quantity"
            );
        }
    }

    @PreUpdate
    protected void onUpdate() {
        validateReceivedQuantity();
    }

    @Transient
    public BigDecimal getOutstandingQuantity() {
        return orderedQuantity.subtract(receivedQuantity);
    }

    @Transient
    public BigDecimal getLineTotal() {
        return orderedQuantity
                .multiply(unitPrice)
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Transient
    public boolean isFullyReceived() {
        return receivedQuantity.compareTo(orderedQuantity) == 0;
    }

    public Long getId() {
        return id;
    }

    public PurchaseOrder getPurchaseOrder() {
        return purchaseOrder;
    }

    public InventoryItem getInventoryItem() {
        return inventoryItem;
    }

    public BigDecimal getOrderedQuantity() {
        return orderedQuantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getReceivedQuantity() {
        return receivedQuantity;
    }

    public Long getVersion() {
        return version;
    }
}
