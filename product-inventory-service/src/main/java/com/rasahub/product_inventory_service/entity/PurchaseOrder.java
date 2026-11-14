
package com.rasahub.product_inventory_service.entity;

import com.rasahub.product_inventory_service.enums.PurchaseOrderStatus;
import com.rasahub.product_inventory_service.enums.PurchaseRequestStatus;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "purchase_orders",
        indexes = {
                @Index(
                        name = "idx_po_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_po_supplier",
                        columnList = "supplier_id"
                )
        }
)
public class PurchaseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "purchase_request_id",
            nullable = false,
            updatable = false,
            unique = true,
            foreignKey = @ForeignKey(
                    name = "fk_po_purchase_request"
            )
    )
    private PurchaseRequest purchaseRequest;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "supplier_id",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(
                    name = "fk_po_supplier"
            )
    )
    private Supplier supplier;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "destination_location_code",
            referencedColumnName = "location_code",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(
                    name = "fk_po_destination"
            )
    )
    private StockLocation destinationLocation;

    @NotNull
    @Positive
    @Column(
            name = "created_by_account_id",
            nullable = false,
            updatable = false
    )
    private Long createdByAccountId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    private PurchaseOrderStatus status =
            PurchaseOrderStatus.ISSUED;

    @NotNull
    @Column(
            name = "expected_delivery_date",
            nullable = false
    )
    private LocalDate expectedDeliveryDate;

    @Size(
            max = 300,
            message = "Remarks must not exceed 300 characters"
    )
    @Column(name = "remarks", length = 300)
    private String remarks;

    @Size(
            max = 300,
            message = "Cancellation reason must not exceed 300 characters"
    )
    @Column(name = "cancellation_reason", length = 300)
    private String cancellationReason;

    @Valid
    @NotEmpty
    @OneToMany(
            mappedBy = "purchaseOrder",
            cascade = CascadeType.ALL
    )
    private List<PurchaseOrderItem> items =
            new ArrayList<>();

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @NotNull
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @NotNull
    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;

    protected PurchaseOrder() {
    }

    public PurchaseOrder(
            PurchaseRequest purchaseRequest,
            Supplier supplier,
            Long createdByAccountId,
            LocalDate expectedDeliveryDate,
            String remarks
    ) {
        if (purchaseRequest == null
                || purchaseRequest.getId() == null
                || purchaseRequest.getStatus()
                != PurchaseRequestStatus.APPROVED) {
            throw new IllegalArgumentException(
                    "An approved purchase request is required"
            );
        }

        if (supplier == null
                || supplier.getId() == null
                || !supplier.isActive()) {
            throw new IllegalArgumentException(
                    "A saved, active supplier is required"
            );
        }

        StockLocation destination =
                purchaseRequest.getDestinationLocation();

        if (destination == null
                || !"Main".equals(destination.getLocationCode())
                || !destination.isActive()) {
            throw new IllegalArgumentException(
                    "PO destination must be active Main warehouse"
            );
        }

        if (createdByAccountId == null
                || createdByAccountId <= 0) {
            throw new IllegalArgumentException(
                    "Valid creator account ID is required"
            );
        }

        if (expectedDeliveryDate == null
                || expectedDeliveryDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "Expected delivery date cannot be in the past"
            );
        }

        if (remarks != null
                && remarks.trim().length() > 300) {
            throw new IllegalArgumentException(
                    "Remarks must not exceed 300 characters"
            );
        }

        this.purchaseRequest = purchaseRequest;
        this.supplier = supplier;
        this.destinationLocation = destination;
        this.createdByAccountId = createdByAccountId;
        this.expectedDeliveryDate = expectedDeliveryDate;
        this.remarks = normalizeText(remarks);
        this.status = PurchaseOrderStatus.ISSUED;
    }

    public void addItem(
            InventoryItem inventoryItem,
            BigDecimal orderedQuantity,
            BigDecimal unitPrice
    ) {
        if (id != null || status != PurchaseOrderStatus.ISSUED) {
            throw new IllegalStateException(
                    "Items can only be added before PO creation"
            );
        }

        if (inventoryItem == null
                || inventoryItem.getId() == null
                || !inventoryItem.isActive()) {
            throw new IllegalArgumentException(
                    "A saved, active inventory item is required"
            );
        }

        boolean duplicate = items.stream()
                .anyMatch(item ->
                        item.getInventoryItem().getId()
                                .equals(inventoryItem.getId())
                );

        if (duplicate) {
            throw new IllegalArgumentException(
                    "Duplicate purchase order item"
            );
        }

        items.add(
                new PurchaseOrderItem(
                        this,
                        inventoryItem,
                        orderedQuantity,
                        unitPrice
                )
        );
    }

    public void recordAcceptedReceipt(
            Long inventoryItemId,
            BigDecimal quantity
    ) {
        if (status != PurchaseOrderStatus.ISSUED
                && status != PurchaseOrderStatus.PARTIALLY_RECEIVED) {
            throw new IllegalStateException(
                    "Purchase order cannot receive stock"
            );
        }

        PurchaseOrderItem item = items.stream()
                .filter(value ->
                        value.getInventoryItem().getId()
                                .equals(inventoryItemId)
                )
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Item not found in purchase order"
                        )
                );

        item.recordAcceptedQuantity(quantity);

        boolean fullyReceived = items.stream()
                .allMatch(PurchaseOrderItem::isFullyReceived);

        this.status = fullyReceived
                ? PurchaseOrderStatus.RECEIVED
                : PurchaseOrderStatus.PARTIALLY_RECEIVED;
    }

    public void cancel(String reason) {
        if (status != PurchaseOrderStatus.ISSUED) {
            throw new IllegalStateException(
                    "Only unreceived purchase orders can be cancelled"
            );
        }

        if (reason == null
                || reason.isBlank()
                || reason.trim().length() > 300) {
            throw new IllegalArgumentException(
                    "Valid cancellation reason is required"
            );
        }

        this.status = PurchaseOrderStatus.CANCELLED;
        this.cancellationReason = reason.trim();
    }

    private String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    @Transient
    public BigDecimal getTotalAmount() {
        return items.stream()
                .map(PurchaseOrderItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @PrePersist
    protected void onCreate() {
        if (items == null || items.isEmpty()) {
            throw new IllegalStateException(
                    "Purchase order must contain items"
            );
        }

        if (status != PurchaseOrderStatus.ISSUED) {
            throw new IllegalStateException(
                    "New purchase orders must start as ISSUED"
            );
        }

        if (purchaseRequest == null
                || purchaseRequest.getStatus()
                != PurchaseRequestStatus.PO_CREATED) {
            throw new IllegalStateException(
                    "Source PR must be marked PO_CREATED"
            );
        }

        if (supplier == null
                || destinationLocation == null
                || !"Main".equals(
                destinationLocation.getLocationCode()
        )) {
            throw new IllegalStateException(
                    "Valid supplier and Main destination are required"
            );
        }

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

    public PurchaseRequest getPurchaseRequest() {
        return purchaseRequest;
    }

    public Supplier getSupplier() {
        return supplier;
    }

    public StockLocation getDestinationLocation() {
        return destinationLocation;
    }

    public Long getCreatedByAccountId() {
        return createdByAccountId;
    }

    public PurchaseOrderStatus getStatus() {
        return status;
    }

    public LocalDate getExpectedDeliveryDate() {
        return expectedDeliveryDate;
    }

    public String getRemarks() {
        return remarks;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public List<PurchaseOrderItem> getItems() {
        return List.copyOf(items);
    }

    public Long getVersion() {
        return version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
