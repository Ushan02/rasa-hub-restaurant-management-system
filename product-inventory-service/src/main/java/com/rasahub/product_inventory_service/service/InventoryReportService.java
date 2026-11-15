package com.rasahub.product_inventory_service.service;

import com.rasahub.product_inventory_service.dto.report.DailyUsageReportResponse;
import com.rasahub.product_inventory_service.dto.report.DailyUsageSummaryResponse;
import com.rasahub.product_inventory_service.dto.report.PageResponse;
import com.rasahub.product_inventory_service.dto.report.StockBalanceReportResponse;
import com.rasahub.product_inventory_service.dto.report.StockAdjustmentReportResponse;
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
import com.rasahub.product_inventory_service.enums.PurchaseRequestStatus;
import com.rasahub.product_inventory_service.enums.PurchaseOrderStatus;
import com.rasahub.product_inventory_service.enums.GoodsReceiptStatus;
import com.rasahub.product_inventory_service.enums.BranchStockRequestStatus;
import com.rasahub.product_inventory_service.enums.StockTransferStatus;
import com.rasahub.product_inventory_service.enums.StockAdjustmentReason;
import com.rasahub.product_inventory_service.enums.StockAdjustmentStatus;
import com.rasahub.product_inventory_service.enums.StockCountStatus;
import com.rasahub.product_inventory_service.dto.report.StockMovementReportResponse;
import com.rasahub.product_inventory_service.enums.StockMovementType;

import java.time.LocalDate;
import java.util.List;

public interface InventoryReportService {
    PageResponse<StockBalanceReportResponse> getStockBalances(
            String locationCode,
            Long inventoryItemId,
            int page,
            int size
    );

    PageResponse<StockMovementReportResponse> getStockMovements(
            String locationCode,
            Long inventoryItemId,
            StockMovementType movementType,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size
    );

    PageResponse<DailyUsageReportResponse> getDailyUsage(
            String locationCode,
            Long inventoryItemId,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size
    );

    List<DailyUsageSummaryResponse> getDailyUsageSummary(
            String locationCode,
            LocalDate startDate,
            LocalDate endDate,
            boolean monthly
    );

    PageResponse<StockAdjustmentReportResponse> getStockAdjustments(
            String locationCode,
            Long inventoryItemId,
            StockAdjustmentReason reason,
            StockAdjustmentStatus status,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size
    );

    PageResponse<StockCountReportResponse> getStockCounts(
            String locationCode,
            Long inventoryItemId,
            StockCountStatus status,
            LocalDate startDate,
            LocalDate endDate,
            boolean variancesOnly,
            int page,
            int size
    );

    PageResponse<LowStockReportResponse> getLowStock(
            String locationCode,
            Long inventoryItemId,
            AlertStatus status,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size
    );

    PageResponse<PurchaseRequestReportResponse> getPurchaseRequests(
            String locationCode,
            PurchaseRequestStatus status,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size
    );

    PageResponse<PurchaseOrderReportResponse> getPurchaseOrders(
            String locationCode,
            Long supplierId,
            PurchaseOrderStatus status,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size
    );

    PageResponse<GoodsReceivedReportResponse> getGoodsReceived(
            String locationCode,
            Long supplierId,
            GoodsReceiptStatus status,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size
    );

    PageResponse<SupplierPurchaseReportResponse> getSupplierPurchases(
            String locationCode,
            Long supplierId,
            PurchaseOrderStatus status,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size
    );

    PageResponse<BranchStockRequestReportResponse> getBranchStockRequests(
            String locationCode,
            BranchStockRequestStatus status,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size
    );

    PageResponse<StockTransferReportResponse> getStockTransfers(
            String locationCode,
            StockTransferStatus status,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size
    );

    InventoryDashboardResponse getDashboard(String locationCode);
}
