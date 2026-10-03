package com.rasahub.auth_people_service.dto.employee;

import com.rasahub.auth_people_service.enums.EmploymentStatus;
import jakarta.validation.constraints.NotNull;

public class EmployeeStatusUpdateRequest {

    @NotNull
    private EmploymentStatus status;

    public EmployeeStatusUpdateRequest() {
    }

    public EmploymentStatus getStatus() {
        return status;
    }

    public void setStatus(EmploymentStatus status) {
        this.status = status;
    }
}