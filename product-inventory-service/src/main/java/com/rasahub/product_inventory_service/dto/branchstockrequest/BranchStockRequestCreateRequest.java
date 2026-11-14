
package com.rasahub.product_inventory_service.dto.branchstockrequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public class BranchStockRequestCreateRequest {

    @NotBlank(message = "Branch location code is required")
    @Pattern(
            regexp = "^(BR001|BR002|BR003)$",
            message = "Branch location must be BR001, BR002 or BR003"
    )
    private String branchLocationCode;

    @Size(
            max = 300,
            message = "Remarks must not exceed 300 characters"
    )
    private String remarks;

    @Valid
    @NotEmpty(
            message = "Branch stock request must contain at least one item"
    )
    private List<
            @NotNull(message = "Request item cannot be null")
                    BranchStockRequestItemRequest
            > items;

    public BranchStockRequestCreateRequest() {
    }

    public String getBranchLocationCode() {
        return branchLocationCode;
    }

    public void setBranchLocationCode(String branchLocationCode) {
        this.branchLocationCode = branchLocationCode;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public List<BranchStockRequestItemRequest> getItems() {
        return items;
    }

    public void setItems(
            List<BranchStockRequestItemRequest> items
    ) {
        this.items = items;
    }
}
