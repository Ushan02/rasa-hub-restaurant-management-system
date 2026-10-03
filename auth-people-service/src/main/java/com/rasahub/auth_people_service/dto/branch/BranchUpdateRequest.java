package com.rasahub.auth_people_service.dto.branch;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class BranchUpdateRequest {

    @NotBlank(message = "Branch name is required")
    @Size(max = 100, message = "Branch name must be under 100 characters")
    private String name;

    public BranchUpdateRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}