package com.rasahub.product_inventory_service.dto.stockadjustment;

import com.rasahub.product_inventory_service.entity.StockAdjustment;
import com.rasahub.product_inventory_service.entity.StockLocation;
import com.rasahub.product_inventory_service.enums.StockAdjustmentDirection;
import com.rasahub.product_inventory_service.enums.StockAdjustmentReason;
import com.rasahub.product_inventory_service.enums.StockAdjustmentStatus;

import java.time.LocalDateTime;
import java.util.List;

public class StockAdjustmentResponse {

    private final Long id;
    private final String locationCode;
    private final String locationName;
    private final StockAdjustmentReason adjustmentReason;
    private final StockAdjustmentDirection direction;
    private final StockAdjustmentStatus status;
    private final Long createdByAccountId;
    private final Long submittedByAccountId;
    private final LocalDateTime submittedAt;
    private final Long reviewedByAccountId;
    private final LocalDateTime reviewedAt;
    private final String rejectionReason;
    private final Long postedByAccountId;
    private final LocalDateTime postedAt;
    private final String remarks;
    private final List<StockAdjustmentItemResponse> items;
    private final Long version;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public StockAdjustmentResponse(StockAdjustment adjustment) {
        StockLocation location = adjustment.getStockLocation();
        id = adjustment.getId();
        locationCode = location.getLocationCode();
        locationName = location.getLocationName();
        adjustmentReason = adjustment.getAdjustmentReason();
        direction = adjustment.getDirection();
        status = adjustment.getStatus();
        createdByAccountId = adjustment.getCreatedByAccountId();
        submittedByAccountId = adjustment.getSubmittedByAccountId();
        submittedAt = adjustment.getSubmittedAt();
        reviewedByAccountId = adjustment.getReviewedByAccountId();
        reviewedAt = adjustment.getReviewedAt();
        rejectionReason = adjustment.getRejectionReason();
        postedByAccountId = adjustment.getPostedByAccountId();
        postedAt = adjustment.getPostedAt();
        remarks = adjustment.getRemarks();
        items = adjustment.getItems().stream()
                .map(StockAdjustmentItemResponse::new)
                .toList();
        version = adjustment.getVersion();
        createdAt = adjustment.getCreatedAt();
        updatedAt = adjustment.getUpdatedAt();
    }

    public Long getId() {
        return id;
    }

    public String getLocationCode() {
        return locationCode;
    }

    public String getLocationName() {
        return locationName;
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

    public List<StockAdjustmentItemResponse> getItems() {
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
