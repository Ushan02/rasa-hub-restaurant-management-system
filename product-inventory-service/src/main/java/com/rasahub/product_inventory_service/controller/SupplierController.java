
package com.rasahub.product_inventory_service.controller;

import com.rasahub.product_inventory_service.dto.supplier.SupplierCreateRequest;
import com.rasahub.product_inventory_service.dto.supplier.SupplierUpdateRequest;
import com.rasahub.product_inventory_service.dto.supplier.SupplierResponse;
import com.rasahub.product_inventory_service.dto.supplier.SupplierStatusUpdateRequest;
import com.rasahub.product_inventory_service.service.SupplierService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/suppliers")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER')")
    public ResponseEntity<SupplierResponse> createSupplier(
            @Valid @RequestBody SupplierCreateRequest request
    ) {

        SupplierResponse response =
                supplierService.createSupplier(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    @PreAuthorize(
            "hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', " +
            "'ACCOUNTANT', 'STOCK_KEEPER')"
    )
    public ResponseEntity<List<SupplierResponse>> getSuppliers(
            @RequestParam(required = false) Boolean active
    ) {

        return ResponseEntity.ok(
                supplierService.getSuppliers(active)
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize(
            "hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', " +
            "'ACCOUNTANT', 'STOCK_KEEPER')"
    )
    public ResponseEntity<SupplierResponse> getSupplierById(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                supplierService.getSupplierById(id)
        );
    }

    @GetMapping("/by-code/{supplierCode}")
    @PreAuthorize(
            "hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', " +
            "'ACCOUNTANT', 'STOCK_KEEPER')"
    )
    public ResponseEntity<SupplierResponse> getSupplierByCode(
            @PathVariable String supplierCode
    ) {

        return ResponseEntity.ok(
                supplierService.getSupplierByCode(supplierCode)
        );
    }

    @GetMapping("/search")
    @PreAuthorize(
            "hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', " +
            "'ACCOUNTANT', 'STOCK_KEEPER')"
    )
    public ResponseEntity<List<SupplierResponse>> searchSuppliers(
            @RequestParam("name") String supplierName
    ) {

        return ResponseEntity.ok(
                supplierService.searchSuppliersByName(supplierName)
        );
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER')")
    public ResponseEntity<SupplierResponse> updateSupplier(
            @PathVariable Long id,
            @Valid @RequestBody SupplierUpdateRequest request
    ) {

        return ResponseEntity.ok(
                supplierService.updateSupplier(id, request)
        );
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER')")
    public ResponseEntity<SupplierResponse> updateSupplierStatus(
            @PathVariable Long id,
            @Valid @RequestBody SupplierStatusUpdateRequest request
    ) {

        return ResponseEntity.ok(
                supplierService.updateSupplierStatus(
                        id,
                        request.getActive()
                )
        );
    }
}
