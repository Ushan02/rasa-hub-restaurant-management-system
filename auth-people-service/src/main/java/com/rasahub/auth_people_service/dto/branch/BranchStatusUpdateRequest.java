package com.rasahub.auth_people_service.dto.branch;

import jakarta.validation.constraints.NotNull;

public class BranchStatusUpdateRequest {

    @NotNull(message = "Active status is required")
    private Boolean active;

    public BranchStatusUpdateRequest() {
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}