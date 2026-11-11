
package com.rasahub.product_inventory_service.controller;

import com.rasahub.product_inventory_service.dto.stockmovement.StockMovementResponse;
import com.rasahub.product_inventory_service.enums.StockMovementType;
import com.rasahub.product_inventory_service.service.StockMovementService;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stock-movements")
@PreAuthorize(
        "hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', " +
                "'ACCOUNTANT', 'STOCK_KEEPER')"
)
public class StockMovementController {

    private final StockMovementService stockMovementService;

    public StockMovementController(
            StockMovementService stockMovementService
    ) {
        this.stockMovementService = stockMovementService;
    }

    @GetMapping("/location/{locationCode}")
    public ResponseEntity<Page<StockMovementResponse>>
    getMovementsByLocation(
            @PathVariable String locationCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                stockMovementService.getMovementsByLocation(
                        locationCode,
                        page,
                        size
                )
        );
    }

    @GetMapping("/item/{inventoryItemId}")
    public ResponseEntity<Page<StockMovementResponse>>
    getMovementsByInventoryItem(
            @PathVariable Long inventoryItemId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                stockMovementService.getMovementsByInventoryItem(
                        inventoryItemId,
                        page,
                        size
                )
        );
    }

    @GetMapping(
            "/location/{locationCode}/item/{inventoryItemId}"
    )
    public ResponseEntity<Page<StockMovementResponse>>
    getMovementsByLocationAndItem(
            @PathVariable String locationCode,
            @PathVariable Long inventoryItemId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                stockMovementService.getMovementsByLocationAndItem(
                        locationCode,
                        inventoryItemId,
                        page,
                        size
                )
        );
    }

    @GetMapping("/reference/{referenceType}/{referenceId}")
    public ResponseEntity<List<StockMovementResponse>>
    getMovementsByReference(
            @PathVariable String referenceType,
            @PathVariable String referenceId
    ) {
        return ResponseEntity.ok(
                stockMovementService.getMovementsByReference(
                        referenceType,
                        referenceId
                )
        );
    }

    @GetMapping("/type/{movementType}")
    public ResponseEntity<Page<StockMovementResponse>>
    getMovementsByType(
            @PathVariable StockMovementType movementType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                stockMovementService.getMovementsByType(
                        movementType,
                        page,
                        size
                )
        );
    }
}
