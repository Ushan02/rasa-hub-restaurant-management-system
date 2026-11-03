
package com.rasahub.product_inventory_service.controller;

import com.rasahub.product_inventory_service.dto.inventory.InventoryItemCreateRequest;
import com.rasahub.product_inventory_service.dto.inventory.InventoryItemUpdateRequest;
import com.rasahub.product_inventory_service.dto.inventory.InventoryItemResponse;
import com.rasahub.product_inventory_service.dto.inventory.InventoryItemStatusUpdateRequest;
import com.rasahub.product_inventory_service.service.InventoryItemService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory-items")
public class InventoryItemController {

    private final InventoryItemService inventoryItemService;

    public InventoryItemController(
            InventoryItemService inventoryItemService
    ) {
        this.inventoryItemService = inventoryItemService;
    }

    @PostMapping
    @PreAuthorize(
            "hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER')"
    )
    public ResponseEntity<InventoryItemResponse> createInventoryItem(
            @Valid @RequestBody InventoryItemCreateRequest request
    ) {

        InventoryItemResponse response =
                inventoryItemService.createInventoryItem(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    @PreAuthorize(
            "hasAnyRole(" +
                    "'OWNER'," +
                    "'MAIN_OFFICE_MANAGER'," +
                    "'ACCOUNTANT'," +
                    "'BRANCH_MANAGER'," +
                    "'STOCK_KEEPER'" +
                    ")"
    )
    public ResponseEntity<List<InventoryItemResponse>> getInventoryItems(
            @RequestParam(required = false) Boolean active
    ) {

        return ResponseEntity.ok(
                inventoryItemService.getInventoryItems(active)
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize(
            "hasAnyRole(" +
                    "'OWNER'," +
                    "'MAIN_OFFICE_MANAGER'," +
                    "'ACCOUNTANT'," +
                    "'BRANCH_MANAGER'," +
                    "'STOCK_KEEPER'" +
                    ")"
    )
    public ResponseEntity<InventoryItemResponse> getInventoryItemById(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                inventoryItemService.getInventoryItemById(id)
        );
    }

    @GetMapping("/by-code/{itemCode}")
    @PreAuthorize(
            "hasAnyRole(" +
                    "'OWNER'," +
                    "'MAIN_OFFICE_MANAGER'," +
                    "'ACCOUNTANT'," +
                    "'BRANCH_MANAGER'," +
                    "'STOCK_KEEPER'" +
                    ")"
    )
    public ResponseEntity<InventoryItemResponse> getInventoryItemByCode(
            @PathVariable String itemCode
    ) {

        return ResponseEntity.ok(
                inventoryItemService.getInventoryItemByCode(itemCode)
        );
    }

    @PatchMapping("/{id}")
    @PreAuthorize(
            "hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER')"
    )
    public ResponseEntity<InventoryItemResponse> updateInventoryItem(
            @PathVariable Long id,
            @Valid @RequestBody InventoryItemUpdateRequest request
    ) {

        return ResponseEntity.ok(
                inventoryItemService.updateInventoryItem(id, request)
        );
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize(
            "hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER')"
    )
    public ResponseEntity<InventoryItemResponse> updateInventoryItemStatus(
            @PathVariable Long id,
            @Valid @RequestBody InventoryItemStatusUpdateRequest request
    ) {

        return ResponseEntity.ok(
                inventoryItemService.updateInventoryItemStatus(
                        id,
                        request.getActive()
                )
        );
    }
}
