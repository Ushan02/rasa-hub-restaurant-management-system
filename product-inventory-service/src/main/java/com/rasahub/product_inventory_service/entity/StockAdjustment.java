package com.rasahub.product_inventory_service.entity;

import com.rasahub.product_inventory_service.enums.StockAdjustmentDirection;
import com.rasahub.product_inventory_service.enums.StockAdjustmentReason;
import com.rasahub.product_inventory_service.enums.StockAdjustmentStatus;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Entity
@Table(
        name = "stock_adjustments",
        indexes = {
                @Index(
                        name = "idx_stock_adjustment_location",
                        columnList = "location_code"
                ),
                @Index(
                        name = "idx_stock_adjustment_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_stock_adjustment_created_at",
                        columnList = "created_at"
                ),
                @Index(
                        name = "idx_stock_adjustment_created_by",
                        columnList = "created_by_account_id"
                )
        }
)
public class StockAdjustment {

    private static final Set<String> STOCK_LOCATION_CODES =
            Set.of("Main", "BR001", "BR002", "BR003");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "location_code",
            referencedColumnName = "location_code",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_stock_adjustment_location")
    )
    private StockLocation stockLocation;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "adjustment_reason", nullable = false, length = 30)
    private StockAdjustmentReason adjustmentReason;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 10)
    private StockAdjustmentDirection direction;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private StockAdjustmentStatus status = StockAdjustmentStatus.DRAFT;

    @NotNull
    @Positive
    @Column(name = "created_by_account_id", nullable = false, updatable = false)
    private Long createdByAccountId;

    @Positive
    @Column(name = "submitted_by_account_id")
    private Long submittedByAccountId;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Positive
    @Column(name = "reviewed_by_account_id")
    private Long reviewedByAccountId;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Size(max = 500)
    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Positive
    @Column(name = "posted_by_account_id")
    private Long postedByAccountId;

    @Column(name = "posted_at")
    private LocalDateTime postedAt;

    @Size(max = 300)
    @Column(name = "remarks", length = 300)
    private String remarks;

    @Valid
    @NotEmpty
    @OneToMany(
            mappedBy = "stockAdjustment",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<StockAdjustmentItem> items = new ArrayList<>();

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @NotNull
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @NotNull
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected StockAdjustment() {
    }

    public StockAdjustment(
            StockLocation stockLocation,
            StockAdjustmentReason adjustmentReason,
            StockAdjustmentDirection direction,
            Long createdByAccountId,
            String remarks
    ) {
        validateLocation(stockLocation);
        validateReasonAndDirection(adjustmentReason, direction);
        validateAccountId(createdByAccountId);
        validateRemarks(adjustmentReason, remarks);

        this.stockLocation = stockLocation;
        this.adjustmentReason = adjustmentReason;
        this.direction = direction;
        this.createdByAccountId = createdByAccountId;
        this.remarks = normalizeText(remarks);
        this.status = StockAdjustmentStatus.DRAFT;
    }

    public void addItem(
            InventoryItem inventoryItem,
            java.math.BigDecimal quantity
    ) {
        validateDraft();
        if (inventoryItem == null
                || inventoryItem.getId() == null
                || !inventoryItem.isActive()) {
            throw new IllegalArgumentException(
                    "A saved, active inventory item is required"
            );
        }

        boolean duplicate = items.stream()
                .anyMatch(existing ->
                        existing.getInventoryItem().getId()
                                .equals(inventoryItem.getId())
                );
        if (duplicate) {
            throw new IllegalArgumentException(
                    "Duplicate inventory item in stock adjustment"
            );
        }

        items.add(new StockAdjustmentItem(this, inventoryItem, quantity));
    }

    public void updateDraft(
            StockLocation location,
            StockAdjustmentReason reason,
            StockAdjustmentDirection adjustmentDirection,
            String newRemarks
    ) {
        validateDraft();
        validateLocation(location);
        validateReasonAndDirection(reason, adjustmentDirection);
        validateRemarks(reason, newRemarks);

        this.stockLocation = location;
        this.adjustmentReason = reason;
        this.direction = adjustmentDirection;
        this.remarks = normalizeText(newRemarks);
    }

    public void replaceItems(List<StockAdjustmentItem> newItems) {
        validateDraft();
        if (newItems == null || newItems.isEmpty()) {
            throw new IllegalArgumentException(
                    "Stock adjustment must contain at least one item"
            );
        }

        Map<Long, StockAdjustmentItem> existingByInventoryId =
                new HashMap<>();
        for (StockAdjustmentItem existing : items) {
            existingByInventoryId.put(
                    existing.getInventoryItem().getId(),
                    existing
            );
        }

        List<StockAdjustmentItem> reconciled =
                new ArrayList<>(newItems.size());
        Set<Long> itemIds = new HashSet<>();

        for (StockAdjustmentItem item : newItems) {
            if (item == null
                    || item.getInventoryItem() == null
                    || item.getInventoryItem().getId() == null) {
                throw new IllegalArgumentException(
                        "A valid stock adjustment item is required"
                );
            }

            Long inventoryItemId = item.getInventoryItem().getId();
            if (!itemIds.add(inventoryItemId)) {
                throw new IllegalArgumentException(
                        "Duplicate inventory item in stock adjustment"
                );
            }

            StockAdjustmentItem existing =
                    existingByInventoryId.get(inventoryItemId);
            if (existing != null) {
                existing.changeQuantity(item.getAdjustmentQuantity());
                reconciled.add(existing);
            } else {
                item.attachTo(this);
                reconciled.add(item);
            }
        }

        items.clear();
        items.addAll(reconciled);
    }

    public void submit(Long accountId) {
        validateDraft();
        validateAccountId(accountId);
        if (!createdByAccountId.equals(accountId)) {
            throw new IllegalStateException(
                    "Only the adjustment creator can submit it"
            );
        }
        validateItemsPresent();

        submittedByAccountId = accountId;
        submittedAt = LocalDateTime.now();
        status = StockAdjustmentStatus.PENDING_APPROVAL;
    }

    public void approve(Long accountId) {
        validatePendingReview(accountId);

        reviewedByAccountId = accountId;
        reviewedAt = LocalDateTime.now();
        rejectionReason = null;
        status = StockAdjustmentStatus.APPROVED;
    }

    public void reject(Long accountId, String reason) {
        validatePendingReview(accountId);
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException(
                    "Rejection reason is required"
            );
        }
        if (reason.trim().length() > 500) {
            throw new IllegalArgumentException(
                    "Rejection reason must not exceed 500 characters"
            );
        }

        reviewedByAccountId = accountId;
        reviewedAt = LocalDateTime.now();
        rejectionReason = reason.trim();
        status = StockAdjustmentStatus.REJECTED;
    }

    public void post(Long accountId) {
        if (status != StockAdjustmentStatus.APPROVED) {
            throw new IllegalStateException(
                    "Only APPROVED adjustments can be posted"
            );
        }
        validateAccountId(accountId);
        if (createdByAccountId.equals(accountId)
                || accountId.equals(reviewedByAccountId)) {
            throw new IllegalStateException(
                    "Adjustment creator and reviewer cannot post it"
            );
        }
        validateItemsPresent();

        postedByAccountId = accountId;
        postedAt = LocalDateTime.now();
        status = StockAdjustmentStatus.POSTED;
    }

    private void validatePendingReview(Long accountId) {
        if (status != StockAdjustmentStatus.PENDING_APPROVAL) {
            throw new IllegalStateException(
                    "Only PENDING_APPROVAL adjustments can be reviewed"
            );
        }
        validateAccountId(accountId);
        if (createdByAccountId.equals(accountId)) {
            throw new IllegalStateException(
                    "Adjustment creator cannot review their own adjustment"
            );
        }
        validateItemsPresent();
    }

    private void validateDraft() {
        if (status != StockAdjustmentStatus.DRAFT) {
            throw new IllegalStateException(
                    "Only DRAFT adjustments can be modified"
            );
        }
    }

    private void validateItemsPresent() {
        if (items == null || items.isEmpty()) {
            throw new IllegalStateException(
                    "Stock adjustment must contain at least one item"
            );
        }
    }

    private void validateLocation(StockLocation location) {
        if (location == null
                || !STOCK_LOCATION_CODES.contains(location.getLocationCode())
                || !location.isActive()) {
            throw new IllegalArgumentException(
                    "An active Main or branch stock location is required"
            );
        }
    }

    private void validateReasonAndDirection(
            StockAdjustmentReason reason,
            StockAdjustmentDirection adjustmentDirection
    ) {
        if (reason == null || !reason.allows(adjustmentDirection)) {
            throw new IllegalArgumentException(
                    "Adjustment reason does not permit the selected direction"
            );
        }
    }

    private void validateRemarks(
            StockAdjustmentReason reason,
            String value
    ) {
        if (value != null && value.trim().length() > 300) {
            throw new IllegalArgumentException(
                    "Remarks must not exceed 300 characters"
            );
        }
        if (reason == StockAdjustmentReason.MANUAL_CORRECTION
                && (value == null || value.isBlank())) {
            throw new IllegalArgumentException(
                    "Manual corrections require a justification"
            );
        }
    }

    private void validateAccountId(Long accountId) {
        if (accountId == null || accountId <= 0) {
            throw new IllegalArgumentException(
                    "Valid account ID is required"
            );
        }
    }

    private String normalizeText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    @PrePersist
    protected void onCreate() {
        validateLocation(stockLocation);
        validateReasonAndDirection(adjustmentReason, direction);
        validateAccountId(createdByAccountId);
        validateRemarks(adjustmentReason, remarks);
        validateItemsPresent();
        if (status != StockAdjustmentStatus.DRAFT) {
            throw new IllegalStateException(
                    "New stock adjustments must start as DRAFT"
            );
        }

        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        validateItemsPresent();
        if (status == StockAdjustmentStatus.PENDING_APPROVAL
                && (submittedByAccountId == null || submittedAt == null)) {
            throw new IllegalStateException(
                    "Submitted adjustments require submission audit details"
            );
        }
        if ((status == StockAdjustmentStatus.APPROVED
                || status == StockAdjustmentStatus.REJECTED
                || status == StockAdjustmentStatus.POSTED)
                && (reviewedByAccountId == null || reviewedAt == null)) {
            throw new IllegalStateException(
                    "Reviewed adjustments require reviewer audit details"
            );
        }
        if (status == StockAdjustmentStatus.REJECTED
                && (rejectionReason == null || rejectionReason.isBlank())) {
            throw new IllegalStateException(
                    "Rejected adjustments require a rejection reason"
            );
        }
        if (status == StockAdjustmentStatus.POSTED
                && (postedByAccountId == null || postedAt == null)) {
            throw new IllegalStateException(
                    "Posted adjustments require posting audit details"
            );
        }
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public StockLocation getStockLocation() {
        return stockLocation;
    }

    public StockAdjustmentReason getAdjustmentReason() {
        return adjustmentReason;
    }

    public StockAdjustmentDirection getDirection() {
        return direction;
    }

    public StockAdjustmentStatus getStatus() {
        return status;
    }

    public Long getCreatedByAccountId() {
        return createdByAccountId;
    }

    public Long getSubmittedByAccountId() {
        return submittedByAccountId;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public Long getReviewedByAccountId() {
        return reviewedByAccountId;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public Long getPostedByAccountId() {
        return postedByAccountId;
    }

    public LocalDateTime getPostedAt() {
        return postedAt;
    }

    public String getRemarks() {
        return remarks;
    }

    public List<StockAdjustmentItem> getItems() {
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
