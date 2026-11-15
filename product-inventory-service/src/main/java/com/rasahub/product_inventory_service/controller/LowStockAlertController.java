package com.rasahub.product_inventory_service.controller;

import com.rasahub.product_inventory_service.entity.LowStockAlert;
import com.rasahub.product_inventory_service.service.LowStockAlertEvaluationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/low-stock-alerts")
public class LowStockAlertController {

    private final LowStockAlertEvaluationService lowStockAlertEvaluationService;

    public LowStockAlertController(LowStockAlertEvaluationService lowStockAlertEvaluationService) {
        this.lowStockAlertEvaluationService = lowStockAlertEvaluationService;
    }

    @GetMapping("/location/{locationCode}")
    @PreAuthorize("hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', 'ACCOUNTANT', 'STOCK_KEEPER')")
    public ResponseEntity<List<LowStockAlert>> getAlertsByLocation(
            @PathVariable String locationCode
    ) {
        return ResponseEntity.ok(lowStockAlertEvaluationService.getAlertsByLocation(locationCode));
    }

    @GetMapping("/item/{inventoryItemId}")
    @PreAuthorize("hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', 'ACCOUNTANT', 'STOCK_KEEPER')")
    public ResponseEntity<List<LowStockAlert>> getAlertsByItem(
            @PathVariable Long inventoryItemId
    ) {
        return ResponseEntity.ok(lowStockAlertEvaluationService.getAlertsByItem(inventoryItemId));
    }

    @PostMapping("/location/{locationCode}/evaluate")
    @PreAuthorize("hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', 'STOCK_KEEPER')")
    public ResponseEntity<List<LowStockAlert>> evaluateLocation(
            @PathVariable String locationCode
    ) {
        return ResponseEntity.ok(lowStockAlertEvaluationService.evaluateLocation(locationCode));
    }

    @PostMapping("/location/{locationCode}/item/{inventoryItemId}/evaluate")
    @PreAuthorize("hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', 'STOCK_KEEPER')")
    public ResponseEntity<LowStockAlert> evaluateItem(
            @PathVariable String locationCode,
            @PathVariable Long inventoryItemId
    ) {
        LowStockAlert alert = lowStockAlertEvaluationService.evaluateItem(locationCode, inventoryItemId);
        return ResponseEntity.ok(alert);
    }

    @PatchMapping("/{alertId}/acknowledge")
    @PreAuthorize("hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', 'STOCK_KEEPER')")
    public ResponseEntity<LowStockAlert> acknowledgeAlert(
            @PathVariable Long alertId,
            @RequestParam Long accountId,
            @RequestParam(required = false) String remarks
    ) {
        return ResponseEntity.ok(lowStockAlertEvaluationService.acknowledgeAlert(alertId, accountId, remarks));
    }

    @PatchMapping("/{alertId}/resolve")
    @PreAuthorize("hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', 'STOCK_KEEPER')")
    public ResponseEntity<LowStockAlert> resolveAlert(
            @PathVariable Long alertId,
            @RequestParam(required = false) String remarks
    ) {
        return ResponseEntity.ok(lowStockAlertEvaluationService.resolveAlert(alertId, remarks));
    }
}
