
package com.rasahub.product_inventory_service.entity;

import com.rasahub.product_inventory_service.enums.GoodsReceiptStatus;
import com.rasahub.product_inventory_service.enums.PurchaseOrderStatus;

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
        name = "goods_receipt_notes",
        indexes = {
                @Index(
                        name = "idx_grn_purchase_order",
                        columnList = "purchase_order_id"
                ),
                @Index(
                        name = "idx_grn_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_grn_received_date",
                        columnList = "received_date"
                )
        }
)
public class GoodsReceiptNote {

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
                    name = "fk_grn_purchase_order"
            )
    )
    private PurchaseOrder purchaseOrder;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "receiving_location_code",
            referencedColumnName = "location_code",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(
                    name = "fk_grn_receiving_location"
            )
    )
    private StockLocation receivingLocation;

    @NotNull
    @Positive
    @Column(
            name = "received_by_account_id",
            nullable = false,
            updatable = false
    )
    private Long receivedByAccountId;

    @NotNull
    @PastOrPresent
    @Column(
            name = "received_date",
            nullable = false,
            updatable = false
    )
    private LocalDate receivedDate;

    @Size(max = 100)
    @Column(
            name = "delivery_reference",
            length = 100,
            updatable = false
    )
    private String deliveryReference;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    private GoodsReceiptStatus status =
            GoodsReceiptStatus.DRAFT;

    @Positive
    @Column(name = "reviewed_by_account_id")
    private Long reviewedByAccountId;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Size(max = 300)
    @Column(name = "remarks", length = 300)
    private String remarks;

    @Size(max = 500)
    @Column(
            name = "rejection_reason",
            length = 500
    )
    private String rejectionReason;

    @Valid
    @NotEmpty
    @OneToMany(
            mappedBy = "goodsReceiptNote",
            cascade = CascadeType.ALL
    )
    private List<GoodsReceiptItem> items =
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

    protected GoodsReceiptNote() {
    }

    public GoodsReceiptNote(
            PurchaseOrder purchaseOrder,
            Long receivedByAccountId,
            LocalDate receivedDate,
            String deliveryReference,
            String remarks
    ) {
        if (purchaseOrder == null
                || purchaseOrder.getId() == null) {
            throw new IllegalArgumentException(
                    "A saved purchase order is required"
            );
        }

        if (purchaseOrder.getStatus()
                != PurchaseOrderStatus.ISSUED
                && purchaseOrder.getStatus()
                != PurchaseOrderStatus.PARTIALLY_RECEIVED) {
            throw new IllegalArgumentException(
                    "Purchase order is not eligible for receiving"
            );
        }

        StockLocation location =
                purchaseOrder.getDestinationLocation();

        if (location == null
                || !"Main".equals(location.getLocationCode())
                || !location.isActive()) {
            throw new IllegalArgumentException(
                    "GRN receiving location must be active Main warehouse"
            );
        }

        if (receivedByAccountId == null
                || receivedByAccountId <= 0) {
            throw new IllegalArgumentException(
                    "Valid receiving account ID is required"
            );
        }

        if (receivedDate == null
                || receivedDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "Received date cannot be in the future"
            );
        }

        validateLength(deliveryReference, 100, "Delivery reference");
        validateLength(remarks, 300, "Remarks");

        this.purchaseOrder = purchaseOrder;
        this.receivingLocation = location;
        this.receivedByAccountId = receivedByAccountId;
        this.receivedDate = receivedDate;
        this.deliveryReference = normalizeText(deliveryReference);
        this.remarks = normalizeText(remarks);
        this.status = GoodsReceiptStatus.DRAFT;
    }

    public void addItem(
            InventoryItem inventoryItem,
            BigDecimal deliveredQuantity,
            BigDecimal acceptedQuantity,
            BigDecimal rejectedQuantity
    ) {
        if (id != null || status != GoodsReceiptStatus.DRAFT) {
            throw new IllegalStateException(
                    "Items can only be added before GRN is saved"
            );
        }

        if (inventoryItem == null
                || inventoryItem.getId() == null) {
            throw new IllegalArgumentException(
                    "A saved inventory item is required"
            );
        }

        boolean belongsToOrder = purchaseOrder.getItems()
                .stream()
                .anyMatch(item ->
                        item.getInventoryItem().getId()
                                .equals(inventoryItem.getId())
                );

        if (!belongsToOrder) {
            throw new IllegalArgumentException(
                    "Inventory item is not in the purchase order"
            );
        }

        boolean duplicate = items.stream()
                .anyMatch(item ->
                        item.getInventoryItem().getId()
                                .equals(inventoryItem.getId())
                );

        if (duplicate) {
            throw new IllegalArgumentException(
                    "Duplicate inventory item in GRN"
            );
        }

        items.add(
                new GoodsReceiptItem(
                        this,
                        inventoryItem,
                        deliveredQuantity,
                        acceptedQuantity,
                        rejectedQuantity
                )
        );
    }

    public void submit() {
        if (status != GoodsReceiptStatus.DRAFT) {
            throw new IllegalStateException(
                    "Only draft GRNs can be submitted"
            );
        }

        validateItemsPresent();

        this.status = GoodsReceiptStatus.SUBMITTED;
    }

    public void confirm(Long managerAccountId) {
        if (status != GoodsReceiptStatus.SUBMITTED) {
            throw new IllegalStateException(
                    "Only submitted GRNs can be confirmed"
            );
        }

        validateReviewer(managerAccountId);
        validateItemsPresent();

        this.reviewedByAccountId = managerAccountId;
        this.reviewedAt = LocalDateTime.now();
        this.status = GoodsReceiptStatus.CONFIRMED;
        this.rejectionReason = null;
    }

    public void reject(
            Long managerAccountId,
            String reason
    ) {
        if (status != GoodsReceiptStatus.SUBMITTED) {
            throw new IllegalStateException(
                    "Only submitted GRNs can be rejected"
            );
        }

        validateReviewer(managerAccountId);

        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException(
                    "Rejection reason is required"
            );
        }

        validateLength(reason, 500, "Rejection reason");

        this.reviewedByAccountId = managerAccountId;
        this.reviewedAt = LocalDateTime.now();
        this.rejectionReason = reason.trim();
        this.status = GoodsReceiptStatus.REJECTED;
    }

    private void validateReviewer(Long managerAccountId) {
        if (managerAccountId == null
                || managerAccountId <= 0) {
            throw new IllegalArgumentException(
                    "Valid manager account ID is required"
            );
        }

        if (managerAccountId.equals(receivedByAccountId)) {
            throw new IllegalStateException(
                    "GRN creator cannot review their own GRN"
            );
        }
    }

    private void validateItemsPresent() {
        if (items == null || items.isEmpty()) {
            throw new IllegalStateException(
                    "GRN must contain at least one item"
            );
        }
    }

    private void validateLength(
            String value,
            int maxLength,
            String fieldName
    ) {
        if (value != null
                && value.trim().length() > maxLength) {
            throw new IllegalArgumentException(
                    fieldName + " exceeds maximum length"
            );
        }
    }

    private String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    @PrePersist
    protected void onCreate() {
        validateItemsPresent();

        if (status != GoodsReceiptStatus.DRAFT) {
            throw new IllegalStateException(
                    "New GRNs must be saved as DRAFT"
            );
        }

        if (purchaseOrder == null
                || receivingLocation == null
                || !"Main".equals(
                receivingLocation.getLocationCode()
        )) {
            throw new IllegalStateException(
                    "Valid purchase order and Main location are required"
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

    public PurchaseOrder getPurchaseOrder() {
        return purchaseOrder;
    }

    public StockLocation getReceivingLocation() {
        return receivingLocation;
    }

    public Long getReceivedByAccountId() {
        return receivedByAccountId;
    }

    public LocalDate getReceivedDate() {
        return receivedDate;
    }

    public String getDeliveryReference() {
        return deliveryReference;
    }

    public GoodsReceiptStatus getStatus() {
        return status;
    }

    public Long getReviewedByAccountId() {
        return reviewedByAccountId;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public String getRemarks() {
        return remarks;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public List<GoodsReceiptItem> getItems() {
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