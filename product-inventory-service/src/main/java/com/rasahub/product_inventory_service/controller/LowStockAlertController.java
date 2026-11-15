package com.rasahub.product_inventory_service.controller;

import com.rasahub.product_inventory_service.dto.lowstock.AlertRemarksRequest;
import com.rasahub.product_inventory_service.dto.lowstock.LowStockAlertResponse;
import com.rasahub.product_inventory_service.dto.lowstock.LowStockSummaryResponse;
import com.rasahub.product_inventory_service.entity.LowStockAlert;
import com.rasahub.product_inventory_service.service.LowStockAlertEvaluationService;
import jakarta.validation.Valid;
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
    @PreAuthorize("hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', 'ACCOUNTANT', 'STOCK_KEEPER', 'BRANCH_MANAGER')")
    public ResponseEntity<List<LowStockAlertResponse>> getAlertsByLocation(
            @PathVariable String locationCode
    ) {
        return ResponseEntity.ok(lowStockAlertEvaluationService.getAlertsByLocation(locationCode));
    }

    @GetMapping("/item/{inventoryItemId}")
    @PreAuthorize("hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', 'ACCOUNTANT', 'STOCK_KEEPER', 'BRANCH_MANAGER')")
    public ResponseEntity<List<LowStockAlertResponse>> getAlertsByItem(
            @PathVariable Long inventoryItemId
    ) {
        return ResponseEntity.ok(lowStockAlertEvaluationService.getAlertsByItem(inventoryItemId));
    }

    @PostMapping("/location/{locationCode}/evaluate")
    @PreAuthorize("hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', 'STOCK_KEEPER')")
    public ResponseEntity<List<LowStockAlertResponse>> evaluateLocation(
            @PathVariable String locationCode
    ) {
        return ResponseEntity.ok(lowStockAlertEvaluationService.evaluateLocationAuthorized(locationCode)
                .stream()
                .map(LowStockAlertResponse::from)
                .toList());
    }

    @PostMapping("/location/{locationCode}/item/{inventoryItemId}/evaluate")
    @PreAuthorize("hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', 'STOCK_KEEPER')")
    public ResponseEntity<LowStockAlertResponse> evaluateItem(
            @PathVariable String locationCode,
            @PathVariable Long inventoryItemId
    ) {
        LowStockAlert alert = lowStockAlertEvaluationService.evaluateItemAuthorized(locationCode, inventoryItemId);
        return ResponseEntity.ok(alert == null ? null : LowStockAlertResponse.from(alert));
    }

    @PatchMapping("/{alertId}/acknowledge")
    @PreAuthorize("hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', 'STOCK_KEEPER', 'BRANCH_MANAGER')")
    public ResponseEntity<LowStockAlertResponse> acknowledgeAlert(
            @PathVariable Long alertId,
            @Valid @ModelAttribute AlertRemarksRequest request
    ) {
        return ResponseEntity.ok(lowStockAlertEvaluationService.acknowledgeAlert(alertId, null, request.remarks()));
    }

    @PatchMapping("/{alertId}/resolve")
    @PreAuthorize("hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', 'STOCK_KEEPER', 'BRANCH_MANAGER')")
    public ResponseEntity<LowStockAlertResponse> resolveAlert(
            @PathVariable Long alertId,
            @Valid @ModelAttribute AlertRemarksRequest request
    ) {
        return ResponseEntity.ok(lowStockAlertEvaluationService.resolveAlert(alertId, request.remarks()));
    }

    @GetMapping("/location/{locationCode}/summary")
    @PreAuthorize("hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', 'ACCOUNTANT', 'STOCK_KEEPER', 'BRANCH_MANAGER')")
    public ResponseEntity<LowStockSummaryResponse> getSummaryByLocation(
            @PathVariable String locationCode
    ) {
        return ResponseEntity.ok(lowStockAlertEvaluationService.getSummaryByLocation(locationCode));
    }
}
