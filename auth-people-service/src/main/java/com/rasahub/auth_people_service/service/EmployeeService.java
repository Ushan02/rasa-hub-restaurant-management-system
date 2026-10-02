package com.rasahub.auth_people_service.service;

import com.rasahub.auth_people_service.dto.employee.EmployeeCreateRequest;
import com.rasahub.auth_people_service.dto.employee.EmployeeResponse;
import com.rasahub.auth_people_service.enums.Role;

import java.util.List;

public interface EmployeeService {

    EmployeeResponse createEmployee(
            EmployeeCreateRequest request,
            Role callerRole
    );

    List<EmployeeResponse> getEmployees(
            Role callerRole,
            Long callerAccountId,
            Long requestedBranchId
    );

    EmployeeResponse getEmployeeById(
            Long employeeId,
            Role callerRole,
            Long callerAccountId
    );
}