package com.rasahub.product_inventory_service.dto.stockcount;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public class StockCountUpdateRequest {

    @Size(max = 300, message = "Remarks must not exceed 300 characters")
    private String remarks;

    @NotEmpty(message = "Stock count must contain its selected items")
    private List<@Valid StockCountItemRequest> items;

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
