package com.rasahub.product_inventory_service.dto.stockadjustment;

import com.rasahub.product_inventory_service.enums.StockAdjustmentDirection;
import com.rasahub.product_inventory_service.enums.StockAdjustmentReason;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public class StockAdjustmentCreateRequest {

    @NotBlank(message = "Stock location is required")
    @Pattern(
            regexp = "^(Main|BR001|BR002|BR003)$",
            message = "Location must be Main, BR001, BR002 or BR003"
    )
    private String locationCode;

    @NotNull(message = "Adjustment reason is required")
    private StockAdjustmentReason adjustmentReason;

    @NotNull(message = "Adjustment direction is required")
    private StockAdjustmentDirection direction;

    @Size(max = 300, message = "Remarks must not exceed 300 characters")
    private String remarks;

    @NotEmpty(message = "Stock adjustment must contain at least one item")
    private List<@Valid StockAdjustmentItemRequest> items;

    public String getLocationCode() {
        return locationCode;
    }

    public void setLocationCode(String locationCode) {
        this.locationCode = locationCode;
    }

    public StockAdjustmentReason getAdjustmentReason() {
        return adjustmentReason;
    }

    public void setAdjustmentReason(StockAdjustmentReason adjustmentReason) {
        this.adjustmentReason = adjustmentReason;
    }

    public StockAdjustmentDirection getDirection() {
        return direction;
    }

    public void setDirection(StockAdjustmentDirection direction) {
        this.direction = direction;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public List<StockAdjustmentItemRequest> getItems() {
        return items;
    }

    public void setItems(List<StockAdjustmentItemRequest> items) {
        this.items = items;
    }
}
