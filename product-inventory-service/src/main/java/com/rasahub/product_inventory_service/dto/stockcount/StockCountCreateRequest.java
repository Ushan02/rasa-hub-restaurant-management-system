package com.rasahub.product_inventory_service.dto.stockcount;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public class StockCountCreateRequest {

    @NotBlank(message = "Stock location is required")
    @Pattern(
            regexp = "^(Main|BR001|BR002|BR003)$",
            message = "Location must be Main, BR001, BR002 or BR003"
    )
    private String locationCode;

    private LocalDate countDate;

    @Size(max = 300, message = "Remarks must not exceed 300 characters")
    private String remarks;

    @NotEmpty(message = "Stock count must contain at least one item")
    private List<@Valid StockCountItemRequest> items;

    public String getLocationCode() {
        return locationCode;
    }

    public void setLocationCode(String locationCode) {
        this.locationCode = locationCode;
    }

    public LocalDate getCountDate() {
        return countDate;
    }

    public void setCountDate(LocalDate countDate) {
        this.countDate = countDate;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public List<StockCountItemRequest> getItems() {
        return items;
    }

    public void setItems(List<StockCountItemRequest> items) {
        this.items = items;
    }
}
