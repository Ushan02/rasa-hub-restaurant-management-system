package com.rasahub.auth_people_service.controller;

import com.rasahub.auth_people_service.dto.employee.EmployeeCreateRequest;
import com.rasahub.auth_people_service.dto.employee.EmployeeResponse;
import com.rasahub.auth_people_service.dto.employee.EmployeeUpdateRequest;
import com.rasahub.auth_people_service.enums.Role;
import com.rasahub.auth_people_service.security.CustomUserPrincipal;
import com.rasahub.auth_people_service.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(
            EmployeeService employeeService
    ) {
        this.employeeService = employeeService;
    }

    // CREATE EMPLOYEE


    @PostMapping
    @PreAuthorize(
            "hasAnyRole('OWNER','MAIN_OFFICE_MANAGER','ACCOUNTANT')"
    )
    public ResponseEntity<EmployeeResponse> createEmployee(
            @Valid
            @RequestBody
            EmployeeCreateRequest request,

            Authentication authentication
    ) {

        Role callerRole =
                extractRole(
                        authentication
                );

        EmployeeResponse response =
                employeeService.createEmployee(
                        request,
                        callerRole
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // GET EMPLOYEE LIST

    @GetMapping
    @PreAuthorize(
            "hasAnyRole('OWNER','MAIN_OFFICE_MANAGER','ACCOUNTANT','BRANCH_MANAGER')"
    )
    public ResponseEntity<List<EmployeeResponse>> getEmployees(
            Authentication authentication,

            @AuthenticationPrincipal
            CustomUserPrincipal principal,

            @RequestParam(required = false)
            Long branchId
    ) {

        Role callerRole =
                extractRole(
                        authentication
                );

        List<EmployeeResponse> employees =
                employeeService.getEmployees(
                        callerRole,
                        principal.getAccountId(),
                        branchId
                );

        return ResponseEntity.ok(
                employees
        );
    }

    // GET SINGLE EMPLOYEE

    @GetMapping("/{id}")
    @PreAuthorize(
            "hasAnyRole('OWNER','MAIN_OFFICE_MANAGER','ACCOUNTANT','BRANCH_MANAGER')"
    )
    public ResponseEntity<EmployeeResponse> getEmployeeById(
            @PathVariable
            Long id,

            Authentication authentication,

            @AuthenticationPrincipal
            CustomUserPrincipal principal
    ) {

        Role callerRole =
                extractRole(
                        authentication
                );

        EmployeeResponse response =
                employeeService.getEmployeeById(
                        id,
                        callerRole,
                        principal.getAccountId()
                );

        return ResponseEntity.ok(
                response
        );
    }

    // UPDATE EMPLOYEE


    @PutMapping("/{id}")
    @PreAuthorize(
            "hasAnyRole('OWNER','MAIN_OFFICE_MANAGER','ACCOUNTANT')"
    )
    public ResponseEntity<EmployeeResponse> updateEmployee(
            @PathVariable
            Long id,

            @Valid
            @RequestBody
            EmployeeUpdateRequest request,

            Authentication authentication,

            @AuthenticationPrincipal
            CustomUserPrincipal principal
    ) {

        Role callerRole =
                extractRole(
                        authentication
                );

        EmployeeResponse response =
                employeeService.updateEmployee(
                        id,
                        request,
                        callerRole,
                        principal.getAccountId()
                );

        return ResponseEntity.ok(
                response
        );
    }


    // ROLE HELPER

    private Role extractRole(
            Authentication authentication
    ) {

        return authentication
                .getAuthorities()
                .stream()
                .map(
                        GrantedAuthority::getAuthority
                )
                .filter(authority ->
                        authority.startsWith(
                                "ROLE_"
                        )
                )
                .map(authority ->
                        Role.valueOf(
                                authority.substring(5)
                        )
                )
                .findFirst()
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "No role found"
                        )
                );
    }
}