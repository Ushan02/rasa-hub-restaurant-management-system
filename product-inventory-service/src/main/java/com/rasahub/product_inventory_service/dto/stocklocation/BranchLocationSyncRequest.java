
package com.rasahub.product_inventory_service.dto.stocklocation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class BranchLocationSyncRequest {

    @NotBlank(message = "Branch code is required")
    @Pattern(
            regexp = "^(MO001|BR[0-9]{3})$",
            message = "Invalid branch code"
    )
    private String branchCode;

    @NotBlank(message = "Branch name is required")
    @Size(max = 100)
    private String name;

    @NotBlank(message = "Branch type is required")
    @Pattern(
            regexp = "^(MAIN_OFFICE|RESTAURANT_BRANCH)$",
            message = "Invalid branch type"
    )
    private String type;

    @NotNull(message = "Branch status is required")
    private Boolean active;

    public BranchLocationSyncRequest() {
    }

    public String getBranchCode() {
        return branchCode;
    }

    public void setBranchCode(String branchCode) {
        this.branchCode = branchCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
