package com.rasahub.product_inventory_service.entity;

import com.rasahub.product_inventory_service.enums.StockCountStatus;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(
        name = "stock_counts",
        indexes = {
                @Index(name = "idx_stock_count_location", columnList = "location_code"),
                @Index(name = "idx_stock_count_status", columnList = "status"),
                @Index(name = "idx_stock_count_date", columnList = "count_date"),
                @Index(name = "idx_stock_count_created_by", columnList = "created_by_account_id")
        }
)
public class StockCount {

    private static final Set<String> ALLOWED_LOCATIONS =
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
            foreignKey = @ForeignKey(name = "fk_stock_count_location")
    )
    private StockLocation stockLocation;

    @NotNull
    @Column(name = "count_date", nullable = false)
    private LocalDate countDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StockCountStatus status = StockCountStatus.DRAFT;

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
    @Column(name = "reconciled_by_account_id")
    private Long reconciledByAccountId;

    @Column(name = "reconciled_at")
    private LocalDateTime reconciledAt;

    @Size(max = 300)
    @Column(name = "remarks", length = 300)
    private String remarks;

    @Valid
    @NotEmpty
    @OneToMany(
            mappedBy = "stockCount",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<StockCountItem> items = new ArrayList<>();

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @NotNull
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @NotNull
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected StockCount() {
    }

    public StockCount(
            StockLocation stockLocation,
            LocalDate countDate,
            Long createdByAccountId,
            String remarks
    ) {
        validateLocation(stockLocation);
        validateCountDate(countDate);
        validateAccountId(createdByAccountId);
        validateRemarks(remarks);

        this.stockLocation = stockLocation;
        this.countDate = countDate;
        this.createdByAccountId = createdByAccountId;
        this.remarks = normalizeText(remarks);
        this.status = StockCountStatus.DRAFT;
    }

    public void addItem(
            InventoryItem inventoryItem,
            java.math.BigDecimal systemQuantity,
            Long systemBalanceVersion,
            java.math.BigDecimal physicalQuantity,
            String itemRemarks
    ) {
        validateDraft();
        if (inventoryItem == null
                || inventoryItem.getId() == null
                || !inventoryItem.isActive()) {
            throw new IllegalArgumentException(
                    "A saved, active inventory item is required"
            );
        }
        if (items.stream().anyMatch(existing ->
                existing.getInventoryItem().getId()
                        .equals(inventoryItem.getId()))) {
            throw new IllegalArgumentException(
                    "Duplicate inventory item in stock count"
            );
        }
        items.add(new StockCountItem(
                this,
                inventoryItem,
                systemQuantity,
                systemBalanceVersion,
                physicalQuantity,
                itemRemarks
        ));
    }

    public void updateDraft(String newRemarks) {
        validateDraft();
        validateRemarks(newRemarks);
        remarks = normalizeText(newRemarks);
    }

    public void updateItem(
            Long inventoryItemId,
            java.math.BigDecimal physicalQuantity,
            String itemRemarks
    ) {
        validateDraft();
        StockCountItem item = items.stream()
                .filter(existing -> existing.getInventoryItem().getId()
                        .equals(inventoryItemId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Inventory item is not part of this stock count"
                ));
        item.updatePhysicalQuantity(physicalQuantity, itemRemarks);
    }

    public void submit(Long accountId) {
        validateDraft();
        validateAccountId(accountId);
        if (!createdByAccountId.equals(accountId)) {
            throw new IllegalStateException(
                    "Only the stock count creator can submit it"
            );
        }
        if (items.isEmpty()
                || items.stream().anyMatch(item ->
                item.getPhysicalQuantity() == null)) {
            throw new IllegalStateException(
                    "Every stock count item must have a physical quantity"
            );
        }
        submittedByAccountId = accountId;
        submittedAt = LocalDateTime.now();
        status = StockCountStatus.SUBMITTED;
    }

    public void approve(Long accountId) {
        validateSubmittedForReview(accountId);
        reviewedByAccountId = accountId;
        reviewedAt = LocalDateTime.now();
        rejectionReason = null;
        status = StockCountStatus.APPROVED;
    }

    public void reject(Long accountId, String reason) {
        validateSubmittedForReview(accountId);
        if (reason == null || reason.isBlank()
                || reason.trim().length() > 500) {
            throw new IllegalArgumentException(
                    "A rejection reason of at most 500 characters is required"
            );
        }
        reviewedByAccountId = accountId;
        reviewedAt = LocalDateTime.now();
        rejectionReason = reason.trim();
        status = StockCountStatus.REJECTED;
    }

    public void reconcile(Long accountId) {
        if (status != StockCountStatus.APPROVED) {
            throw new IllegalStateException(
                    "Only APPROVED stock counts can be reconciled"
            );
        }
        validateAccountId(accountId);
        if (createdByAccountId.equals(accountId)
                || accountId.equals(reviewedByAccountId)) {
            throw new IllegalStateException(
                    "Stock count creator and reviewer cannot reconcile it"
            );
        }
        if (items.isEmpty()
                || items.stream().anyMatch(item ->
                item.getPhysicalQuantity() == null)) {
            throw new IllegalStateException(
                    "Every stock count item must have a physical quantity"
            );
        }
        reconciledByAccountId = accountId;
        reconciledAt = LocalDateTime.now();
        status = StockCountStatus.RECONCILED;
    }

    private void validateSubmittedForReview(Long accountId) {
        if (status != StockCountStatus.SUBMITTED) {
            throw new IllegalStateException(
                    "Only SUBMITTED stock counts can be reviewed"
            );
        }
        validateAccountId(accountId);
        if (createdByAccountId.equals(accountId)) {
            throw new IllegalStateException(
                    "Stock count creator cannot review their own count"
            );
        }
        if (items.isEmpty()
                || items.stream().anyMatch(item ->
                item.getPhysicalQuantity() == null)) {
            throw new IllegalStateException(
                    "Every stock count item must have a physical quantity"
            );
        }
    }

    private void validateDraft() {
        if (status != StockCountStatus.DRAFT) {
            throw new IllegalStateException(
                    "Only DRAFT stock counts can be modified"
            );
        }
    }

    private void validateLocation(StockLocation location) {
        if (location == null
                || !ALLOWED_LOCATIONS.contains(location.getLocationCode())
                || !location.isActive()) {
            throw new IllegalArgumentException(
                    "An active Main or branch stock location is required"
            );
        }
    }

    private void validateCountDate(LocalDate value) {
        if (value == null || value.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "Count date is required and cannot be in the future"
            );
        }
    }

    private void validateRemarks(String value) {
        if (value != null && value.trim().length() > 300) {
            throw new IllegalArgumentException(
                    "Remarks must not exceed 300 characters"
            );
        }
    }

    private void validateAccountId(Long value) {
        if (value == null || value <= 0) {
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
        validateCountDate(countDate);
        validateAccountId(createdByAccountId);
        validateRemarks(remarks);
        if (items.isEmpty() || status != StockCountStatus.DRAFT) {
            throw new IllegalStateException(
                    "New stock counts must start as DRAFT with at least one item"
            );
        }
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        validateLocation(stockLocation);
        validateCountDate(countDate);
        validateRemarks(remarks);
        if (items.isEmpty()) {
            throw new IllegalStateException(
                    "Stock count must contain at least one item"
            );
        }
        if (status == StockCountStatus.SUBMITTED
                && (submittedByAccountId == null || submittedAt == null)) {
            throw new IllegalStateException(
                    "Submitted stock counts require submission audit details"
            );
        }
        if ((status == StockCountStatus.APPROVED
                || status == StockCountStatus.REJECTED
                || status == StockCountStatus.RECONCILED)
                && (reviewedByAccountId == null || reviewedAt == null)) {
            throw new IllegalStateException(
                    "Reviewed stock counts require reviewer audit details"
            );
        }
        if (status == StockCountStatus.REJECTED
                && (rejectionReason == null || rejectionReason.isBlank())) {
            throw new IllegalStateException(
                    "Rejected stock counts require a rejection reason"
            );
        }
        if (status == StockCountStatus.RECONCILED
                && (reconciledByAccountId == null || reconciledAt == null)) {
            throw new IllegalStateException(
                    "Reconciled stock counts require reconciliation audit details"
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

    public LocalDate getCountDate() {
        return countDate;
    }

    public StockCountStatus getStatus() {
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

    public Long getReconciledByAccountId() {
        return reconciledByAccountId;
    }

    public LocalDateTime getReconciledAt() {
        return reconciledAt;
    }

    public String getRemarks() {
        return remarks;
    }

    public List<StockCountItem> getItems() {
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
