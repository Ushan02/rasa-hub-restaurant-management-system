
package com.rasahub.product_inventory_service.entity;

import com.rasahub.product_inventory_service.enums.PurchaseRequestStatus;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "purchase_requests",
        indexes = {
                @Index(
                        name = "idx_pr_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_pr_requester",
                        columnList = "requested_by_account_id"
                )
        }
)
public class PurchaseRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "destination_location_code",
            referencedColumnName = "location_code",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(
                    name = "fk_pr_destination"
            )
    )
    private StockLocation destinationLocation;

    @NotNull
    @Positive
    @Column(
            name = "requested_by_account_id",
            nullable = false,
            updatable = false
    )
    private Long requestedByAccountId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 10
    )
    private PurchaseRequestStatus status =
            PurchaseRequestStatus.PENDING;

    @Positive
    @Column(name = "reviewed_by_account_id")
    private Long reviewedByAccountId;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Size(
            max = 300,
            message = "Remarks must not exceed 300 characters"
    )
    @Column(
            name = "remarks",
            length = 300
    )
    private String remarks;

    @Size(
            max = 500,
            message = "Rejection reason must not exceed 500 characters"
    )
    @Column(
            name = "rejection_reason",
            length = 500
    )
    private String rejectionReason;

    @Valid
    @NotEmpty
    @OneToMany(
            mappedBy = "purchaseRequest",
            cascade = CascadeType.ALL
    )
    private List<PurchaseRequestItem> items =
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

    protected PurchaseRequest() {
    }

    public PurchaseRequest(
            StockLocation destinationLocation,
            Long requestedByAccountId,
            String remarks
    ) {
        if (destinationLocation == null
                || !"Main".equals(
                destinationLocation.getLocationCode()
        )
                || !destinationLocation.isActive()) {
            throw new IllegalArgumentException(
                    "Purchase request destination must be active Main"
            );
        }

        if (requestedByAccountId == null
                || requestedByAccountId <= 0) {
            throw new IllegalArgumentException(
                    "Valid requester account ID is required"
            );
        }

        if (remarks != null
                && remarks.trim().length() > 300) {
            throw new IllegalArgumentException(
                    "Remarks must not exceed 300 characters"
            );
        }

        this.destinationLocation = destinationLocation;
        this.requestedByAccountId = requestedByAccountId;
        this.remarks = normalizeOptionalText(remarks);
        this.status = PurchaseRequestStatus.PENDING;
    }

    public void addItem(
            InventoryItem inventoryItem,
            BigDecimal requestedQuantity
    ) {
        if (status != PurchaseRequestStatus.PENDING) {
            throw new IllegalStateException(
                    "Items can only be added to pending requests"
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
                        item.getInventoryItem()
                                .getId()
                                .equals(inventoryItem.getId())
                );

        if (duplicate) {
            throw new IllegalArgumentException(
                    "Inventory item already exists in this request"
            );
        }

        PurchaseRequestItem item =
                new PurchaseRequestItem(
                        this,
                        inventoryItem,
                        requestedQuantity
                );

        items.add(item);
    }

    public void approve(Long managerAccountId) {
        validateReview(managerAccountId);

        this.status = PurchaseRequestStatus.APPROVED;
        this.reviewedByAccountId = managerAccountId;
        this.reviewedAt = LocalDateTime.now();
        this.rejectionReason = null;
    }

    public void reject(
            Long managerAccountId,
            String reason
    ) {
        validateReview(managerAccountId);

        if (reason == null
                || reason.isBlank()
                || reason.trim().length() > 500) {
            throw new IllegalArgumentException(
                    "Valid rejection reason is required"
            );
        }

        this.status = PurchaseRequestStatus.REJECTED;
        this.reviewedByAccountId = managerAccountId;
        this.reviewedAt = LocalDateTime.now();
        this.rejectionReason = reason.trim();
    }

    public void markPurchaseOrderCreated() {
        if (status != PurchaseRequestStatus.APPROVED) {
            throw new IllegalStateException(
                    "Only approved requests can create a PO"
            );
        }

        this.status = PurchaseRequestStatus.PO_CREATED;
    }

    private void validateReview(Long managerAccountId) {
        if (managerAccountId == null
                || managerAccountId <= 0) {
            throw new IllegalArgumentException(
                    "Valid manager account ID is required"
            );
        }

        if (status != PurchaseRequestStatus.PENDING) {
            throw new IllegalStateException(
                    "Purchase request is already reviewed"
            );
        }

        if (requestedByAccountId.equals(managerAccountId)) {
            throw new IllegalStateException(
                    "Requester cannot review their own request"
            );
        }

        if (items.isEmpty()) {
            throw new IllegalStateException(
                    "Purchase request must contain at least one item"
            );
        }
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private void validateState() {
        if (destinationLocation == null
                || !"Main".equals(
                destinationLocation.getLocationCode()
        )) {
            throw new IllegalStateException(
                    "Destination must be Main warehouse"
            );
        }

        if (requestedByAccountId == null
                || requestedByAccountId <= 0) {
            throw new IllegalStateException(
                    "Invalid requester account ID"
            );
        }

        if (status == null) {
            throw new IllegalStateException(
                    "Purchase request status is required"
            );
        }

        if (remarks != null && remarks.length() > 300) {
            throw new IllegalStateException(
                    "Remarks must not exceed 300 characters"
            );
        }

        if (items == null || items.isEmpty()) {
            throw new IllegalStateException(
                    "Purchase request must contain items"
            );
        }

        if (status == PurchaseRequestStatus.PENDING) {
            if (reviewedByAccountId != null
                    || reviewedAt != null
                    || rejectionReason != null) {
                throw new IllegalStateException(
                        "Pending request cannot have review details"
                );
            }
        } else {
            if (reviewedByAccountId == null
                    || reviewedByAccountId <= 0
                    || reviewedAt == null) {
                throw new IllegalStateException(
                        "Reviewed request requires reviewer and date"
                );
            }

            if (requestedByAccountId.equals(
                    reviewedByAccountId
            )) {
                throw new IllegalStateException(
                        "Requester cannot review their own request"
                );
            }

            if (status == PurchaseRequestStatus.REJECTED) {
                if (rejectionReason == null
                        || rejectionReason.isBlank()
                        || rejectionReason.length() > 500) {
                    throw new IllegalStateException(
                            "Rejected request requires a valid reason"
                    );
                }
            } else if (rejectionReason != null) {
                throw new IllegalStateException(
                        "Only rejected requests may have a rejection reason"
                );
            }
        }
    }

    @PrePersist
    protected void onCreate() {
        if (status != PurchaseRequestStatus.PENDING) {
            throw new IllegalStateException(
                    "New purchase requests must start as PENDING"
            );
        }

        if (destinationLocation == null
                || !destinationLocation.isActive()) {
            throw new IllegalStateException(
                    "Main warehouse must be active"
            );
        }

        validateState();

        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        validateState();
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public StockLocation getDestinationLocation() {
        return destinationLocation;
    }

    public Long getRequestedByAccountId() {
        return requestedByAccountId;
    }

    public PurchaseRequestStatus getStatus() {
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

    public List<PurchaseRequestItem> getItems() {
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
