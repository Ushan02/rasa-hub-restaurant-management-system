package com.rasahub.auth_people_service.dto.branch;

import com.rasahub.auth_people_service.enums.BranchType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class BranchRequest {

    @NotBlank(message = "Branch code is required")
    @Size(max = 20, message = "Branch code must be under 20 characters")
    @Pattern(
            regexp = "^[A-Za-z0-9_-]+$",
            message = "Branch code can contain only letters, numbers, hyphens and underscores"
    )
    private String branchCode;

    @NotBlank(message = "Branch name is required")
    @Size(max = 100, message = "Branch name must be under 100 characters")
    private String name;

    @NotNull(message = "Branch type is required")
    private BranchType type;

    public BranchRequest() {
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

    public BranchType getType() {
        return type;
    }

    public void setType(BranchType type) {
        this.type = type;
    }
}