
package com.rasahub.product_inventory_service.dto.stocktransfer;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public class StockTransferCreateRequest {

    @NotNull(message = "Branch stock request ID is required")
    @Positive(message = "Branch stock request ID must be positive")
    private Long branchStockRequestId;

    @Size(max = 300, message = "Remarks must not exceed 300 characters")
    private String remarks;

    @Valid
    @NotEmpty(message = "At least one transfer item is required")
    private List<
            @NotNull(message = "Transfer item cannot be null")
                    StockTransferItemRequest
            > items;

    public StockTransferCreateRequest() {
    }

    public Long getBranchStockRequestId() {
        return branchStockRequestId;
    }

    public void setBranchStockRequestId(Long branchStockRequestId) {
        this.branchStockRequestId = branchStockRequestId;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public List<StockTransferItemRequest> getItems() {
        return items;
    }

    public void setItems(List<StockTransferItemRequest> items) {
        this.items = items;
    }
}
