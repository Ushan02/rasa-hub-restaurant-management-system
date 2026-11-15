package com.rasahub.product_inventory_service.dto.dailyrawmaterialusage;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public class DailyRawMaterialUsageCreateRequest {

    @NotNull(message = "Usage date is required")
    @PastOrPresent(message = "Usage date cannot be in the future")
    private LocalDate usageDate;

    @Size(max = 300, message = "Remarks must not exceed 300 characters")
    private String remarks;

    @Valid
    @NotEmpty(message = "Daily usage must contain at least one item")
    private List<DailyRawMaterialUsageItemRequest> items;

    public DailyRawMaterialUsageCreateRequest() {
    }

    public LocalDate getUsageDate() {
        return usageDate;
    }

    public void setUsageDate(LocalDate usageDate) {
        this.usageDate = usageDate;
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
