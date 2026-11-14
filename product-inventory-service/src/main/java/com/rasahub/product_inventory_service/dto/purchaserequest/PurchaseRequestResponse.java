
package com.rasahub.product_inventory_service.dto.purchaserequest;

import com.rasahub.product_inventory_service.entity.PurchaseRequest;
import com.rasahub.product_inventory_service.enums.PurchaseRequestStatus;

import java.time.LocalDateTime;
import java.util.List;

public class PurchaseRequestResponse {

    private final Long id;
    private final String destinationLocationCode;
    private final String destinationLocationName;
    private final Long requestedByAccountId;
    private final PurchaseRequestStatus status;
    private final Long reviewedByAccountId;
    private final LocalDateTime reviewedAt;
    private final String remarks;
    private final String rejectionReason;
    private final List<PurchaseRequestItemResponse> items;
    private final Long version;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public PurchaseRequestResponse(PurchaseRequest request) {

        this.id = request.getId();

        this.destinationLocationCode =
                request.getDestinationLocation().getLocationCode();

        this.destinationLocationName =
                request.getDestinationLocation().getLocationName();

        this.requestedByAccountId =
                request.getRequestedByAccountId();

        this.status = request.getStatus();

        this.reviewedByAccountId =
                request.getReviewedByAccountId();

        this.reviewedAt = request.getReviewedAt();

        this.remarks = request.getRemarks();

        this.rejectionReason = request.getRejectionReason();

        this.items = request.getItems()
                .stream()
                .map(PurchaseRequestItemResponse::new)
                .toList();

        this.version = request.getVersion();
        this.createdAt = request.getCreatedAt();
        this.updatedAt = request.getUpdatedAt();
    }

    public Long getId() {
        return id;
    }

    public String getDestinationLocationCode() {
        return destinationLocationCode;
    }

    public String getDestinationLocationName() {
        return destinationLocationName;
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

    public List<PurchaseRequestItemResponse> getItems() {
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
