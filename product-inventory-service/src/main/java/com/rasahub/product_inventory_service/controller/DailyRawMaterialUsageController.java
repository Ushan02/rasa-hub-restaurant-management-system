package com.rasahub.product_inventory_service.controller;

import com.rasahub.product_inventory_service.dto.dailyrawmaterialusage.DailyRawMaterialUsageCreateRequest;
import com.rasahub.product_inventory_service.dto.dailyrawmaterialusage.DailyRawMaterialUsageResponse;
import com.rasahub.product_inventory_service.dto.dailyrawmaterialusage.DailyRawMaterialUsageUpdateRequest;
import com.rasahub.product_inventory_service.enums.DailyRawMaterialUsageStatus;
import com.rasahub.product_inventory_service.service.DailyRawMaterialUsageService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/daily-raw-material-usages")
public class DailyRawMaterialUsageController {

    private final DailyRawMaterialUsageService service;

    public DailyRawMaterialUsageController(
            DailyRawMaterialUsageService service
    ) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasRole('BRANCH_MANAGER')")
    public ResponseEntity<DailyRawMaterialUsageResponse>
    createDailyUsage(
            @Valid @RequestBody DailyRawMaterialUsageCreateRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.createDailyUsage(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('BRANCH_MANAGER')")
    public ResponseEntity<DailyRawMaterialUsageResponse>
    updateDraftUsage(
            @PathVariable Long id,
            @Valid @RequestBody DailyRawMaterialUsageUpdateRequest request
    ) {
        return ResponseEntity.ok(
                service.updateDraftUsage(id, request)
        );
    }

    @PostMapping("/{id}/post")
    @PreAuthorize("hasRole('BRANCH_MANAGER')")
    public ResponseEntity<DailyRawMaterialUsageResponse>
    postDailyUsage(@PathVariable Long id) {
        return ResponseEntity.ok(service.postDailyUsage(id));
    }

    @GetMapping("/{id}")
    @PreAuthorize(
            "hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', 'BRANCH_MANAGER')"
    )
    public ResponseEntity<DailyRawMaterialUsageResponse>
    getDailyUsageById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getDailyUsageById(id));
    }

    @GetMapping
    @PreAuthorize(
            "hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER')"
    )
    public ResponseEntity<Page<DailyRawMaterialUsageResponse>>
    getAllDailyUsages(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                service.getAllDailyUsages(page, size)
        );
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('BRANCH_MANAGER')")
    public ResponseEntity<Page<DailyRawMaterialUsageResponse>>
    getMyBranchDailyUsages(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                service.getMyBranchDailyUsages(page, size)
        );
    }

    @GetMapping("/status/{status}")
    @PreAuthorize(
            "hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', 'BRANCH_MANAGER')"
    )
    public ResponseEntity<Page<DailyRawMaterialUsageResponse>>
    getDailyUsagesByStatus(
            @PathVariable DailyRawMaterialUsageStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                service.getDailyUsagesByStatus(status, page, size)
        );
    }

    @GetMapping("/branch/{branchCode}")
    @PreAuthorize(
            "hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', 'BRANCH_MANAGER')"
    )
    public ResponseEntity<Page<DailyRawMaterialUsageResponse>>
    getDailyUsagesByBranch(
            @PathVariable("branchCode") String branchLocationCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                service.getDailyUsagesByBranch(
                        branchLocationCode,
                        page,
                        size
                )
        );
    }

    @GetMapping("/branch/{branchCode}/date-range")
    @PreAuthorize(
            "hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', 'BRANCH_MANAGER')"
    )
    public ResponseEntity<Page<DailyRawMaterialUsageResponse>>
    getDailyUsagesByDateRange(
            @PathVariable("branchCode") String branchLocationCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                service.getDailyUsagesByDateRange(
                        branchLocationCode,
                        startDate,
                        endDate,
                        page,
                        size
                )
        );
    }
}
