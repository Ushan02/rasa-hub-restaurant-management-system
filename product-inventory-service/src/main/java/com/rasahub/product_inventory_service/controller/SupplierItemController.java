
package com.rasahub.product_inventory_service.controller;

import com.rasahub.product_inventory_service.dto.supplieritem.SupplierItemCreateRequest;
import com.rasahub.product_inventory_service.dto.supplieritem.SupplierItemResponse;
import com.rasahub.product_inventory_service.dto.supplieritem.SupplierItemStatusUpdateRequest;
import com.rasahub.product_inventory_service.dto.supplieritem.SupplierItemUpdateRequest;
import com.rasahub.product_inventory_service.service.SupplierItemService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/supplier-items")
public class SupplierItemController {

    private final SupplierItemService supplierItemService;

    public SupplierItemController(
            SupplierItemService supplierItemService
    ) {
        this.supplierItemService = supplierItemService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER')")
    public ResponseEntity<SupplierItemResponse> createSupplierItem(
            @Valid @RequestBody SupplierItemCreateRequest request
    ) {
        SupplierItemResponse response =
                supplierItemService.createSupplierItem(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', 'ACCOUNTANT', 'STOCK_KEEPER')")
    public ResponseEntity<List<SupplierItemResponse>> getSupplierItems(
            @RequestParam(required = false) Boolean active
    ) {
        List<SupplierItemResponse> response =
                supplierItemService.getSupplierItems(active);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', 'ACCOUNTANT', 'STOCK_KEEPER')")
    public ResponseEntity<SupplierItemResponse> getSupplierItemById(
            @PathVariable Long id
    ) {
        SupplierItemResponse response =
                supplierItemService.getSupplierItemById(id);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/by-supplier-and-item")
    @PreAuthorize("hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', 'ACCOUNTANT', 'STOCK_KEEPER')")
    public ResponseEntity<SupplierItemResponse> getSupplierItemBySupplierAndInventoryItem(
            @RequestParam Long supplierId,
            @RequestParam Long inventoryItemId
    ) {
        SupplierItemResponse response =
                supplierItemService.getSupplierItemBySupplierAndInventoryItem(
                        supplierId,
                        inventoryItemId
                );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/by-supplier/{supplierId}")
    @PreAuthorize("hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', 'ACCOUNTANT', 'STOCK_KEEPER')")
    public ResponseEntity<List<SupplierItemResponse>> getSupplierItemsBySupplier(
            @PathVariable Long supplierId,
            @RequestParam(required = false) Boolean active
    ) {
        List<SupplierItemResponse> response =
                supplierItemService.getSupplierItemsBySupplier(
                        supplierId,
                        active
                );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/by-inventory-item/{inventoryItemId}")
    @PreAuthorize("hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', 'ACCOUNTANT', 'STOCK_KEEPER')")
    public ResponseEntity<List<SupplierItemResponse>> getSupplierItemsByInventoryItem(
            @PathVariable Long inventoryItemId
    ) {
        List<SupplierItemResponse> response =
                supplierItemService.getSupplierItemsByInventoryItem(
                        inventoryItemId
                );

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER')")
    public ResponseEntity<SupplierItemResponse> updateSupplierItem(
            @PathVariable Long id,
            @Valid @RequestBody SupplierItemUpdateRequest request
    ) {
        SupplierItemResponse response =
                supplierItemService.updateSupplierItem(
                        id,
                        request
                );

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER')")
    public ResponseEntity<SupplierItemResponse> updateSupplierItemStatus(
            @PathVariable Long id,
            @Valid @RequestBody SupplierItemStatusUpdateRequest request
    ) {
        SupplierItemResponse response =
                supplierItemService.updateSupplierItemStatus(
                        id,
                        request.getActive()
                );

        return ResponseEntity.ok(response);
    }
}
