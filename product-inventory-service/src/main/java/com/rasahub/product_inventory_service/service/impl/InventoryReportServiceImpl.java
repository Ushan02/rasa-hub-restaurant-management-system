package com.rasahub.product_inventory_service.service.impl;

import com.rasahub.product_inventory_service.dto.report.DailyUsageReportResponse;
import com.rasahub.product_inventory_service.dto.report.LowStockReportResponse;
import com.rasahub.product_inventory_service.dto.report.DailyUsageSummaryResponse;
import com.rasahub.product_inventory_service.dto.report.PageResponse;
import com.rasahub.product_inventory_service.dto.report.StockAdjustmentReportResponse;
import com.rasahub.product_inventory_service.dto.report.StockBalanceReportResponse;
import com.rasahub.product_inventory_service.dto.report.StockCountReportResponse;
import com.rasahub.product_inventory_service.dto.report.LowStockReportRow;
import com.rasahub.product_inventory_service.dto.report.StockMovementReportResponse;
import com.rasahub.product_inventory_service.dto.report.PurchaseRequestReportResponse;
import com.rasahub.product_inventory_service.dto.report.PurchaseOrderReportResponse;
import com.rasahub.product_inventory_service.dto.report.GoodsReceivedReportResponse;
import com.rasahub.product_inventory_service.dto.report.SupplierPurchaseReportResponse;
import com.rasahub.product_inventory_service.dto.report.BranchStockRequestReportResponse;
import com.rasahub.product_inventory_service.dto.report.StockTransferReportResponse;
import com.rasahub.product_inventory_service.dto.report.InventoryDashboardResponse;
import com.rasahub.product_inventory_service.enums.AlertStatus;
import com.rasahub.product_inventory_service.enums.BranchStockRequestStatus;
import com.rasahub.product_inventory_service.enums.DailyRawMaterialUsageStatus;
import com.rasahub.product_inventory_service.enums.GoodsReceiptStatus;
import com.rasahub.product_inventory_service.enums.MeasurementUnit;
import com.rasahub.product_inventory_service.enums.PurchaseOrderStatus;
import com.rasahub.product_inventory_service.enums.PurchaseRequestStatus;
import com.rasahub.product_inventory_service.enums.StockAdjustmentReason;
import com.rasahub.product_inventory_service.enums.StockAdjustmentStatus;
import com.rasahub.product_inventory_service.enums.StockCountStatus;
import com.rasahub.product_inventory_service.enums.StockMovementType;
import com.rasahub.product_inventory_service.enums.StockTransferStatus;
import com.rasahub.product_inventory_service.enums.StockStatus;
import com.rasahub.product_inventory_service.exception.BusinessRuleException;
import com.rasahub.product_inventory_service.repository.DailyRawMaterialUsageItemRepository;
import com.rasahub.product_inventory_service.repository.DailyUsageSummaryProjection;
import com.rasahub.product_inventory_service.repository.StockBalanceRepository;
import com.rasahub.product_inventory_service.repository.StockAdjustmentItemRepository;
import com.rasahub.product_inventory_service.repository.StockCountItemRepository;
import com.rasahub.product_inventory_service.repository.LowStockAlertRepository;
import com.rasahub.product_inventory_service.repository.PurchaseRequestItemRepository;
import com.rasahub.product_inventory_service.repository.PurchaseOrderItemRepository;
import com.rasahub.product_inventory_service.repository.GoodsReceiptItemRepository;
import com.rasahub.product_inventory_service.repository.BranchStockRequestItemRepository;
import com.rasahub.product_inventory_service.repository.StockTransferRepository;
import com.rasahub.product_inventory_service.repository.StockAdjustmentRepository;
import com.rasahub.product_inventory_service.repository.StockCountRepository;
import com.rasahub.product_inventory_service.repository.BranchStockRequestRepository;
import com.rasahub.product_inventory_service.repository.StockReorderLevelRepository;
import com.rasahub.product_inventory_service.repository.StockLocationRepository;
import com.rasahub.product_inventory_service.repository.InventoryItemRepository;
import com.rasahub.product_inventory_service.repository.LocationCountProjection;
import com.rasahub.product_inventory_service.repository.LocationStockStatusProjection;
import com.rasahub.product_inventory_service.entity.StockLocation;
import com.rasahub.product_inventory_service.repository.StockMovementRepository;
import com.rasahub.product_inventory_service.service.InventoryReportService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class InventoryReportServiceImpl implements InventoryReportService {

    private final StockBalanceRepository stockBalanceRepository;
    private final StockMovementRepository stockMovementRepository;
    private final DailyRawMaterialUsageItemRepository usageItemRepository;
    private final InventoryReportAccessService accessService;
    private final StockAdjustmentItemRepository adjustmentItemRepository;
    private final StockCountItemRepository stockCountItemRepository;
    private final LowStockAlertRepository lowStockAlertRepository;
    private final PurchaseRequestItemRepository purchaseRequestItemRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;
    private final GoodsReceiptItemRepository goodsReceiptItemRepository;
    private final BranchStockRequestItemRepository branchStockRequestItemRepository;
    private final StockTransferRepository stockTransferRepository;
    private final StockAdjustmentRepository stockAdjustmentRepository;
    private final StockCountRepository stockCountRepository;
    private final BranchStockRequestRepository branchStockRequestRepository;
    private final StockReorderLevelRepository reorderLevelRepository;
    private final StockLocationRepository stockLocationRepository;
    private final InventoryItemRepository inventoryItemRepository;

    public InventoryReportServiceImpl(
            StockBalanceRepository stockBalanceRepository,
            StockMovementRepository stockMovementRepository,
            DailyRawMaterialUsageItemRepository usageItemRepository,
            InventoryReportAccessService accessService,
            StockAdjustmentItemRepository adjustmentItemRepository,
            StockCountItemRepository stockCountItemRepository,
            LowStockAlertRepository lowStockAlertRepository,
            PurchaseRequestItemRepository purchaseRequestItemRepository,
            PurchaseOrderItemRepository purchaseOrderItemRepository,
            GoodsReceiptItemRepository goodsReceiptItemRepository,
            BranchStockRequestItemRepository branchStockRequestItemRepository,
            StockTransferRepository stockTransferRepository,
            StockAdjustmentRepository stockAdjustmentRepository,
            StockCountRepository stockCountRepository,
            BranchStockRequestRepository branchStockRequestRepository,
            StockReorderLevelRepository reorderLevelRepository,
            StockLocationRepository stockLocationRepository,
            InventoryItemRepository inventoryItemRepository
    ) {
        this.stockBalanceRepository = stockBalanceRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.usageItemRepository = usageItemRepository;
        this.accessService = accessService;
        this.adjustmentItemRepository = adjustmentItemRepository;
        this.stockCountItemRepository = stockCountItemRepository;
        this.lowStockAlertRepository = lowStockAlertRepository;
        this.purchaseRequestItemRepository = purchaseRequestItemRepository;
        this.purchaseOrderItemRepository = purchaseOrderItemRepository;
        this.goodsReceiptItemRepository = goodsReceiptItemRepository;
        this.branchStockRequestItemRepository = branchStockRequestItemRepository;
        this.stockTransferRepository = stockTransferRepository;
        this.stockAdjustmentRepository = stockAdjustmentRepository;
        this.stockCountRepository = stockCountRepository;
        this.branchStockRequestRepository = branchStockRequestRepository;
        this.reorderLevelRepository = reorderLevelRepository;
        this.stockLocationRepository = stockLocationRepository;
        this.inventoryItemRepository = inventoryItemRepository;
    }

    @Override
    public PageResponse<StockBalanceReportResponse> getStockBalances(
            String locationCode,
            Long inventoryItemId,
            int page,
            int size
    ) {
        validateItemId(inventoryItemId);
        Pageable pageable = pageable(page, size);
        String location = accessService.resolveLocation(locationCode);
        Page<StockBalanceReportResponse> result = stockBalanceRepository
                .findStockBalanceReportRows(
                        location,
                        inventoryItemId,
                        pageable
                )
                .map(row -> {
                    MeasurementUnit unit =
                            MeasurementUnit.valueOf(row.getMeasurementUnit());
                    return new StockBalanceReportResponse(
                            row.getLocationCode(),
                            row.getLocationName(),
                            row.getInventoryItemId(),
                            row.getItemCode(),
                            row.getItemName(),
                            unit,
                            row.getCurrentQuantity(),
                            row.getReorderLevel(),
                            StockStatus.fromQuantity(
                                    row.getCurrentQuantity(),
                                    row.getReorderLevel()
                            ),
                            row.getLastStockUpdateAt()
                    );
                });
        return PageResponse.from(result);
    }

    @Override
    public PageResponse<StockMovementReportResponse> getStockMovements(
            String locationCode,
            Long inventoryItemId,
            StockMovementType movementType,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size
    ) {
        validateItemId(inventoryItemId);
        validateDateRange(startDate, endDate);
        Pageable pageable = pageable(page, size);
        String location = accessService.resolveLocation(locationCode);
        return PageResponse.from(stockMovementRepository.findReportRows(
                location,
                inventoryItemId,
                movementType,
                startDate == null ? null : startDate.atStartOfDay(),
                exclusiveEnd(endDate),
                pageable
        ));
    }

    @Override
    public PageResponse<DailyUsageReportResponse> getDailyUsage(
            String locationCode,
            Long inventoryItemId,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size
    ) {
        validateItemId(inventoryItemId);
        validateDateRange(startDate, endDate);
        Pageable pageable = pageable(page, size);
        String location = accessService.resolveLocation(locationCode);
        Page<DailyUsageReportResponse> result =
                usageItemRepository.findReportRows(
                        DailyRawMaterialUsageStatus.POSTED,
                        location,
                        inventoryItemId,
                        startDate,
                        exclusiveEndDate(endDate),
                        pageable
                );
        return PageResponse.from(result);
    }

    @Override
    public List<DailyUsageSummaryResponse> getDailyUsageSummary(
            String locationCode,
            LocalDate startDate,
            LocalDate endDate,
            boolean monthly
    ) {
        if (startDate == null || endDate == null) {
            throw new BusinessRuleException(
                    "Both startDate and endDate are required for usage summary"
            );
        }
        validateDateRange(startDate, endDate);
        String location = accessService.resolveLocation(locationCode);
        List<DailyUsageSummaryProjection> rows = monthly
                ? usageItemRepository.summarizeMonthly(
                        DailyRawMaterialUsageStatus.POSTED,
                        location,
                        startDate,
                        endDate
                )
                : usageItemRepository.summarizeDaily(
                        DailyRawMaterialUsageStatus.POSTED,
                        location,
                        startDate,
                        endDate
                );
        return rows.stream()
                .map(row -> new DailyUsageSummaryResponse(
                        monthly
                                ? YearMonth.of(
                                        row.getPeriodYear(),
                                        row.getPeriodMonth()
                                ).atDay(1)
                                : row.getPeriodStart(),
                        row.getPeriodYear(),
                        row.getPeriodMonth(),
                        row.getInventoryItemId(),
                        row.getItemCode(),
                        row.getItemName(),
                        row.getMeasurementUnit(),
                        row.getQuantityUsed()
                ))
                .toList();
    }

    @Override
    public PageResponse<StockAdjustmentReportResponse> getStockAdjustments(
            String locationCode,
            Long inventoryItemId,
            StockAdjustmentReason reason,
            StockAdjustmentStatus status,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size
    ) {
        validateItemId(inventoryItemId);
        validateDateRange(startDate, endDate);
        String location = accessService.resolveLocation(locationCode);
        return PageResponse.from(adjustmentItemRepository.findReportRows(
                location,
                reason,
                status,
                inventoryItemId,
                startDate == null ? null : startDate.atStartOfDay(),
                exclusiveEnd(endDate),
                pageable(page, size)
        ));
    }

    @Override
    public PageResponse<StockCountReportResponse> getStockCounts(
            String locationCode,
            Long inventoryItemId,
            StockCountStatus status,
            LocalDate startDate,
            LocalDate endDate,
            boolean variancesOnly,
            int page,
            int size
    ) {
        validateItemId(inventoryItemId);
        validateDateRange(startDate, endDate);
        String location = accessService.resolveLocation(locationCode);
        return PageResponse.from(stockCountItemRepository.findReportRows(
                location,
                status,
                inventoryItemId,
                startDate,
                endDate,
                variancesOnly,
                pageable(page, size)
        ));
    }

    @Override
    public PageResponse<LowStockReportResponse> getLowStock(
            String locationCode,
            Long inventoryItemId,
            AlertStatus status,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size
    ) {
        validateItemId(inventoryItemId);
        validateDateRange(startDate, endDate);
        String location = accessService.resolveLocation(locationCode);
        return PageResponse.from(lowStockAlertRepository.findReportRows(
                location,
                inventoryItemId,
                status,
                startDate == null ? null : startDate.atStartOfDay(),
                exclusiveEnd(endDate),
                java.math.BigDecimal.ZERO,
                pageable(page, size)
        ).map(this::toLowStockResponse));
    }

    private LowStockReportResponse toLowStockResponse(LowStockReportRow row) {
        return new LowStockReportResponse(
                row.alertId(),
                row.locationCode(),
                row.locationName(),
                row.inventoryItemId(),
                row.itemCode(),
                row.itemName(),
                row.measurementUnit(),
                row.currentQuantity(),
                row.reorderLevel(),
                StockStatus.fromQuantity(
                        row.currentQuantity(),
                        row.reorderLevel()
                ),
                row.severity(),
                row.alertStatus(),
                row.detectedAt(),
                row.resolvedAt(),
                row.resolutionReason()
        );
    }

    @Override
    public PageResponse<PurchaseRequestReportResponse> getPurchaseRequests(
            String locationCode,
            PurchaseRequestStatus status,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size
    ) {
        validateDateRange(startDate, endDate);
        String location = accessService.resolveLocation(locationCode);
        return PageResponse.from(purchaseRequestItemRepository.findReportRows(
                location,
                status,
                startDate == null ? null : startDate.atStartOfDay(),
                exclusiveEnd(endDate),
                pageable(page, size)
        ));
    }

    @Override
    public PageResponse<PurchaseOrderReportResponse> getPurchaseOrders(
            String locationCode,
            Long supplierId,
            PurchaseOrderStatus status,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size
    ) {
        validateSupplierId(supplierId);
        validateDateRange(startDate, endDate);
        String location = accessService.resolveLocation(locationCode);
        return PageResponse.from(purchaseOrderItemRepository.findReportRows(
                location,
                supplierId,
                status,
                startDate == null ? null : startDate.atStartOfDay(),
                exclusiveEnd(endDate),
                pageable(page, size)
        ));
    }

    @Override
    public PageResponse<GoodsReceivedReportResponse> getGoodsReceived(
            String locationCode,
            Long supplierId,
            GoodsReceiptStatus status,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size
    ) {
        validateSupplierId(supplierId);
        validateDateRange(startDate, endDate);
        String location = accessService.resolveLocation(locationCode);
        return PageResponse.from(goodsReceiptItemRepository.findReportRows(
                location,
                supplierId,
                status,
                startDate,
                endDate,
                pageable(page, size)
        ));
    }

    @Override
    public PageResponse<SupplierPurchaseReportResponse> getSupplierPurchases(
            String locationCode,
            Long supplierId,
            PurchaseOrderStatus status,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size
    ) {
        validateSupplierId(supplierId);
        validateDateRange(startDate, endDate);
        String location = accessService.resolveLocation(locationCode);
        return PageResponse.from(
                purchaseOrderItemRepository.findSupplierPurchaseRows(
                        location,
                        supplierId,
                        status,
                        startDate == null ? null : startDate.atStartOfDay(),
                        exclusiveEnd(endDate),
                        pageable(page, size)
                )
        );
    }

    @Override
    public PageResponse<BranchStockRequestReportResponse> getBranchStockRequests(
            String locationCode,
            BranchStockRequestStatus status,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size
    ) {
        validateDateRange(startDate, endDate);
        String location = accessService.resolveLocation(locationCode);
        return PageResponse.from(
                branchStockRequestItemRepository.findReportRows(
                        location,
                        status,
                        startDate == null ? null : startDate.atStartOfDay(),
                        exclusiveEnd(endDate),
                        pageable(page, size)
                )
        );
    }

    @Override
    public PageResponse<StockTransferReportResponse> getStockTransfers(
            String locationCode,
            StockTransferStatus status,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size
    ) {
        validateDateRange(startDate, endDate);
        String location = accessService.resolveLocation(locationCode);
        return PageResponse.from(stockTransferRepository.findReportRows(
                location,
                status,
                startDate == null ? null : startDate.atStartOfDay(),
                exclusiveEnd(endDate),
                pageable(page, size)
        ));
    }

    @Override
    public InventoryDashboardResponse getDashboard(String locationCode) {
        List<StockLocation> locations =
                accessService.resolveLocations(locationCode);
        LocalDate periodEnd = LocalDate.now();
        LocalDate periodStart = periodEnd.minusDays(29);
        LocalDateTime from = periodStart.atStartOfDay();
        LocalDateTime until = periodEnd.plusDays(1).atStartOfDay();
        long activeItemCount = inventoryItemRepository.countByActiveTrue();

        Map<String, LocationCountProjection> activeAlerts = countsByLocation(
                lowStockAlertRepository.countActiveAlertsGroupedByLocation(
                        List.of(AlertStatus.OPEN, AlertStatus.ACKNOWLEDGED)
                )
        );
        Map<String, LocationCountProjection> pendingAdjustments =
                countsByLocation(stockAdjustmentRepository
                        .countByStatusGroupedByLocation(
                                StockAdjustmentStatus.PENDING_APPROVAL
                        ));
        Map<String, LocationCountProjection> pendingCounts =
                countsByLocation(stockCountRepository
                        .countByStatusGroupedByLocation(
                                StockCountStatus.SUBMITTED
                        ));
        Map<String, LocationCountProjection> pendingRequests =
                countsByLocation(branchStockRequestRepository
                        .countByStatusGroupedByLocation(
                                BranchStockRequestStatus.PENDING
                        ));
        Map<String, LocationCountProjection> completedTransfers =
                countsByLocation(stockTransferRepository
                        .countTransfersByStatusAndReceiptPeriod(
                                StockTransferStatus.RECEIVED,
                                from,
                                until
                        ));
        Map<String, LocationStockStatusProjection> stockStatuses =
                reorderLevelRepository.summarizeConfiguredStockStatus(
                                java.math.BigDecimal.ZERO
                        )
                        .stream()
                        .collect(Collectors.toMap(
                                LocationStockStatusProjection::getLocationCode,
                                Function.identity()
                        ));

        List<InventoryDashboardResponse.LocationDashboardSummary> summaries =
                locations.stream()
                        .map(location -> {
                            String code = location.getLocationCode();
                            LocationStockStatusProjection stockStatus =
                                    stockStatuses.get(code);
                            return new InventoryDashboardResponse
                                    .LocationDashboardSummary(
                                    code,
                                    location.getLocationName(),
                                    activeItemCount,
                                    stockStatus == null
                                            ? 0
                                            : stockStatus.getLowStockCount(),
                                    stockStatus == null
                                            ? 0
                                            : stockStatus.getOutOfStockCount(),
                                    recordCount(activeAlerts, code),
                                    recordCount(pendingAdjustments, code),
                                    recordCount(pendingCounts, code),
                                    recordCount(pendingRequests, code),
                                    recordCount(completedTransfers, code)
                            );
                        })
                        .toList();
        return new InventoryDashboardResponse(periodStart, periodEnd, summaries);
    }

    private Map<String, LocationCountProjection> countsByLocation(
            List<LocationCountProjection> rows
    ) {
        return rows.stream().collect(Collectors.toMap(
                LocationCountProjection::getLocationCode,
                Function.identity()
        ));
    }

    private long recordCount(
            Map<String, LocationCountProjection> counts,
            String locationCode
    ) {
        LocationCountProjection count = counts.get(locationCode);
        return count == null ? 0 : count.getRecordCount();
    }

    private void validateSupplierId(Long supplierId) {
        if (supplierId != null && supplierId < 1) {
            throw new BusinessRuleException("Supplier ID must be positive");
        }
    }

    private Pageable pageable(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BusinessRuleException(
                    "Page must be non-negative and size must be between 1 and 100"
            );
        }
        return PageRequest.of(page, size);
    }

    private void validateItemId(Long inventoryItemId) {
        if (inventoryItemId != null && inventoryItemId < 1) {
            throw new BusinessRuleException(
                    "Inventory item ID must be positive"
            );
        }
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null
                && endDate.isBefore(startDate)) {
            throw new BusinessRuleException(
                    "endDate must not be before startDate"
            );
        }
        if (endDate != null && endDate.equals(LocalDate.MAX)) {
            throw new BusinessRuleException(
                    "endDate is outside the supported range"
            );
        }
    }

    private LocalDateTime exclusiveEnd(LocalDate endDate) {
        return endDate == null ? null : endDate.plusDays(1).atStartOfDay();
    }

    private LocalDate exclusiveEndDate(LocalDate endDate) {
        if (endDate == null) {
            return null;
        }
        try {
            return endDate.plusDays(1);
        } catch (DateTimeException exception) {
            throw new BusinessRuleException(
                    "endDate is outside the supported range"
            );
        }
    }
}
