
package com.rasahub.product_inventory_service.controller;

import com.rasahub.product_inventory_service.dto.branchstockrequest.BranchStockRequestCreateRequest;
import com.rasahub.product_inventory_service.dto.branchstockrequest.BranchStockRequestRejectRequest;
import com.rasahub.product_inventory_service.dto.branchstockrequest.BranchStockRequestResponse;

import com.rasahub.product_inventory_service.enums.BranchStockRequestStatus;
import com.rasahub.product_inventory_service.service.BranchStockRequestService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/branch-stock-requests")
public class BranchStockRequestController {

    private final BranchStockRequestService branchStockRequestService;

    public BranchStockRequestController(
            BranchStockRequestService branchStockRequestService
    ) {
        this.branchStockRequestService = branchStockRequestService;
    }

    @PostMapping
    @PreAuthorize("hasRole('BRANCH_MANAGER')")
    public ResponseEntity<BranchStockRequestResponse>
    createBranchStockRequest(
            @Valid @RequestBody BranchStockRequestCreateRequest request
    ) {
        BranchStockRequestResponse response =
                branchStockRequestService.createBranchStockRequest(
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    @PreAuthorize(
            "hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER')"
    )
    public ResponseEntity<Page<BranchStockRequestResponse>>
    getAllBranchStockRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                branchStockRequestService.getAllBranchStockRequests(
                        page,
                        size
                )
        );
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('BRANCH_MANAGER')")
    public ResponseEntity<Page<BranchStockRequestResponse>>
    getMyBranchStockRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                branchStockRequestService.getMyBranchStockRequests(
                        page,
                        size
                )
        );
    }

    @GetMapping("/status/{status}")
    @PreAuthorize(
            "hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', 'BRANCH_MANAGER')"
    )
    public ResponseEntity<Page<BranchStockRequestResponse>>
    getBranchStockRequestsByStatus(
            @PathVariable BranchStockRequestStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                branchStockRequestService.getBranchStockRequestsByStatus(
                        status,
                        page,
                        size
                )
        );
    }

    @GetMapping("/branch/{branchCode}")
    @PreAuthorize(
            "hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', 'BRANCH_MANAGER')"
    )
    public ResponseEntity<Page<BranchStockRequestResponse>>
    getBranchStockRequestsByBranch(
            @PathVariable("branchCode") String branchLocationCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                branchStockRequestService.getBranchStockRequestsByBranch(
                        branchLocationCode,
                        page,
                        size
                )
        );
    }

    @GetMapping("/branch/{branchCode}/status/{status}")
    @PreAuthorize(
            "hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', 'BRANCH_MANAGER')"
    )
    public ResponseEntity<Page<BranchStockRequestResponse>>
    getBranchStockRequestsByBranchAndStatus(
            @PathVariable("branchCode") String branchLocationCode,
            @PathVariable BranchStockRequestStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                branchStockRequestService
                        .getBranchStockRequestsByBranchAndStatus(
                                branchLocationCode,
                                status,
                                page,
                                size
                        )
        );
    }

    @GetMapping("/{branchStockRequestId}")
    @PreAuthorize(
            "hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', 'BRANCH_MANAGER')"
    )
    public ResponseEntity<BranchStockRequestResponse>
    getBranchStockRequestById(
            @PathVariable Long branchStockRequestId
    ) {
        return ResponseEntity.ok(
                branchStockRequestService.getBranchStockRequestById(
                        branchStockRequestId
                )
        );
    }

    @PatchMapping("/{branchStockRequestId}/approve")
    @PreAuthorize(
            "hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER')"
    )
    public ResponseEntity<BranchStockRequestResponse>
    approveBranchStockRequest(
            @PathVariable Long branchStockRequestId
    ) {
        return ResponseEntity.ok(
                branchStockRequestService.approveBranchStockRequest(
                        branchStockRequestId
                )
        );
    }

    @PatchMapping("/{branchStockRequestId}/reject")
    @PreAuthorize(
            "hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER')"
    )
    public ResponseEntity<BranchStockRequestResponse>
    rejectBranchStockRequest(
            @PathVariable Long branchStockRequestId,
            @Valid @RequestBody BranchStockRequestRejectRequest request
    ) {
        return ResponseEntity.ok(
                branchStockRequestService.rejectBranchStockRequest(
                        branchStockRequestId,
                        request
                )
        );
    }
}
