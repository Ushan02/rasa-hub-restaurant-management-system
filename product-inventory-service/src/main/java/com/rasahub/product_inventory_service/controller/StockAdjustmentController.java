package com.rasahub.product_inventory_service.controller;

import com.rasahub.product_inventory_service.dto.stockadjustment.StockAdjustmentCreateRequest;
import com.rasahub.product_inventory_service.dto.stockadjustment.StockAdjustmentRejectRequest;
import com.rasahub.product_inventory_service.dto.stockadjustment.StockAdjustmentResponse;
import com.rasahub.product_inventory_service.dto.stockadjustment.StockAdjustmentUpdateRequest;
import com.rasahub.product_inventory_service.enums.StockAdjustmentStatus;
import com.rasahub.product_inventory_service.service.StockAdjustmentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/stock-adjustments")
public class StockAdjustmentController {

    private final StockAdjustmentService service;

    public StockAdjustmentController(StockAdjustmentService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER', 'STOCK_KEEPER')")
    public ResponseEntity<StockAdjustmentResponse> create(
            @Valid @RequestBody StockAdjustmentCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.createStockAdjustment(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER', 'STOCK_KEEPER')")
    public ResponseEntity<StockAdjustmentResponse> updateDraft(
            @PathVariable Long id,
            @Valid @RequestBody StockAdjustmentUpdateRequest request
    ) {
        return ResponseEntity.ok(
                service.updateDraftStockAdjustment(id, request)
        );
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER', 'STOCK_KEEPER')")
    public ResponseEntity<StockAdjustmentResponse> submit(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                service.submitStockAdjustment(id)
        );
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER')")
    public ResponseEntity<StockAdjustmentResponse> approve(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                service.approveStockAdjustment(id)
        );
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER')")
    public ResponseEntity<StockAdjustmentResponse> reject(
            @PathVariable Long id,
            @Valid @RequestBody StockAdjustmentRejectRequest request
    ) {
        return ResponseEntity.ok(
                service.rejectStockAdjustment(id, request)
        );
    }

    @PostMapping("/{id}/post")
    @PreAuthorize("hasRole('STOCK_KEEPER')")
    public ResponseEntity<StockAdjustmentResponse> post(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                service.postStockAdjustment(id)
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("""
            hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', 'ACCOUNTANT',
                    'STOCK_KEEPER', 'BRANCH_MANAGER')
            """)
    public ResponseEntity<StockAdjustmentResponse> getById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                service.getStockAdjustmentById(id)
        );
    }

    @GetMapping
    @PreAuthorize("""
            hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', 'ACCOUNTANT',
                    'STOCK_KEEPER')
            """)
    public ResponseEntity<Page<StockAdjustmentResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                service.getAllStockAdjustments(page, size)
        );
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER', 'STOCK_KEEPER')")
    public ResponseEntity<Page<StockAdjustmentResponse>> getMine(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                service.getMyStockAdjustments(page, size)
        );
    }

    @GetMapping("/location/{locationCode}")
    @PreAuthorize("""
            hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', 'ACCOUNTANT',
                    'STOCK_KEEPER', 'BRANCH_MANAGER')
            """)
    public ResponseEntity<Page<StockAdjustmentResponse>> getByLocation(
            @PathVariable String locationCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                service.getStockAdjustmentsByLocation(
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
    public ResponseEntity<Page<StockAdjustmentResponse>> getByStatus(
            @PathVariable StockAdjustmentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                service.getStockAdjustmentsByStatus(status, page, size)
        );
    }

    @GetMapping("/date-range")
    @PreAuthorize("""
            hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', 'ACCOUNTANT',
                    'STOCK_KEEPER', 'BRANCH_MANAGER')
            """)
    public ResponseEntity<Page<StockAdjustmentResponse>> getByDateRange(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,
            @RequestParam(required = false) String locationCode,
            @RequestParam(required = false) StockAdjustmentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                service.getStockAdjustmentsByDateRange(
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
