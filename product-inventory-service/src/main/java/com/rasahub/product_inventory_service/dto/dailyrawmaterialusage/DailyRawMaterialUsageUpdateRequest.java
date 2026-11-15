package com.rasahub.product_inventory_service.dto.dailyrawmaterialusage;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public class DailyRawMaterialUsageUpdateRequest {

    @Size(max = 300, message = "Remarks must not exceed 300 characters")
    private String remarks;

    @Valid
    @NotEmpty(message = "Daily usage must contain at least one item")
    private List<DailyRawMaterialUsageItemRequest> items;

    public DailyRawMaterialUsageUpdateRequest() {
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public List<DailyRawMaterialUsageItemRequest> getItems() {
        return items;
    }

    public void setItems(List<DailyRawMaterialUsageItemRequest> items) {
        this.items = items;
    }
}
