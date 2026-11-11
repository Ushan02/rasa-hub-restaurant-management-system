
package com.rasahub.product_inventory_service.controller;

import com.rasahub.product_inventory_service.dto.stockbalance.StockBalanceMinimumLevelUpdateRequest;
import com.rasahub.product_inventory_service.dto.stockbalance.StockBalanceResponse;
import com.rasahub.product_inventory_service.service.StockBalanceService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stock-balances")
public class StockBalanceController {

    private final StockBalanceService stockBalanceService;

    public StockBalanceController(
            StockBalanceService stockBalanceService
    ) {
        this.stockBalanceService = stockBalanceService;
    }

    @GetMapping("/location/{locationCode}")
    @PreAuthorize(
            "hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', " +
                    "'ACCOUNTANT', 'STOCK_KEEPER')"
    )
    public ResponseEntity<List<StockBalanceResponse>>
    getBalancesByLocation(
            @PathVariable String locationCode
    ) {
        return ResponseEntity.ok(
                stockBalanceService.getBalancesByLocation(
                        locationCode
                )
        );
    }

    @GetMapping("/item/{inventoryItemId}")
    @PreAuthorize(
            "hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', " +
                    "'ACCOUNTANT', 'STOCK_KEEPER')"
    )
    public ResponseEntity<List<StockBalanceResponse>>
    getBalancesByInventoryItem(
            @PathVariable Long inventoryItemId
    ) {
        return ResponseEntity.ok(
                stockBalanceService.getBalancesByInventoryItem(
                        inventoryItemId
                )
        );
    }

    @GetMapping(
            "/location/{locationCode}/item/{inventoryItemId}"
    )
    @PreAuthorize(
            "hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', " +
                    "'ACCOUNTANT', 'STOCK_KEEPER')"
    )
    public ResponseEntity<StockBalanceResponse> getBalance(
            @PathVariable String locationCode,
            @PathVariable Long inventoryItemId
    ) {
        return ResponseEntity.ok(
                stockBalanceService.getBalance(
                        locationCode,
                        inventoryItemId
                )
        );
    }

    @GetMapping("/location/{locationCode}/low-stock")
    @PreAuthorize(
            "hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', " +
                    "'ACCOUNTANT', 'STOCK_KEEPER')"
    )
    public ResponseEntity<List<StockBalanceResponse>>
    getLowStockBalances(
            @PathVariable String locationCode
    ) {
        return ResponseEntity.ok(
                stockBalanceService.getLowStockBalances(
                        locationCode
                )
        );
    }

    @PatchMapping(
            "/location/{locationCode}/item/{inventoryItemId}/minimum-level"
    )
    @PreAuthorize(
            "hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', " +
                    "'STOCK_KEEPER')"
    )
    public ResponseEntity<StockBalanceResponse>
    updateMinimumStockLevel(
            @PathVariable String locationCode,
            @PathVariable Long inventoryItemId,
            @Valid @RequestBody
            StockBalanceMinimumLevelUpdateRequest request
    ) {
        return ResponseEntity.ok(
                stockBalanceService.updateMinimumStockLevel(
                        locationCode,
                        inventoryItemId,
                        request
                )
        );
    }
}
