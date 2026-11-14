
package com.rasahub.product_inventory_service.entity;

import com.rasahub.product_inventory_service.enums.BranchStockRequestStatus;
import com.rasahub.product_inventory_service.enums.StockTransferStatus;

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
        name = "stock_transfers",
        indexes = {
                @Index(
                        name = "idx_transfer_request",
                        columnList = "branch_stock_request_id"
                ),
                @Index(
                        name = "idx_transfer_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_transfer_destination",
                        columnList = "destination_location_code"
                ),
                @Index(
                        name = "idx_transfer_created_at",
                        columnList = "created_at"
                )
        }
)
public class StockTransfer {

    private static final Set<String> BRANCH_CODES =
            Set.of("BR001", "BR002", "BR003");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "branch_stock_request_id",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(
                    name = "fk_transfer_branch_request"
            )
    )
    private BranchStockRequest branchStockRequest;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "source_location_code",
            referencedColumnName = "location_code",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(
                    name = "fk_transfer_source"
            )
    )
    private StockLocation sourceLocation;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "destination_location_code",
            referencedColumnName = "location_code",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(
                    name = "fk_transfer_destination"
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
    private StockTransferStatus status =
            StockTransferStatus.DRAFT;

    @Positive
    @Column(name = "dispatched_by_account_id")
    private Long dispatchedByAccountId;

    @Column(name = "dispatched_at")
    private LocalDateTime dispatchedAt;

    @Positive
    @Column(name = "received_by_account_id")
    private Long receivedByAccountId;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @Positive
    @Column(name = "cancelled_by_account_id")
    private Long cancelledByAccountId;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Size(max = 300)
    @Column(
            name = "cancellation_reason",
            length = 300
    )
    private String cancellationReason;

    @Size(max = 300)
    @Column(
            name = "remarks",
            length = 300
    )
    private String remarks;

    @Valid
    @NotEmpty
    @OneToMany(
            mappedBy = "stockTransfer",
            cascade = CascadeType.ALL
    )
    private List<StockTransferItem> items =
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

    protected StockTransfer() {
    }

    public StockTransfer(
            BranchStockRequest branchStockRequest,
            StockLocation sourceLocation,
            Long createdByAccountId,
            String remarks
    ) {
        if (branchStockRequest == null
                || branchStockRequest.getId() == null) {
            throw new IllegalArgumentException(
                    "A saved branch stock request is required"
            );
        }

        validateRequestStatus(branchStockRequest);
        validateSourceLocation(sourceLocation);

        StockLocation destination =
                branchStockRequest.getBranchLocation();

        validateDestinationLocation(destination);

        if (sourceLocation.getLocationCode()
                .equals(destination.getLocationCode())) {
            throw new IllegalArgumentException(
                    "Source and destination cannot be the same"
            );
        }

        validateAccountId(createdByAccountId);
        validateLength(remarks, 300, "Remarks");

        this.branchStockRequest = branchStockRequest;
        this.sourceLocation = sourceLocation;
        this.destinationLocation = destination;
        this.createdByAccountId = createdByAccountId;
        this.remarks = normalizeText(remarks);
        this.status = StockTransferStatus.DRAFT;
    }

    public void addItem(
            InventoryItem inventoryItem,
            BigDecimal transferQuantity
    ) {
        if (id != null
                || status != StockTransferStatus.DRAFT) {
            throw new IllegalStateException(
                    "Items can only be added before transfer creation"
            );
        }

        if (inventoryItem == null
                || inventoryItem.getId() == null
                || !inventoryItem.isActive()) {
            throw new IllegalArgumentException(
                    "A saved, active inventory item is required"
            );
        }

        BranchStockRequestItem requestItem =
                branchStockRequest.getItems()
                        .stream()
                        .filter(item ->
                                item.getInventoryItem()
                                        .getId()
                                        .equals(inventoryItem.getId())
                        )
                        .findFirst()
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Inventory item is not in the branch request"
                                )
                        );

        boolean duplicate = items.stream()
                .anyMatch(item ->
                        item.getInventoryItem()
                                .getId()
                                .equals(inventoryItem.getId())
                );

        if (duplicate) {
            throw new IllegalArgumentException(
                    "Duplicate inventory item in transfer"
            );
        }

        validateQuantity(inventoryItem, transferQuantity);

        if (transferQuantity.compareTo(
                requestItem.getOutstandingQuantity()) > 0) {
            throw new IllegalArgumentException(
                    "Transfer quantity exceeds request outstanding quantity"
            );
        }

        items.add(
                new StockTransferItem(
                        this,
                        inventoryItem,
                        transferQuantity
                )
        );
    }

    public void dispatch(Long staffAccountId) {
        if (status != StockTransferStatus.DRAFT) {
            throw new IllegalStateException(
                    "Only draft transfers can be dispatched"
            );
        }

        if (id == null) {
            throw new IllegalStateException(
                    "Transfer must be saved before dispatch"
            );
        }

        validateAccountId(staffAccountId);
        validateItemsPresent();
        validateRequestStatus(branchStockRequest);

        this.dispatchedByAccountId = staffAccountId;
        this.dispatchedAt = LocalDateTime.now();
        this.status = StockTransferStatus.DISPATCHED;
    }

    public void confirmReceipt(Long receiverAccountId) {
        if (status != StockTransferStatus.DISPATCHED) {
            throw new IllegalStateException(
                    "Only dispatched transfers can be received"
            );
        }

        validateAccountId(receiverAccountId);
        validateItemsPresent();

        this.receivedByAccountId = receiverAccountId;
        this.receivedAt = LocalDateTime.now();
        this.status = StockTransferStatus.RECEIVED;
    }

    public void cancel(
            Long staffAccountId,
            String reason
    ) {
        if (status != StockTransferStatus.DRAFT) {
            throw new IllegalStateException(
                    "Only draft transfers can be cancelled"
            );
        }

        validateAccountId(staffAccountId);

        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException(
                    "Cancellation reason is required"
            );
        }

        validateLength(
                reason,
                300,
                "Cancellation reason"
        );

        this.cancelledByAccountId = staffAccountId;
        this.cancelledAt = LocalDateTime.now();
        this.cancellationReason = reason.trim();
        this.status = StockTransferStatus.CANCELLED;
    }

    private void validateRequestStatus(
            BranchStockRequest request
    ) {
        if (request.getStatus() != BranchStockRequestStatus.APPROVED
                && request.getStatus()
                != BranchStockRequestStatus.PARTIALLY_FULFILLED) {
            throw new IllegalStateException(
                    "Branch stock request is not eligible for transfer"
            );
        }
    }

    private void validateSourceLocation(
            StockLocation location
    ) {
        if (location == null
                || !"Main".equals(location.getLocationCode())
                || !location.isActive()) {
            throw new IllegalArgumentException(
                    "Transfer source must be active Main warehouse"
            );
        }
    }

    private void validateDestinationLocation(
            StockLocation location
    ) {
        if (location == null
                || !BRANCH_CODES.contains(location.getLocationCode())
                || !location.isActive()) {
            throw new IllegalArgumentException(
                    "Transfer destination must be an active branch"
            );
        }
    }

    private void validateQuantity(
            InventoryItem inventoryItem,
            BigDecimal quantity
    ) {
        if (quantity == null
                || quantity.signum() <= 0
                || quantity.scale() > 3
                || quantity.precision() - quantity.scale() > 15) {
            throw new IllegalArgumentException(
                    "Invalid transfer quantity"
            );
        }

        if (inventoryItem.getMeasurementUnit()
                == com.rasahub.product_inventory_service
                .enums.MeasurementUnit.PCS
                && quantity.stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException(
                    "PCS transfer quantity must be a whole number"
            );
        }
    }

    private void validateAccountId(Long accountId) {
        if (accountId == null || accountId <= 0) {
            throw new IllegalArgumentException(
                    "Valid staff account ID is required"
            );
        }
    }

    private void validateItemsPresent() {
        if (items == null || items.isEmpty()) {
            throw new IllegalStateException(
                    "Stock transfer must contain at least one item"
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
        validateRequestStatus(branchStockRequest);
        validateSourceLocation(sourceLocation);
        validateDestinationLocation(destinationLocation);
        validateItemsPresent();

        if (!destinationLocation.getLocationCode().equals(
                branchStockRequest.getBranchLocation()
                        .getLocationCode())) {
            throw new IllegalStateException(
                    "Transfer destination must match branch request"
            );
        }

        if (status != StockTransferStatus.DRAFT) {
            throw new IllegalStateException(
                    "New transfers must start as DRAFT"
            );
        }

        validateAccountId(createdByAccountId);

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

    public BranchStockRequest getBranchStockRequest() {
        return branchStockRequest;
    }

    public StockLocation getSourceLocation() {
        return sourceLocation;
    }

    public StockLocation getDestinationLocation() {
        return destinationLocation;
    }

    public Long getCreatedByAccountId() {
        return createdByAccountId;
    }

    public StockTransferStatus getStatus() {
        return status;
    }

    public Long getDispatchedByAccountId() {
        return dispatchedByAccountId;
    }

    public LocalDateTime getDispatchedAt() {
        return dispatchedAt;
    }

    public Long getReceivedByAccountId() {
        return receivedByAccountId;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public Long getCancelledByAccountId() {
        return cancelledByAccountId;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public String getRemarks() {
        return remarks;
    }

    public List<StockTransferItem> getItems() {
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
