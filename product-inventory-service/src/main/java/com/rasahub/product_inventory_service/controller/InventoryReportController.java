package com.rasahub.product_inventory_service.controller;

import com.rasahub.product_inventory_service.dto.report.DailyUsageReportResponse;
import com.rasahub.product_inventory_service.dto.report.DailyUsageSummaryResponse;
import com.rasahub.product_inventory_service.dto.report.PageResponse;
import com.rasahub.product_inventory_service.dto.report.StockAdjustmentReportResponse;
import com.rasahub.product_inventory_service.dto.report.StockBalanceReportResponse;
import com.rasahub.product_inventory_service.dto.report.StockCountReportResponse;
import com.rasahub.product_inventory_service.dto.report.LowStockReportResponse;
import com.rasahub.product_inventory_service.dto.report.PurchaseRequestReportResponse;
import com.rasahub.product_inventory_service.dto.report.PurchaseOrderReportResponse;
import com.rasahub.product_inventory_service.dto.report.GoodsReceivedReportResponse;
import com.rasahub.product_inventory_service.dto.report.SupplierPurchaseReportResponse;
import com.rasahub.product_inventory_service.dto.report.BranchStockRequestReportResponse;
import com.rasahub.product_inventory_service.dto.report.StockTransferReportResponse;
import com.rasahub.product_inventory_service.dto.report.InventoryDashboardResponse;
import com.rasahub.product_inventory_service.enums.AlertStatus;
import com.rasahub.product_inventory_service.enums.BranchStockRequestStatus;
import com.rasahub.product_inventory_service.enums.GoodsReceiptStatus;
import com.rasahub.product_inventory_service.enums.PurchaseOrderStatus;
import com.rasahub.product_inventory_service.enums.PurchaseRequestStatus;
import com.rasahub.product_inventory_service.enums.StockAdjustmentReason;
import com.rasahub.product_inventory_service.enums.StockAdjustmentStatus;
import com.rasahub.product_inventory_service.enums.StockCountStatus;
import com.rasahub.product_inventory_service.enums.StockTransferStatus;
import com.rasahub.product_inventory_service.dto.report.StockMovementReportResponse;
import com.rasahub.product_inventory_service.enums.StockMovementType;
import com.rasahub.product_inventory_service.service.InventoryReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory-reports")
@PreAuthorize("hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', 'STOCK_KEEPER', 'ACCOUNTANT', 'BRANCH_MANAGER')")
public class InventoryReportController {

    private final InventoryReportService reportService;

    public InventoryReportController(InventoryReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/stock-balances")
    public PageResponse<StockBalanceReportResponse> getStockBalances(
            @RequestParam(required = false) String locationCode,
            @RequestParam(required = false) Long inventoryItemId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return reportService.getStockBalances(
                locationCode,
                inventoryItemId,
                page,
                size
        );
    }

    @GetMapping("/stock-movements")
    public PageResponse<StockMovementReportResponse> getStockMovements(
            @RequestParam(required = false) String locationCode,
            @RequestParam(required = false) Long inventoryItemId,
            @RequestParam(required = false) StockMovementType movementType,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return reportService.getStockMovements(
                locationCode,
                inventoryItemId,
                movementType,
                startDate,
                endDate,
                page,
                size
        );
    }

    @GetMapping("/daily-usage")
    public PageResponse<DailyUsageReportResponse> getDailyUsage(
            @RequestParam(required = false) String locationCode,
            @RequestParam(required = false) Long inventoryItemId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return reportService.getDailyUsage(
                locationCode,
                inventoryItemId,
                startDate,
                endDate,
                page,
                size
        );
    }

    @GetMapping("/daily-usage/summary")
    public List<DailyUsageSummaryResponse> getDailyUsageSummary(
            @RequestParam(required = false) String locationCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,
            @RequestParam(defaultValue = "false") boolean monthly
    ) {
        return reportService.getDailyUsageSummary(
                locationCode,
                startDate,
                endDate,
                monthly
        );
    }

    @GetMapping("/stock-adjustments")
    public PageResponse<StockAdjustmentReportResponse> getStockAdjustments(
            @RequestParam(required = false) String locationCode,
            @RequestParam(required = false) Long inventoryItemId,
            @RequestParam(required = false) StockAdjustmentReason reason,
            @RequestParam(required = false) StockAdjustmentStatus status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return reportService.getStockAdjustments(
                locationCode,
                inventoryItemId,
                reason,
                status,
                startDate,
                endDate,
                page,
                size
        );
    }

    @GetMapping("/stock-counts")
    public PageResponse<StockCountReportResponse> getStockCounts(
            @RequestParam(required = false) String locationCode,
            @RequestParam(required = false) Long inventoryItemId,
            @RequestParam(required = false) StockCountStatus status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "false") boolean variancesOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return reportService.getStockCounts(
                locationCode,
                inventoryItemId,
                status,
                startDate,
                endDate,
                variancesOnly,
                page,
                size
        );
    }

    @GetMapping("/stock-variances")
    public PageResponse<StockCountReportResponse> getStockVariances(
            @RequestParam(required = false) String locationCode,
            @RequestParam(required = false) Long inventoryItemId,
            @RequestParam(required = false) StockCountStatus status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return reportService.getStockCounts(
                locationCode,
                inventoryItemId,
                status,
                startDate,
                endDate,
                true,
                page,
                size
        );
    }

    @GetMapping("/low-stock")
    public PageResponse<LowStockReportResponse> getLowStock(
            @RequestParam(required = false) String locationCode,
            @RequestParam(required = false) Long inventoryItemId,
            @RequestParam(required = false) AlertStatus status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return reportService.getLowStock(
                locationCode,
                inventoryItemId,
                status,
                startDate,
                endDate,
                page,
                size
        );
    }

    @GetMapping("/purchase-requests")
    public PageResponse<PurchaseRequestReportResponse> getPurchaseRequests(
            @RequestParam(required = false) String locationCode,
            @RequestParam(required = false) PurchaseRequestStatus status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return reportService.getPurchaseRequests(
                locationCode, status, startDate, endDate, page, size
        );
    }

    @GetMapping("/purchase-orders")
    public PageResponse<PurchaseOrderReportResponse> getPurchaseOrders(
            @RequestParam(required = false) String locationCode,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) PurchaseOrderStatus status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return reportService.getPurchaseOrders(
                locationCode, supplierId, status, startDate, endDate, page, size
        );
    }

    @GetMapping("/goods-received")
    public PageResponse<GoodsReceivedReportResponse> getGoodsReceived(
            @RequestParam(required = false) String locationCode,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) GoodsReceiptStatus status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return reportService.getGoodsReceived(
                locationCode, supplierId, status, startDate, endDate, page, size
        );
    }

    @GetMapping("/supplier-purchases")
    public PageResponse<SupplierPurchaseReportResponse> getSupplierPurchases(
            @RequestParam(required = false) String locationCode,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) PurchaseOrderStatus status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return reportService.getSupplierPurchases(
                locationCode, supplierId, status, startDate, endDate, page, size
        );
    }

    @GetMapping("/branch-stock-requests")
    public PageResponse<BranchStockRequestReportResponse>
    getBranchStockRequests(
            @RequestParam(required = false) String locationCode,
            @RequestParam(required = false) BranchStockRequestStatus status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return reportService.getBranchStockRequests(
                locationCode, status, startDate, endDate, page, size
        );
    }

    @GetMapping("/stock-transfers")
    public PageResponse<StockTransferReportResponse> getStockTransfers(
            @RequestParam(required = false) String locationCode,
            @RequestParam(required = false) StockTransferStatus status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return reportService.getStockTransfers(
                locationCode, status, startDate, endDate, page, size
        );
    }

    @GetMapping("/dashboard")
    public InventoryDashboardResponse getDashboard(
            @RequestParam(required = false) String locationCode
    ) {
        return reportService.getDashboard(locationCode);
    }
}
