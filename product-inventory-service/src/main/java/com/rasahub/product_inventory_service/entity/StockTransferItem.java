
package com.rasahub.product_inventory_service.entity;

import com.rasahub.product_inventory_service.enums.MeasurementUnit;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Entity
@Table(
        name = "stock_transfer_items",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_transfer_item_inventory",
                        columnNames = {
                                "stock_transfer_id",
                                "inventory_item_id"
                        }
                )
        }
)
public class StockTransferItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "stock_transfer_id",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(
                    name = "fk_transfer_item_transfer"
            )
    )
    private StockTransfer stockTransfer;

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
                    name = "fk_transfer_item_inventory"
            )
    )
    private InventoryItem inventoryItem;

    @NotNull
    @DecimalMin(
            value = "0.000",
            inclusive = false,
            message = "Transfer quantity must be greater than zero"
    )
    @Digits(
            integer = 15,
            fraction = 3,
            message = "Maximum 15 integer digits and 3 decimal places allowed"
    )
    @Column(
            name = "transfer_quantity",
            nullable = false,
            updatable = false,
            precision = 18,
            scale = 3
    )
    private BigDecimal transferQuantity;

    @Version
    @Column(
            name = "version",
            nullable = false
    )
    private Long version;

    protected StockTransferItem() {
    }

    public StockTransferItem(
            StockTransfer stockTransfer,
            InventoryItem inventoryItem,
            BigDecimal transferQuantity
    ) {
        if (stockTransfer == null) {
            throw new IllegalArgumentException(
                    "Stock transfer is required"
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
                transferQuantity
        );

        this.stockTransfer = stockTransfer;
        this.inventoryItem = inventoryItem;
        this.transferQuantity = transferQuantity;
    }

    private void validateQuantity(
            InventoryItem item,
            BigDecimal quantity
    ) {
        if (quantity == null) {
            throw new IllegalArgumentException(
                    "Transfer quantity is required"
            );
        }

        if (quantity.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Transfer quantity must be greater than zero"
            );
        }

        if (quantity.scale() > 3
                || quantity.precision() - quantity.scale() > 15) {
            throw new IllegalArgumentException(
                    "Invalid transfer quantity precision"
            );
        }

        if (item.getMeasurementUnit() == MeasurementUnit.PCS
                && quantity.stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException(
                    "PCS transfer quantity must be a whole number"
            );
        }
    }

    @PrePersist
    protected void onCreate() {

        if (stockTransfer == null
                || inventoryItem == null
                || inventoryItem.getId() == null) {
            throw new IllegalStateException(
                    "Stock transfer and inventory item are required"
            );
        }

        validateQuantity(
                inventoryItem,
                transferQuantity
        );
    }

    public Long getId() {
        return id;
    }

    public StockTransfer getStockTransfer() {
        return stockTransfer;
    }

    public InventoryItem getInventoryItem() {
        return inventoryItem;
    }

    public BigDecimal getTransferQuantity() {
        return transferQuantity;
    }

    public Long getVersion() {
        return version;
    }
}
