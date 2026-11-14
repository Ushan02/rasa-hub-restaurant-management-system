
package com.rasahub.product_inventory_service.controller;

import com.rasahub.product_inventory_service.dto.stocktransfer.StockTransferCancelRequest;
import com.rasahub.product_inventory_service.dto.stocktransfer.StockTransferCreateRequest;
import com.rasahub.product_inventory_service.dto.stocktransfer.StockTransferResponse;
import com.rasahub.product_inventory_service.enums.StockTransferStatus;
import com.rasahub.product_inventory_service.service.StockTransferService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/stock-transfers")
public class StockTransferController {

    private final StockTransferService service;

    public StockTransferController(StockTransferService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasRole('STOCK_KEEPER')")
    public ResponseEntity<StockTransferResponse> create(
            @Valid @RequestBody StockTransferCreateRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.createTransfer(request));
    }

    @GetMapping
    @PreAuthorize(
            "hasAnyRole('OWNER','MAIN_OFFICE_MANAGER','STOCK_KEEPER','ACCOUNTANT')"
    )
    public ResponseEntity<Page<StockTransferResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                service.getAllTransfers(page, size)
        );
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('BRANCH_MANAGER')")
    public ResponseEntity<Page<StockTransferResponse>> getMyBranchTransfers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                service.getMyBranchTransfers(page, size)
        );
    }

    @GetMapping("/status/{status}")
    @PreAuthorize(
            "hasAnyRole('OWNER','MAIN_OFFICE_MANAGER','STOCK_KEEPER','ACCOUNTANT','BRANCH_MANAGER')"
    )
    public ResponseEntity<Page<StockTransferResponse>> getByStatus(
            @PathVariable StockTransferStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                service.getTransfersByStatus(status, page, size)
        );
    }

    @GetMapping("/branch/{branchCode}")
    @PreAuthorize(
            "hasAnyRole('OWNER','MAIN_OFFICE_MANAGER','STOCK_KEEPER','ACCOUNTANT','BRANCH_MANAGER')"
    )
    public ResponseEntity<Page<StockTransferResponse>> getByBranch(
            @PathVariable String branchCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                service.getTransfersByBranch(branchCode, page, size)
        );
    }

    @GetMapping("/branch/{branchCode}/status/{status}")
    @PreAuthorize(
            "hasAnyRole('OWNER','MAIN_OFFICE_MANAGER','STOCK_KEEPER','ACCOUNTANT','BRANCH_MANAGER')"
    )
    public ResponseEntity<Page<StockTransferResponse>> getByBranchAndStatus(
            @PathVariable String branchCode,
            @PathVariable StockTransferStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                service.getTransfersByBranchAndStatus(
                        branchCode,
                        status,
                        page,
                        size
                )
        );
    }

    @GetMapping("/{transferId}")
    @PreAuthorize(
            "hasAnyRole('OWNER','MAIN_OFFICE_MANAGER','STOCK_KEEPER','ACCOUNTANT','BRANCH_MANAGER')"
    )
    public ResponseEntity<StockTransferResponse> getById(
            @PathVariable Long transferId
    ) {
        return ResponseEntity.ok(
                service.getTransferById(transferId)
        );
    }

    @PatchMapping("/{transferId}/dispatch")
    @PreAuthorize("hasRole('STOCK_KEEPER')")
    public ResponseEntity<StockTransferResponse> dispatch(
            @PathVariable Long transferId
    ) {
        return ResponseEntity.ok(
                service.dispatchTransfer(transferId)
        );
    }

    @PatchMapping("/{transferId}/receive")
    @PreAuthorize("hasRole('BRANCH_MANAGER')")
    public ResponseEntity<StockTransferResponse> confirmReceipt(
            @PathVariable Long transferId
    ) {
        return ResponseEntity.ok(
                service.confirmReceipt(transferId)
        );
    }

    @PatchMapping("/{transferId}/cancel")
    @PreAuthorize(
            "hasAnyRole('STOCK_KEEPER','MAIN_OFFICE_MANAGER','OWNER')"
    )
    public ResponseEntity<StockTransferResponse> cancel(
            @PathVariable Long transferId,
            @Valid @RequestBody StockTransferCancelRequest request
    ) {
        return ResponseEntity.ok(
                service.cancelTransfer(transferId, request)
        );
    }
}
