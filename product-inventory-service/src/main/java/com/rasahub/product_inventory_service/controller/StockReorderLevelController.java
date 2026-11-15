package com.rasahub.product_inventory_service.controller;

import com.rasahub.product_inventory_service.dto.stockreorderlevel.StockReorderLevelRequest;
import com.rasahub.product_inventory_service.dto.stockreorderlevel.StockReorderLevelResponse;
import com.rasahub.product_inventory_service.service.StockReorderLevelService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/stock-reorder-levels")
public class StockReorderLevelController {

    private final StockReorderLevelService stockReorderLevelService;

    public StockReorderLevelController(StockReorderLevelService stockReorderLevelService) {
        this.stockReorderLevelService = stockReorderLevelService;
    }

    @GetMapping("/location/{locationCode}")
    @PreAuthorize("hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', 'ACCOUNTANT', 'STOCK_KEEPER', 'BRANCH_MANAGER')")
    public ResponseEntity<List<StockReorderLevelResponse>> getConfigurationsByLocation(
            @PathVariable String locationCode
    ) {
        return ResponseEntity.ok(stockReorderLevelService.getConfigurationsByLocation(locationCode));
    }

    @GetMapping("/location/{locationCode}/item/{inventoryItemId}")
    @PreAuthorize("hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', 'ACCOUNTANT', 'STOCK_KEEPER', 'BRANCH_MANAGER')")
    public ResponseEntity<StockReorderLevelResponse> getConfiguration(
            @PathVariable String locationCode,
            @PathVariable Long inventoryItemId
    ) {
        return ResponseEntity.ok(
                stockReorderLevelService.getConfiguration(locationCode, inventoryItemId)
                        .orElseThrow(() -> new IllegalArgumentException("Reorder level configuration not found"))
        );
    }

    @PostMapping("/location/{locationCode}/item/{inventoryItemId}")
    @PreAuthorize("hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', 'STOCK_KEEPER')")
    public ResponseEntity<StockReorderLevelResponse> createConfiguration(
            @PathVariable String locationCode,
            @PathVariable Long inventoryItemId,
            @Valid @ModelAttribute StockReorderLevelRequest request
    ) {
        return ResponseEntity.ok(
                stockReorderLevelService.createConfiguration(
                        locationCode,
                        inventoryItemId,
                        request.reorderLevel(),
                        request.recommendedReorderQuantity(),
                        null
                )
        );
    }

    @PutMapping("/location/{locationCode}/item/{inventoryItemId}")
    @PreAuthorize("hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', 'STOCK_KEEPER')")
    public ResponseEntity<StockReorderLevelResponse> updateConfiguration(
            @PathVariable String locationCode,
            @PathVariable Long inventoryItemId,
            @Valid @ModelAttribute StockReorderLevelRequest request
    ) {
        return ResponseEntity.ok(
                stockReorderLevelService.updateConfiguration(
                        locationCode,
                        inventoryItemId,
                        request.reorderLevel(),
                        request.recommendedReorderQuantity(),
                        null
                )
        );
    }

    @PatchMapping("/location/{locationCode}/item/{inventoryItemId}/deactivate")
    @PreAuthorize("hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', 'STOCK_KEEPER')")
    public ResponseEntity<StockReorderLevelResponse> deactivateConfiguration(
            @PathVariable String locationCode,
            @PathVariable Long inventoryItemId
    ) {
        return ResponseEntity.ok(
                stockReorderLevelService.deactivateConfiguration(
                        locationCode,
                        inventoryItemId,
                        null
                )
        );
    }

    @PatchMapping("/location/{locationCode}/item/{inventoryItemId}/activate")
    @PreAuthorize("hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', 'STOCK_KEEPER')")
    public ResponseEntity<StockReorderLevelResponse> reactivateConfiguration(
            @PathVariable String locationCode,
            @PathVariable Long inventoryItemId
    ) {
        return ResponseEntity.ok(
                stockReorderLevelService.reactivateConfiguration(
                        locationCode,
                        inventoryItemId,
                        null
                )
        );
    }
}
