package com.rasahub.auth_people_service.controller;

import com.rasahub.auth_people_service.dto.branch.BranchRequest;
import com.rasahub.auth_people_service.dto.branch.BranchResponse;
import com.rasahub.auth_people_service.dto.branch.BranchStatusUpdateRequest;
import com.rasahub.auth_people_service.dto.branch.BranchUpdateRequest;
import com.rasahub.auth_people_service.enums.Role;
import com.rasahub.auth_people_service.security.CustomUserPrincipal;
import com.rasahub.auth_people_service.service.BranchService;
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
@RequestMapping("/api/v1/branches")
public class BranchController {

    private final BranchService branchService;

    public BranchController(
            BranchService branchService
    ) {
        this.branchService = branchService;
    }

    @PostMapping
    @PreAuthorize(
            "hasAnyRole('OWNER','MAIN_OFFICE_MANAGER')"
    )
    public ResponseEntity<BranchResponse> createBranch(
            @Valid
            @RequestBody
            BranchRequest request
    ) {

        BranchResponse response =
                branchService.createBranch(
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    @PreAuthorize(
            "hasAnyRole('OWNER','MAIN_OFFICE_MANAGER','ACCOUNTANT','BRANCH_MANAGER')"
    )
    public ResponseEntity<List<BranchResponse>> getBranches(
            @RequestParam(required = false)
            Boolean active,

            Authentication authentication,

            @AuthenticationPrincipal
            CustomUserPrincipal principal
    ) {

        Role callerRole =
                extractRole(authentication);

        return ResponseEntity.ok(
                branchService.getBranches(
                        active,
                        callerRole,
                        principal.getAccountId()
                )
        );
    }

    @GetMapping("/by-code/{branchCode}")
    @PreAuthorize(
            "hasAnyRole('OWNER','MAIN_OFFICE_MANAGER','ACCOUNTANT','BRANCH_MANAGER')"
    )
    public ResponseEntity<BranchResponse> getBranchByCode(
            @PathVariable String branchCode,

            Authentication authentication,

            @AuthenticationPrincipal
            CustomUserPrincipal principal
    ) {

        Role callerRole =
                extractRole(authentication);

        return ResponseEntity.ok(
                branchService.getBranchByCode(
                        branchCode,
                        callerRole,
                        principal.getAccountId()
                )
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize(
            "hasAnyRole('OWNER','MAIN_OFFICE_MANAGER','ACCOUNTANT','BRANCH_MANAGER')"
    )
    public ResponseEntity<BranchResponse> getBranchById(
            @PathVariable Long id,

            Authentication authentication,

            @AuthenticationPrincipal
            CustomUserPrincipal principal
    ) {

        Role callerRole =
                extractRole(authentication);

        return ResponseEntity.ok(
                branchService.getBranchById(
                        id,
                        callerRole,
                        principal.getAccountId()
                )
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize(
            "hasAnyRole('OWNER','MAIN_OFFICE_MANAGER')"
    )
    public ResponseEntity<BranchResponse> updateBranch(
            @PathVariable Long id,
            @Valid
            @RequestBody
            BranchUpdateRequest request
    ) {

        return ResponseEntity.ok(
                branchService.updateBranch(
                        id,
                        request
                )
        );
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize(
            "hasAnyRole('OWNER','MAIN_OFFICE_MANAGER')"
    )
    public ResponseEntity<BranchResponse> updateBranchStatus(
            @PathVariable Long id,
            @Valid
            @RequestBody
            BranchStatusUpdateRequest request
    ) {

        return ResponseEntity.ok(
                branchService.updateBranchStatus(
                        id,
                        request
                )
        );
    }

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