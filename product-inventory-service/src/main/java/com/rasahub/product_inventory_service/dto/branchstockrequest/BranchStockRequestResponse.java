
package com.rasahub.product_inventory_service.dto.branchstockrequest;

import com.rasahub.product_inventory_service.entity.BranchStockRequest;
import com.rasahub.product_inventory_service.entity.StockLocation;

import com.rasahub.product_inventory_service.enums.BranchStockRequestStatus;

import java.time.LocalDateTime;
import java.util.List;

public class BranchStockRequestResponse {

    private final Long id;

    private final String branchLocationCode;
    private final String branchLocationName;

    private final Long requestedByAccountId;

    private final BranchStockRequestStatus status;

    private final Long reviewedByAccountId;
    private final LocalDateTime reviewedAt;

    private final String remarks;
    private final String rejectionReason;

    private final List<BranchStockRequestItemResponse> items;

    private final Long version;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public BranchStockRequestResponse(
            BranchStockRequest request
    ) {
        StockLocation branchLocation =
                request.getBranchLocation();

        this.id = request.getId();

        this.branchLocationCode =
                branchLocation.getLocationCode();

        this.branchLocationName =
                branchLocation.getLocationName();

        this.requestedByAccountId =
                request.getRequestedByAccountId();

        this.status =
                request.getStatus();

        this.reviewedByAccountId =
                request.getReviewedByAccountId();

        this.reviewedAt =
                request.getReviewedAt();

        this.remarks =
                request.getRemarks();

        this.rejectionReason =
                request.getRejectionReason();

        this.items = request.getItems()
                .stream()
                .map(BranchStockRequestItemResponse::new)
                .toList();

        this.version =
                request.getVersion();

        this.createdAt =
                request.getCreatedAt();

        this.updatedAt =
                request.getUpdatedAt();
    }

    public Long getId() {
        return id;
    }

    public String getBranchLocationCode() {
        return branchLocationCode;
    }

    public String getBranchLocationName() {
        return branchLocationName;
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

    public List<BranchStockRequestItemResponse> getItems() {
        return items;
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
