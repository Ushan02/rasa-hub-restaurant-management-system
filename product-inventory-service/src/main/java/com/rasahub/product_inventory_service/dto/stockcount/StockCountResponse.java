package com.rasahub.product_inventory_service.dto.stockcount;

import com.rasahub.product_inventory_service.entity.StockCount;
import com.rasahub.product_inventory_service.enums.StockCountStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class StockCountResponse {

    private final Long id;
    private final String locationCode;
    private final String locationName;
    private final LocalDate countDate;
    private final StockCountStatus status;
    private final Long createdByAccountId;
    private final Long submittedByAccountId;
    private final LocalDateTime submittedAt;
    private final Long reviewedByAccountId;
    private final LocalDateTime reviewedAt;
    private final String rejectionReason;
    private final Long reconciledByAccountId;
    private final LocalDateTime reconciledAt;
    private final String remarks;
    private final List<StockCountItemResponse> items;
    private final Long version;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public StockCountResponse(StockCount count) {
        id = count.getId();
        locationCode = count.getStockLocation().getLocationCode();
        locationName = count.getStockLocation().getLocationName();
        countDate = count.getCountDate();
        status = count.getStatus();
        createdByAccountId = count.getCreatedByAccountId();
        submittedByAccountId = count.getSubmittedByAccountId();
        submittedAt = count.getSubmittedAt();
        reviewedByAccountId = count.getReviewedByAccountId();
        reviewedAt = count.getReviewedAt();
        rejectionReason = count.getRejectionReason();
        reconciledByAccountId = count.getReconciledByAccountId();
        reconciledAt = count.getReconciledAt();
        remarks = count.getRemarks();
        items = count.getItems().stream()
                .map(StockCountItemResponse::new)
                .toList();
        version = count.getVersion();
        createdAt = count.getCreatedAt();
        updatedAt = count.getUpdatedAt();
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

    public List<StockCountItemResponse> getItems() {
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
