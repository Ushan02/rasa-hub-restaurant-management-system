package com.rasahub.product_inventory_service.dto.dailyrawmaterialusage;

import com.rasahub.product_inventory_service.entity.DailyRawMaterialUsage;
import com.rasahub.product_inventory_service.entity.StockLocation;
import com.rasahub.product_inventory_service.enums.DailyRawMaterialUsageStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class DailyRawMaterialUsageResponse {

    private final Long id;
    private final String branchLocationCode;
    private final String branchLocationName;
    private final LocalDate usageDate;
    private final DailyRawMaterialUsageStatus status;
    private final Long createdByAccountId;
    private final Long postedByAccountId;
    private final LocalDateTime postedAt;
    private final String remarks;
    private final List<DailyRawMaterialUsageItemResponse> items;
    private final Long version;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public DailyRawMaterialUsageResponse(
            DailyRawMaterialUsage usage
    ) {
        StockLocation branchLocation = usage.getBranchLocation();

        this.id = usage.getId();
        this.branchLocationCode = branchLocation.getLocationCode();
        this.branchLocationName = branchLocation.getLocationName();
        this.usageDate = usage.getUsageDate();
        this.status = usage.getStatus();
        this.createdByAccountId = usage.getCreatedByAccountId();
        this.postedByAccountId = usage.getPostedByAccountId();
        this.postedAt = usage.getPostedAt();
        this.remarks = usage.getRemarks();
        this.items = usage.getItems().stream()
                .map(DailyRawMaterialUsageItemResponse::new)
                .toList();
        this.version = usage.getVersion();
        this.createdAt = usage.getCreatedAt();
        this.updatedAt = usage.getUpdatedAt();
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

    public LocalDate getUsageDate() {
        return usageDate;
    }

    public DailyRawMaterialUsageStatus getStatus() {
        return status;
    }

    public Long getCreatedByAccountId() {
        return createdByAccountId;
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

    public List<DailyRawMaterialUsageItemResponse> getItems() {
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
