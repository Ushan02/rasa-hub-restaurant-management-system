package com.rasahub.auth_people_service.service;

import com.rasahub.auth_people_service.dto.employee.EmployeeCreateRequest;
import com.rasahub.auth_people_service.dto.employee.EmployeeResponse;
import com.rasahub.auth_people_service.enums.Role;

public interface EmployeeService {

    EmployeeResponse createEmployee(EmployeeCreateRequest request, Role callerRole);
}