package com.rasahub.auth_people_service.controller;

import com.rasahub.auth_people_service.dto.auth.StaffPasswordChangeRequest;
import com.rasahub.auth_people_service.dto.auth.StaffPasswordResetRequest;
import com.rasahub.auth_people_service.enums.Role;
import com.rasahub.auth_people_service.security.CustomUserPrincipal;
import com.rasahub.auth_people_service.service.StaffAccountService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/staff/accounts")
public class StaffAccountController {

    private final StaffAccountService staffAccountService;

    public StaffAccountController(
            StaffAccountService staffAccountService
    ) {
        this.staffAccountService = staffAccountService;
    }

    @PutMapping("/me/password")
    @PreAuthorize(
            "hasAnyRole('OWNER','MAIN_OFFICE_MANAGER','ACCOUNTANT','STOCK_KEEPER','BRANCH_MANAGER','CASHIER')"
    )
    public ResponseEntity<Void> changeOwnPassword(
            @Valid @RequestBody StaffPasswordChangeRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {

        staffAccountService.changeOwnPassword(
                principal.getAccountId(),
                request
        );

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/employees/{id}/password")
    @PreAuthorize(
            "hasAnyRole('OWNER','MAIN_OFFICE_MANAGER','ACCOUNTANT')"
    )
    public ResponseEntity<Void> resetEmployeePassword(
            @PathVariable Long id,
            @Valid @RequestBody StaffPasswordResetRequest request,
            Authentication authentication,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {

        Role callerRole =
                extractRole(authentication);

        staffAccountService.resetEmployeePassword(
                id,
                principal.getAccountId(),
                callerRole,
                request
        );

        return ResponseEntity.noContent().build();
    }

    private Role extractRole(
            Authentication authentication
    ) {

        return authentication
                .getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority ->
                        authority.startsWith("ROLE_")
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