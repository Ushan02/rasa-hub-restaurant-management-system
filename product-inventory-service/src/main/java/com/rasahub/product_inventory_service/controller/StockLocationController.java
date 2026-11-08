
package com.rasahub.product_inventory_service.controller;

import com.rasahub.product_inventory_service.dto.stocklocation.StockLocationResponse;
import com.rasahub.product_inventory_service.dto.stocklocation.StockLocationStatusUpdateRequest;
import com.rasahub.product_inventory_service.dto.stocklocation.StockLocationUpdateRequest;
import com.rasahub.product_inventory_service.enums.StockLocationType;
import com.rasahub.product_inventory_service.service.StockLocationService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stock-locations")
public class StockLocationController {

    private final StockLocationService stockLocationService;

    public StockLocationController(
            StockLocationService stockLocationService
    ) {
        this.stockLocationService = stockLocationService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', 'ACCOUNTANT', 'STOCK_KEEPER', 'BRANCH_MANAGER')")
    public ResponseEntity<List<StockLocationResponse>> getStockLocations(
            @RequestParam(required = false) StockLocationType locationType,
            @RequestParam(required = false) Boolean active
    ) {
        List<StockLocationResponse> response =
                stockLocationService.getStockLocations(
                        locationType,
                        active
                );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{locationCode}")
    @PreAuthorize("hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', 'ACCOUNTANT', 'STOCK_KEEPER', 'BRANCH_MANAGER')")
    public ResponseEntity<StockLocationResponse> getStockLocationByCode(
            @PathVariable String locationCode
    ) {
        StockLocationResponse response =
                stockLocationService.getStockLocationByCode(
                        locationCode
                );

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{locationCode}")
    @PreAuthorize("hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER')")
    public ResponseEntity<StockLocationResponse> updateStockLocation(
            @PathVariable String locationCode,
            @Valid @RequestBody StockLocationUpdateRequest request
    ) {
        StockLocationResponse response =
                stockLocationService.updateStockLocation(
                        locationCode,
                        request
                );

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{locationCode}/status")
    @PreAuthorize("hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER')")
    public ResponseEntity<StockLocationResponse> updateStockLocationStatus(
            @PathVariable String locationCode,
            @Valid @RequestBody StockLocationStatusUpdateRequest request
    ) {
        StockLocationResponse response =
                stockLocationService.updateStockLocationStatus(
                        locationCode,
                        request.getActive()
                );

        return ResponseEntity.ok(response);
    }
}
