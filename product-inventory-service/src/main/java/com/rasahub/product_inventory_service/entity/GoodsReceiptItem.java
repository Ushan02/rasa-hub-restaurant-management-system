
package com.rasahub.product_inventory_service.entity;

import com.rasahub.product_inventory_service.enums.MeasurementUnit;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

@Entity
@Table(
        name = "goods_receipt_items",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_grn_inventory_item",
                        columnNames = {
                                "goods_receipt_note_id",
                                "inventory_item_id"
                        }
                )
        }
)
public class GoodsReceiptItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "goods_receipt_note_id",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(
                    name = "fk_grn_item_receipt"
            )
    )
    private GoodsReceiptNote goodsReceiptNote;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "inventory_item_id",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(
                    name = "fk_grn_item_inventory"
            )
    )
    private InventoryItem inventoryItem;

    @NotNull
    @DecimalMin(
            value = "0.000",
            inclusive = false,
            message = "Delivered quantity must be greater than zero"
    )
    @Digits(integer = 15, fraction = 3)
    @Column(
            name = "delivered_quantity",
            nullable = false,
            updatable = false,
            precision = 18,
            scale = 3
    )
    private BigDecimal deliveredQuantity;

    @NotNull
    @DecimalMin(
            value = "0.000",
            message = "Accepted quantity cannot be negative"
    )
    @Digits(integer = 15, fraction = 3)
    @Column(
            name = "accepted_quantity",
            nullable = false,
            updatable = false,
            precision = 18,
            scale = 3
    )
    private BigDecimal acceptedQuantity;

    @NotNull
    @DecimalMin(
            value = "0.000",
            message = "Rejected quantity cannot be negative"
    )
    @Digits(integer = 15, fraction = 3)
    @Column(
            name = "rejected_quantity",
            nullable = false,
            updatable = false,
            precision = 18,
            scale = 3
    )
    private BigDecimal rejectedQuantity;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected GoodsReceiptItem() {
    }

    public GoodsReceiptItem(
            GoodsReceiptNote goodsReceiptNote,
            InventoryItem inventoryItem,
            BigDecimal deliveredQuantity,
            BigDecimal acceptedQuantity,
            BigDecimal rejectedQuantity
    ) {
        if (goodsReceiptNote == null) {
            throw new IllegalArgumentException(
                    "Goods receipt note is required"
            );
        }

        if (inventoryItem == null
                || inventoryItem.getId() == null) {
            throw new IllegalArgumentException(
                    "A saved inventory item is required"
            );
        }

        validateQuantities(
                inventoryItem,
                deliveredQuantity,
                acceptedQuantity,
                rejectedQuantity
        );

        this.goodsReceiptNote = goodsReceiptNote;
        this.inventoryItem = inventoryItem;
        this.deliveredQuantity = deliveredQuantity;
        this.acceptedQuantity = acceptedQuantity;
        this.rejectedQuantity = rejectedQuantity;
    }

    private void validateQuantities(
            InventoryItem inventoryItem,
            BigDecimal delivered,
            BigDecimal accepted,
            BigDecimal rejected
    ) {
        validateQuantity(delivered, "Delivered", false);
        validateQuantity(accepted, "Accepted", true);
        validateQuantity(rejected, "Rejected", true);

        BigDecimal total =
                accepted.add(rejected);

        if (delivered.compareTo(total) != 0) {
            throw new IllegalArgumentException(
                    "Delivered quantity must equal accepted plus rejected quantity"
            );
        }

        if (inventoryItem.getMeasurementUnit()
                == MeasurementUnit.PCS) {

            if (!isWholeNumber(delivered)
                    || !isWholeNumber(accepted)
                    || !isWholeNumber(rejected)) {
                throw new IllegalArgumentException(
                        "PCS quantities must be whole numbers"
                );
            }
        }
    }

    private void validateQuantity(
            BigDecimal quantity,
            String fieldName,
            boolean allowZero
    ) {
        if (quantity == null) {
            throw new IllegalArgumentException(
                    fieldName + " quantity is required"
            );
        }

        if (allowZero) {
            if (quantity.signum() < 0) {
                throw new IllegalArgumentException(
                        fieldName + " quantity cannot be negative"
                );
            }
        } else {
            if (quantity.signum() <= 0) {
                throw new IllegalArgumentException(
                        fieldName + " quantity must be greater than zero"
                );
            }
        }

        if (quantity.scale() > 3
                || quantity.precision() - quantity.scale() > 15) {
            throw new IllegalArgumentException(
                    fieldName + " quantity has invalid precision"
            );
        }
    }

    private boolean isWholeNumber(BigDecimal quantity) {
        return quantity.stripTrailingZeros().scale() <= 0;
    }

    @PrePersist
    protected void onCreate() {
        if (goodsReceiptNote == null
                || inventoryItem == null
                || inventoryItem.getId() == null) {
            throw new IllegalStateException(
                    "GRN and inventory item are required"
            );
        }

        validateQuantities(
                inventoryItem,
                deliveredQuantity,
                acceptedQuantity,
                rejectedQuantity
        );
    }

    public Long getId() {
        return id;
    }

    public GoodsReceiptNote getGoodsReceiptNote() {
        return goodsReceiptNote;
    }

    public InventoryItem getInventoryItem() {
        return inventoryItem;
    }

    public BigDecimal getDeliveredQuantity() {
        return deliveredQuantity;
    }

    public BigDecimal getAcceptedQuantity() {
        return acceptedQuantity;
    }

    public BigDecimal getRejectedQuantity() {
        return rejectedQuantity;
    }

    public Long getVersion() {
        return version;
    }
}
