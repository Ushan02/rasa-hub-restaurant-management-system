package com.rasahub.auth_people_service.controller;

import com.rasahub.auth_people_service.dto.employee.EmployeeCreateRequest;
import com.rasahub.auth_people_service.dto.employee.EmployeeResponse;
import com.rasahub.auth_people_service.enums.Role;
import com.rasahub.auth_people_service.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER','MAIN_OFFICE_MANAGER','ACCOUNTANT')")
    public ResponseEntity<EmployeeResponse> createEmployee(
            @Valid @RequestBody EmployeeCreateRequest request,
            Authentication authentication) {

        Role callerRole = extractRole(authentication);
        EmployeeResponse response = employeeService.createEmployee(request, callerRole);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private Role extractRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> Role.valueOf(authority.substring(5)))
                .findFirst()
                .orElseThrow(() -> new AccessDeniedException("No role found"));
    }
}