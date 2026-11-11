
package com.rasahub.product_inventory_service.entity;

import com.rasahub.product_inventory_service.enums.StockMovementType;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "stock_movements",
        indexes = {
                @Index(
                        name = "idx_stock_movements_location_date",
                        columnList = "location_code, created_at"
                ),
                @Index(
                        name = "idx_stock_movements_item_date",
                        columnList = "inventory_item_id, created_at"
                ),
                @Index(
                        name = "idx_stock_movements_reference",
                        columnList = "reference_type, reference_id"
                )
        }
)
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "location_code",
            referencedColumnName = "location_code",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(
                    name = "fk_stock_movements_location"
            )
    )
    private StockLocation stockLocation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "inventory_item_id",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(
                    name = "fk_stock_movements_inventory_item"
            )
    )
    private InventoryItem inventoryItem;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "movement_type",
            nullable = false,
            updatable = false,
            length = 30
    )
    private StockMovementType movementType;

    @Column(
            name = "quantity",
            nullable = false,
            updatable = false,
            precision = 18,
            scale = 3
    )
    private BigDecimal quantity;

    @Column(
            name = "quantity_before",
            nullable = false,
            updatable = false,
            precision = 18,
            scale = 3
    )
    private BigDecimal quantityBefore;

    @Column(
            name = "quantity_after",
            nullable = false,
            updatable = false,
            precision = 18,
            scale = 3
    )
    private BigDecimal quantityAfter;

    @Column(
            name = "reference_type",
            nullable = false,
            updatable = false,
            length = 30
    )
    private String referenceType;

    @Column(
            name = "reference_id",
            nullable = false,
            updatable = false,
            length = 64
    )
    private String referenceId;

    @Column(
            name = "performed_by_account_id",
            nullable = false,
            updatable = false
    )
    private Long performedByAccountId;

    @Column(
            name = "remarks",
            updatable = false,
            length = 500
    )
    private String remarks;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    protected StockMovement() {
    }

    public StockMovement(
            StockLocation stockLocation,
            InventoryItem inventoryItem,
            StockMovementType movementType,
            BigDecimal quantity,
            BigDecimal quantityBefore,
            BigDecimal quantityAfter,
            String referenceType,
            String referenceId,
            Long performedByAccountId,
            String remarks
    ) {
        if (stockLocation == null || inventoryItem == null) {
            throw new IllegalArgumentException(
                    "Location and inventory item are required"
            );
        }

        if (movementType == null) {
            throw new IllegalArgumentException(
                    "Movement type is required"
            );
        }

        validateQuantity(quantity, true);
        validateQuantity(quantityBefore, false);
        validateQuantity(quantityAfter, false);

        BigDecimal expectedAfter =
                movementType.increasesStock()
                        ? quantityBefore.add(quantity)
                        : quantityBefore.subtract(quantity);

        if (expectedAfter.compareTo(quantityAfter) != 0) {
            throw new IllegalArgumentException(
                    "Movement quantities do not match"
            );
        }

        if (referenceType == null
                || referenceType.isBlank()
                || referenceType.length() > 30) {
            throw new IllegalArgumentException(
                    "Invalid reference type"
            );
        }

        if (referenceId == null
                || referenceId.isBlank()
                || referenceId.length() > 64) {
            throw new IllegalArgumentException(
                    "Invalid reference ID"
            );
        }

        if (performedByAccountId == null
                || performedByAccountId < 1) {
            throw new IllegalArgumentException(
                    "Valid account ID is required"
            );
        }

        if (remarks != null && remarks.length() > 500) {
            throw new IllegalArgumentException(
                    "Remarks must not exceed 500 characters"
            );
        }

        this.stockLocation = stockLocation;
        this.inventoryItem = inventoryItem;
        this.movementType = movementType;
        this.quantity = quantity;
        this.quantityBefore = quantityBefore;
        this.quantityAfter = quantityAfter;
        this.referenceType = referenceType.trim();
        this.referenceId = referenceId.trim();
        this.performedByAccountId = performedByAccountId;
        this.remarks = remarks;
    }

    private void validateQuantity(
            BigDecimal value,
            boolean mustBePositive
    ) {
        if (value == null
                || value.signum() < 0
                || (mustBePositive && value.signum() == 0)
                || value.scale() > 3
                || value.precision() - value.scale() > 15) {

            throw new IllegalArgumentException(
                    "Invalid movement quantity"
            );
        }
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public StockLocation getStockLocation() {
        return stockLocation;
    }

    public InventoryItem getInventoryItem() {
        return inventoryItem;
    }

    public StockMovementType getMovementType() {
        return movementType;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getQuantityBefore() {
        return quantityBefore;
    }

    public BigDecimal getQuantityAfter() {
        return quantityAfter;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public Long getPerformedByAccountId() {
        return performedByAccountId;
    }

    public String getRemarks() {
        return remarks;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
