
package com.rasahub.product_inventory_service.entity;

import com.rasahub.product_inventory_service.enums.DailyRawMaterialUsageStatus;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Entity
@Table(
        name = "daily_raw_material_usages",
        indexes = {
                @Index(
                        name = "idx_daily_usage_branch",
                        columnList = "branch_location_code"
                ),
                @Index(
                        name = "idx_daily_usage_date",
                        columnList = "usage_date"
                ),
                @Index(
                        name = "idx_daily_usage_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_daily_usage_created_at",
                        columnList = "created_at"
                )
        }
)
public class DailyRawMaterialUsage {

    private static final Set<String> BRANCH_CODES =
            Set.of("BR001", "BR002", "BR003");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "branch_location_code",
            referencedColumnName = "location_code",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(
                    name = "fk_daily_usage_branch"
            )
    )
    private StockLocation branchLocation;

    @NotNull
    @PastOrPresent
    @Column(
            name = "usage_date",
            nullable = false,
            updatable = false
    )
    private LocalDate usageDate;

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
    private DailyRawMaterialUsageStatus status =
            DailyRawMaterialUsageStatus.DRAFT;

    @Positive
    @Column(name = "posted_by_account_id")
    private Long postedByAccountId;

    @Column(name = "posted_at")
    private LocalDateTime postedAt;

    @Size(max = 300)
    @Column(
            name = "remarks",
            length = 300
    )
    private String remarks;

    @Valid
    @NotEmpty
    @OneToMany(
            mappedBy = "dailyRawMaterialUsage",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<DailyRawMaterialUsageItem> items =
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

    protected DailyRawMaterialUsage() {
    }

    public DailyRawMaterialUsage(
            StockLocation branchLocation,
            LocalDate usageDate,
            Long createdByAccountId,
            String remarks
    ) {
        validateBranch(branchLocation);
        validateUsageDate(usageDate);
        validateAccountId(createdByAccountId);

        if (remarks != null
                && remarks.trim().length() > 300) {
            throw new IllegalArgumentException(
                    "Remarks must not exceed 300 characters"
            );
        }

        this.branchLocation = branchLocation;
        this.usageDate = usageDate;
        this.createdByAccountId = createdByAccountId;
        this.remarks = normalizeText(remarks);
        this.status = DailyRawMaterialUsageStatus.DRAFT;
    }

    public void addItem(
            InventoryItem inventoryItem,
            BigDecimal usedQuantity
    ) {
        validateDraftStatus();

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
                    "Duplicate inventory item in daily usage"
            );
        }

        items.add(
                new DailyRawMaterialUsageItem(
                        this,
                        inventoryItem,
                        usedQuantity
                )
        );
    }

    public void updateDraftDetails(String remarks) {
        validateDraftStatus();
        this.remarks = normalizeText(remarks);
    }

    public void replaceItems(List<DailyRawMaterialUsageItem> newItems) {
        validateDraftStatus();

        if (newItems == null || newItems.isEmpty()) {
            throw new IllegalArgumentException(
                    "Daily usage must contain at least one item"
            );
        }

        Map<Long, DailyRawMaterialUsageItem> existingItemsByInventoryId =
                new HashMap<>();
        for (DailyRawMaterialUsageItem existingItem : items) {
            existingItemsByInventoryId.put(
                    existingItem.getInventoryItem().getId(),
                    existingItem
            );
        }

        List<DailyRawMaterialUsageItem> reconciledItems =
                new ArrayList<>(newItems.size());
        Set<Long> itemIds = new HashSet<>();

        for (DailyRawMaterialUsageItem item : newItems) {
            if (item == null) {
                throw new IllegalArgumentException(
                        "Daily usage item cannot be null"
                );
            }

            if (item.getInventoryItem() == null
                    || item.getInventoryItem().getId() == null) {
                throw new IllegalArgumentException(
                        "Valid inventory item is required"
                );
            }

            Long itemId = item.getInventoryItem().getId();

            if (!itemIds.add(itemId)) {
                throw new IllegalArgumentException(
                        "Duplicate inventory item in daily usage"
                );
            }

            DailyRawMaterialUsageItem existingItem =
                    existingItemsByInventoryId.get(itemId);
            if (existingItem != null) {
                existingItem.changeUsedQuantity(item.getUsedQuantity());
                reconciledItems.add(existingItem);
            } else {
                item.attachToUsage(this);
                reconciledItems.add(item);
            }
        }

        this.items.clear();
        this.items.addAll(reconciledItems);
    }

    public void post(Long accountId) {
        validateDraftStatus();

        if (id == null) {
            throw new IllegalStateException(
                    "Usage record must be saved before posting"
            );
        }

        validateAccountId(accountId);
        validateBranch(branchLocation);
        validateUsageDate(usageDate);
        validateItemsPresent();

        this.postedByAccountId = accountId;
        this.postedAt = LocalDateTime.now();
        this.status = DailyRawMaterialUsageStatus.POSTED;
    }

    private void validateDraftStatus() {
        if (status != DailyRawMaterialUsageStatus.DRAFT) {
            throw new IllegalStateException(
                    "Only DRAFT usage records can be modified"
            );
        }
    }

    private void validateBranch(StockLocation branch) {
        if (branch == null
                || !BRANCH_CODES.contains(
                branch.getLocationCode()
        )
                || !branch.isActive()) {
            throw new IllegalArgumentException(
                    "An active, valid branch is required"
            );
        }
    }

    private void validateUsageDate(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException(
                    "Usage date is required"
            );
        }

        if (date.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "Usage date cannot be in the future"
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

    private void validateItemsPresent() {
        if (items == null || items.isEmpty()) {
            throw new IllegalStateException(
                    "Daily usage must contain at least one item"
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
        validateUsageDate(usageDate);
        validateAccountId(createdByAccountId);
        validateItemsPresent();

        if (status != DailyRawMaterialUsageStatus.DRAFT) {
            throw new IllegalStateException(
                    "New usage records must start as DRAFT"
            );
        }

        LocalDateTime now = LocalDateTime.now();

        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        validateItemsPresent();

        if (status == DailyRawMaterialUsageStatus.POSTED
                && (postedByAccountId == null
                || postedAt == null)) {
            throw new IllegalStateException(
                    "Posted usage requires posting audit details"
            );
        }

        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public StockLocation getBranchLocation() {
        return branchLocation;
    }

    public LocalDate getUsageDate() {
        return usageDate;
    }

    public Long getCreatedByAccountId() {
        return createdByAccountId;
    }

    public DailyRawMaterialUsageStatus getStatus() {
        return status;
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

    public List<DailyRawMaterialUsageItem> getItems() {
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
