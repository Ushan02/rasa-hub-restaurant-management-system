package com.rasahub.product_inventory_service.controller;

import com.rasahub.product_inventory_service.dto.stockcount.StockCountCreateRequest;
import com.rasahub.product_inventory_service.dto.stockcount.StockCountRejectRequest;
import com.rasahub.product_inventory_service.dto.stockcount.StockCountResponse;
import com.rasahub.product_inventory_service.dto.stockcount.StockCountUpdateRequest;
import com.rasahub.product_inventory_service.enums.StockCountStatus;
import com.rasahub.product_inventory_service.service.StockCountService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/stock-counts")
public class StockCountController {

    private final StockCountService service;

    public StockCountController(StockCountService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER', 'STOCK_KEEPER')")
    public ResponseEntity<StockCountResponse> create(
            @Valid @RequestBody StockCountCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.createStockCount(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER', 'STOCK_KEEPER')")
    public ResponseEntity<StockCountResponse> updateDraft(
            @PathVariable Long id,
            @Valid @RequestBody StockCountUpdateRequest request
    ) {
        return ResponseEntity.ok(
                service.updateDraftStockCount(id, request)
        );
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER', 'STOCK_KEEPER')")
    public ResponseEntity<StockCountResponse> submit(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(service.submitStockCount(id));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER')")
    public ResponseEntity<StockCountResponse> approve(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(service.approveStockCount(id));
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER')")
    public ResponseEntity<StockCountResponse> reject(
            @PathVariable Long id,
            @Valid @RequestBody StockCountRejectRequest request
    ) {
        return ResponseEntity.ok(
                service.rejectStockCount(id, request)
        );
    }

    @PostMapping("/{id}/reconcile")
    @PreAuthorize("hasRole('STOCK_KEEPER')")
    public ResponseEntity<StockCountResponse> reconcile(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(service.reconcileStockCount(id));
    }

    @GetMapping("/{id}")
    @PreAuthorize("""
            hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', 'ACCOUNTANT',
                    'STOCK_KEEPER', 'BRANCH_MANAGER')
            """)
    public ResponseEntity<StockCountResponse> getById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(service.getStockCountById(id));
    }

    @GetMapping
    @PreAuthorize("""
            hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', 'ACCOUNTANT',
                    'STOCK_KEEPER')
            """)
    public ResponseEntity<Page<StockCountResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                service.getAllStockCounts(page, size)
        );
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('BRANCH_MANAGER')")
    public ResponseEntity<Page<StockCountResponse>> getMyBranchCounts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                service.getMyBranchStockCounts(page, size)
        );
    }

    @GetMapping("/location/{locationCode}")
    @PreAuthorize("""
            hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', 'ACCOUNTANT',
                    'STOCK_KEEPER', 'BRANCH_MANAGER')
            """)
    public ResponseEntity<Page<StockCountResponse>> getByLocation(
            @PathVariable String locationCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                service.getStockCountsByLocation(
                        locationCode,
                        page,
                        size
                )
        );
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("""
            hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', 'ACCOUNTANT',
                    'STOCK_KEEPER', 'BRANCH_MANAGER')
            """)
    public ResponseEntity<Page<StockCountResponse>> getByStatus(
            @PathVariable StockCountStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                service.getStockCountsByStatus(status, page, size)
        );
    }

    @GetMapping("/date-range")
    @PreAuthorize("""
            hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', 'ACCOUNTANT',
                    'STOCK_KEEPER', 'BRANCH_MANAGER')
            """)
    public ResponseEntity<Page<StockCountResponse>> getByDateRange(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,
            @RequestParam(required = false) String locationCode,
            @RequestParam(required = false) StockCountStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                service.getStockCountsByDateRange(
                        startDate,
                        endDate,
                        locationCode,
                        status,
                        page,
                        size
                )
        );
    }
}
