package com.rasahub.product_inventory_service.controller;

import com.rasahub.product_inventory_service.entity.StockReorderLevel;
import com.rasahub.product_inventory_service.service.StockReorderLevelService;
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
    @PreAuthorize("hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', 'ACCOUNTANT', 'STOCK_KEEPER')")
    public ResponseEntity<List<StockReorderLevel>> getConfigurationsByLocation(
            @PathVariable String locationCode
    ) {
        return ResponseEntity.ok(stockReorderLevelService.getConfigurationsByLocation(locationCode));
    }

    @GetMapping("/location/{locationCode}/item/{inventoryItemId}")
    @PreAuthorize("hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', 'ACCOUNTANT', 'STOCK_KEEPER')")
    public ResponseEntity<StockReorderLevel> getConfiguration(
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
    public ResponseEntity<StockReorderLevel> createConfiguration(
            @PathVariable String locationCode,
            @PathVariable Long inventoryItemId,
            @RequestParam BigDecimal reorderLevel,
            @RequestParam(required = false) BigDecimal recommendedReorderQuantity,
            @RequestParam(required = false) Long createdByAccountId
    ) {
        return ResponseEntity.ok(
                stockReorderLevelService.createConfiguration(
                        locationCode,
                        inventoryItemId,
                        reorderLevel,
                        recommendedReorderQuantity,
                        createdByAccountId
                )
        );
    }

    @PutMapping("/location/{locationCode}/item/{inventoryItemId}")
    @PreAuthorize("hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', 'STOCK_KEEPER')")
    public ResponseEntity<StockReorderLevel> updateConfiguration(
            @PathVariable String locationCode,
            @PathVariable Long inventoryItemId,
            @RequestParam BigDecimal reorderLevel,
            @RequestParam(required = false) BigDecimal recommendedReorderQuantity,
            @RequestParam(required = false) Long updatedByAccountId
    ) {
        return ResponseEntity.ok(
                stockReorderLevelService.updateConfiguration(
                        locationCode,
                        inventoryItemId,
                        reorderLevel,
                        recommendedReorderQuantity,
                        updatedByAccountId
                )
        );
    }

    @PatchMapping("/location/{locationCode}/item/{inventoryItemId}/deactivate")
    @PreAuthorize("hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', 'STOCK_KEEPER')")
    public ResponseEntity<StockReorderLevel> deactivateConfiguration(
            @PathVariable String locationCode,
            @PathVariable Long inventoryItemId,
            @RequestParam(required = false) Long updatedByAccountId
    ) {
        return ResponseEntity.ok(
                stockReorderLevelService.deactivateConfiguration(
                        locationCode,
                        inventoryItemId,
                        updatedByAccountId
                )
        );
    }
}
