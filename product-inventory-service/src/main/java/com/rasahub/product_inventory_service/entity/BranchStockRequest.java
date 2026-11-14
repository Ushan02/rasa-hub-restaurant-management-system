
package com.rasahub.product_inventory_service.entity;

import com.rasahub.product_inventory_service.enums.BranchStockRequestStatus;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Entity
@Table(
        name = "branch_stock_requests",
        indexes = {
                @Index(
                        name = "idx_bsr_branch",
                        columnList = "branch_location_code"
                ),
                @Index(
                        name = "idx_bsr_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_bsr_created_at",
                        columnList = "created_at"
                )
        }
)
public class BranchStockRequest {

    private static final Set<String> ALLOWED_BRANCH_CODES =
            Set.of("BR001", "BR002", "BR003");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "branch_location_code",
            referencedColumnName = "location_code",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(
                    name = "fk_bsr_branch_location"
            )
    )
    private StockLocation branchLocation;

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
            length = 25
    )
    private BranchStockRequestStatus status =
            BranchStockRequestStatus.PENDING;

    @Positive
    @Column(name = "reviewed_by_account_id")
    private Long reviewedByAccountId;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Size(max = 300)
    @Column(
            name = "remarks",
            length = 300
    )
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
            mappedBy = "branchStockRequest",
            cascade = CascadeType.ALL
    )
    private List<BranchStockRequestItem> items =
            new ArrayList<>();

    @Version
    @Column(
            name = "version",
            nullable = false
    )
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

    protected BranchStockRequest() {
    }

    public BranchStockRequest(
            StockLocation branchLocation,
            Long requestedByAccountId,
            String remarks
    ) {
        validateBranch(branchLocation);

        if (requestedByAccountId == null
                || requestedByAccountId <= 0) {
            throw new IllegalArgumentException(
                    "Valid requester account ID is required"
            );
        }

        validateLength(remarks, 300, "Remarks");

        this.branchLocation = branchLocation;
        this.requestedByAccountId = requestedByAccountId;
        this.remarks = normalizeText(remarks);
        this.status = BranchStockRequestStatus.PENDING;
    }

    public void addItem(
            InventoryItem inventoryItem,
            BigDecimal requestedQuantity
    ) {
        if (id != null
                || status != BranchStockRequestStatus.PENDING) {
            throw new IllegalStateException(
                    "Items can only be added before request creation"
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
                    "Duplicate inventory item in branch stock request"
            );
        }

        items.add(
                new BranchStockRequestItem(
                        this,
                        inventoryItem,
                        requestedQuantity
                )
        );
    }

    public void approve(Long managerAccountId) {
        validatePendingReview(managerAccountId);

        this.status = BranchStockRequestStatus.APPROVED;
        this.reviewedByAccountId = managerAccountId;
        this.reviewedAt = LocalDateTime.now();
        this.rejectionReason = null;
    }

    public void reject(
            Long managerAccountId,
            String reason
    ) {
        validatePendingReview(managerAccountId);

        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException(
                    "Rejection reason is required"
            );
        }

        validateLength(
                reason,
                500,
                "Rejection reason"
        );

        this.status = BranchStockRequestStatus.REJECTED;
        this.reviewedByAccountId = managerAccountId;
        this.reviewedAt = LocalDateTime.now();
        this.rejectionReason = reason.trim();
    }

    public void recordReceivedQuantity(
            Long inventoryItemId,
            BigDecimal quantity
    ) {
        if (status != BranchStockRequestStatus.APPROVED
                && status !=
                BranchStockRequestStatus.PARTIALLY_FULFILLED) {
            throw new IllegalStateException(
                    "Request is not eligible for stock receiving"
            );
        }

        if (inventoryItemId == null
                || inventoryItemId <= 0) {
            throw new IllegalArgumentException(
                    "Valid inventory item ID is required"
            );
        }

        BranchStockRequestItem requestItem =
                items.stream()
                        .filter(item ->
                                item.getInventoryItem()
                                        .getId()
                                        .equals(inventoryItemId)
                        )
                        .findFirst()
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Inventory item is not in the request"
                                )
                        );

        requestItem.recordReceivedQuantity(quantity);

        boolean fullyFulfilled = items.stream()
                .allMatch(
                        BranchStockRequestItem::isFullyFulfilled
                );

        this.status = fullyFulfilled
                ? BranchStockRequestStatus.FULFILLED
                : BranchStockRequestStatus.PARTIALLY_FULFILLED;
    }

    private void validatePendingReview(
            Long managerAccountId
    ) {
        if (status != BranchStockRequestStatus.PENDING) {
            throw new IllegalStateException(
                    "Only pending branch stock requests can be reviewed"
            );
        }

        if (managerAccountId == null
                || managerAccountId <= 0) {
            throw new IllegalArgumentException(
                    "Valid manager account ID is required"
            );
        }

        if (managerAccountId.equals(requestedByAccountId)) {
            throw new IllegalStateException(
                    "Requester cannot review their own request"
            );
        }

        validateItemsPresent();
    }

    private void validateBranch(
            StockLocation branch
    ) {
        if (branch == null
                || !ALLOWED_BRANCH_CODES.contains(
                branch.getLocationCode()
        )
                || !branch.isActive()) {
            throw new IllegalArgumentException(
                    "An active, valid branch location is required"
            );
        }
    }

    private void validateItemsPresent() {
        if (items == null || items.isEmpty()) {
            throw new IllegalStateException(
                    "Branch stock request must contain at least one item"
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
        validateBranch(branchLocation);
        validateItemsPresent();

        if (status != BranchStockRequestStatus.PENDING) {
            throw new IllegalStateException(
                    "New branch stock requests must start as PENDING"
            );
        }

        if (requestedByAccountId == null
                || requestedByAccountId <= 0) {
            throw new IllegalStateException(
                    "Valid requester account ID is required"
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

    public StockLocation getBranchLocation() {
        return branchLocation;
    }

    public Long getRequestedByAccountId() {
        return requestedByAccountId;
    }

    public BranchStockRequestStatus getStatus() {
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

    public List<BranchStockRequestItem> getItems() {
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
