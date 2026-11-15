package com.rasahub.product_inventory_service.report;

import com.rasahub.product_inventory_service.dto.report.PageResponse;
import com.rasahub.product_inventory_service.dto.report.StockBalanceReportResponse;
import com.rasahub.product_inventory_service.dto.report.StockMovementReportResponse;
import com.rasahub.product_inventory_service.dto.report.DailyUsageReportResponse;
import com.rasahub.product_inventory_service.dto.report.StockAdjustmentReportResponse;
import com.rasahub.product_inventory_service.dto.report.StockCountReportResponse;
import com.rasahub.product_inventory_service.dto.report.LowStockReportResponse;
import com.rasahub.product_inventory_service.entity.LowStockAlert;
import com.rasahub.product_inventory_service.entity.StockAdjustment;
import com.rasahub.product_inventory_service.entity.StockCount;
import com.rasahub.product_inventory_service.entity.DailyRawMaterialUsage;
import com.rasahub.product_inventory_service.entity.InventoryItem;
import com.rasahub.product_inventory_service.entity.StockBalance;
import com.rasahub.product_inventory_service.entity.StockLocation;
import com.rasahub.product_inventory_service.entity.StockMovement;
import com.rasahub.product_inventory_service.entity.StockReorderLevel;
import com.rasahub.product_inventory_service.entity.Supplier;
import com.rasahub.product_inventory_service.entity.PurchaseRequest;
import com.rasahub.product_inventory_service.entity.PurchaseOrder;
import com.rasahub.product_inventory_service.entity.GoodsReceiptNote;
import com.rasahub.product_inventory_service.entity.BranchStockRequest;
import com.rasahub.product_inventory_service.entity.StockTransfer;
import com.rasahub.product_inventory_service.enums.DailyRawMaterialUsageStatus;
import com.rasahub.product_inventory_service.enums.AlertResolutionReason;
import com.rasahub.product_inventory_service.enums.AlertSeverity;
import com.rasahub.product_inventory_service.enums.AlertStatus;
import com.rasahub.product_inventory_service.enums.InventoryItemType;
import com.rasahub.product_inventory_service.enums.MeasurementUnit;
import com.rasahub.product_inventory_service.enums.StockAdjustmentDirection;
import com.rasahub.product_inventory_service.enums.StockAdjustmentReason;
import com.rasahub.product_inventory_service.enums.StockAdjustmentStatus;
import com.rasahub.product_inventory_service.enums.StockLocationType;
import com.rasahub.product_inventory_service.enums.StockMovementType;
import com.rasahub.product_inventory_service.enums.StockCountStatus;
import com.rasahub.product_inventory_service.enums.PurchaseOrderStatus;
import com.rasahub.product_inventory_service.enums.PurchaseRequestStatus;
import com.rasahub.product_inventory_service.enums.GoodsReceiptStatus;
import com.rasahub.product_inventory_service.enums.BranchStockRequestStatus;
import com.rasahub.product_inventory_service.enums.StockTransferStatus;
import com.rasahub.product_inventory_service.repository.LowStockAlertRepository;
import com.rasahub.product_inventory_service.repository.StockAdjustmentRepository;
import com.rasahub.product_inventory_service.repository.StockCountRepository;
import com.rasahub.product_inventory_service.repository.DailyRawMaterialUsageRepository;
import com.rasahub.product_inventory_service.repository.InventoryItemRepository;
import com.rasahub.product_inventory_service.repository.StockBalanceRepository;
import com.rasahub.product_inventory_service.repository.StockLocationRepository;
import com.rasahub.product_inventory_service.repository.StockMovementRepository;
import com.rasahub.product_inventory_service.repository.StockReorderLevelRepository;
import com.rasahub.product_inventory_service.repository.SupplierRepository;
import com.rasahub.product_inventory_service.repository.PurchaseRequestRepository;
import com.rasahub.product_inventory_service.repository.PurchaseOrderRepository;
import com.rasahub.product_inventory_service.repository.GoodsReceiptNoteRepository;
import com.rasahub.product_inventory_service.repository.BranchStockRequestRepository;
import com.rasahub.product_inventory_service.repository.StockTransferRepository;
import com.rasahub.product_inventory_service.service.InventoryReportService;
import com.rasahub.product_inventory_service.security.ProductUserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class InventoryReportIntegrationTest {

    @Autowired
    private InventoryReportService reportService;

    @Autowired
    private StockLocationRepository locationRepository;

    @Autowired
    private InventoryItemRepository itemRepository;

    @Autowired
    private StockBalanceRepository balanceRepository;

    @Autowired
    private StockMovementRepository movementRepository;

    @Autowired
    private StockReorderLevelRepository reorderLevelRepository;

    @Autowired
    private DailyRawMaterialUsageRepository usageRepository;

    @Autowired
    private StockAdjustmentRepository adjustmentRepository;

    @Autowired
    private StockCountRepository countRepository;

    @Autowired
    private LowStockAlertRepository alertRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private PurchaseRequestRepository purchaseRequestRepository;

    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;

    @Autowired
    private GoodsReceiptNoteRepository goodsReceiptNoteRepository;

    @Autowired
    private BranchStockRequestRepository branchStockRequestRepository;

    @Autowired
    private StockTransferRepository stockTransferRepository;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void stockBalanceReportUsesCurrentBalanceAndIncludesConfiguredZeroStock() {
        authenticateAsOwner();
        StockLocation main = saveLocation("Main", "Central Warehouse",
                StockLocationType.MAIN_WAREHOUSE);
        InventoryItem stockedItem = saveItem("RPT-001", MeasurementUnit.KG);
        InventoryItem configuredZeroItem = saveItem("RPT-002", MeasurementUnit.PCS);

        StockBalance balance = new StockBalance(main, stockedItem);
        balance.increaseQuantity(new BigDecimal("2.500"));
        balanceRepository.saveAndFlush(balance);
        reorderLevelRepository.saveAndFlush(new StockReorderLevel(
                main,
                stockedItem,
                new BigDecimal("3.000")
        ));
        reorderLevelRepository.saveAndFlush(new StockReorderLevel(
                main,
                configuredZeroItem,
                new BigDecimal("2.000")
        ));

        PageResponse<StockBalanceReportResponse> response =
                reportService.getStockBalances("Main", null, 0, 20);

        assertEquals(2, response.totalElements());
        StockBalanceReportResponse stocked =
                response.content().stream()
                        .filter(row -> row.inventoryItemId().equals(stockedItem.getId()))
                        .findFirst()
                        .orElseThrow();
        StockBalanceReportResponse zero =
                response.content().stream()
                        .filter(row -> row.inventoryItemId().equals(configuredZeroItem.getId()))
                        .findFirst()
                        .orElseThrow();
        assertEquals(new BigDecimal("2.500"), stocked.currentQuantity());
        assertEquals(new BigDecimal("3.000"), stocked.reorderLevel());
        assertEquals("LOW_STOCK", stocked.stockStatus().name());
        assertEquals(BigDecimal.ZERO.setScale(3), zero.currentQuantity());
        assertEquals("OUT_OF_STOCK", zero.stockStatus().name());
        assertNull(zero.lastStockUpdateAt());
        assertEquals(
                new BigDecimal("2.500"),
                balanceRepository
                        .findByStockLocation_LocationCodeAndInventoryItem_Id(
                                "Main",
                                stockedItem.getId()
                        )
                        .orElseThrow()
                        .getQuantityOnHand()
        );
    }

    @Test
    void movementReportFiltersByTypeAndInclusiveDateRange() {
        authenticateAsOwner();
        StockLocation main = saveLocation("Main", "Central Warehouse",
                StockLocationType.MAIN_WAREHOUSE);
        InventoryItem item = saveItem("RPT-003", MeasurementUnit.L);
        movementRepository.saveAndFlush(new StockMovement(
                main,
                item,
                StockMovementType.GOODS_RECEIPT,
                new BigDecimal("4.000"),
                BigDecimal.ZERO,
                new BigDecimal("4.000"),
                "TEST",
                "REPORT-1",
                17L,
                "received"
        ));

        PageResponse<StockMovementReportResponse> response =
                reportService.getStockMovements(
                        "Main",
                        item.getId(),
                        StockMovementType.GOODS_RECEIPT,
                        LocalDate.now(),
                        LocalDate.now(),
                        0,
                        20
                );

        assertEquals(1, response.totalElements());
        assertEquals(new BigDecimal("4.000"), response.content().getFirst().quantity());
        assertEquals(new BigDecimal("0.000"),
                response.content().getFirst().previousBalance());
        assertEquals(new BigDecimal("4.000"), response.content().getFirst().newBalance());
        assertEquals(17L, response.content().getFirst().accountId());
    }

    @Test
    void usageReportAndSummaryCountPostedRowsButNotDraftRows() {
        authenticateAsOwner();
        StockLocation branch = saveLocation("BR001", "Veyangoda",
                StockLocationType.BRANCH);
        InventoryItem item = saveItem("RPT-004", MeasurementUnit.KG);
        DailyRawMaterialUsage posted = createUsage(branch, item, "1.250");
        posted.post(18L);
        usageRepository.saveAndFlush(posted);
        createUsage(branch, item, "8.000");

        PageResponse<DailyUsageReportResponse> report =
                reportService.getDailyUsage(
                        "BR001",
                        item.getId(),
                        LocalDate.now(),
                        LocalDate.now(),
                        0,
                        20
                );

        assertEquals(1, report.totalElements());
        assertEquals(DailyRawMaterialUsageStatus.POSTED,
                report.content().getFirst().status());
        assertEquals(18L, report.content().getFirst().postedByAccountId());
        assertEquals(new BigDecimal("1.250"),
                report.content().getFirst().quantityUsed());

        var dailySummary = reportService.getDailyUsageSummary(
                "BR001",
                LocalDate.now(),
                LocalDate.now(),
                false
        );
        var monthlySummary = reportService.getDailyUsageSummary(
                "BR001",
                LocalDate.now(),
                LocalDate.now(),
                true
        );
        assertEquals(1, dailySummary.size());
        assertEquals(new BigDecimal("1.250"), dailySummary.getFirst().quantityUsed());
        assertEquals(1, monthlySummary.size());
        assertEquals(LocalDate.now().withDayOfMonth(1),
                monthlySummary.getFirst().periodStart());
        assertEquals(dailySummary.getFirst().measurementUnit(),
                monthlySummary.getFirst().measurementUnit());
    }

    @Test
    void adjustmentReportShowsStatusAndDoesNotUseMovementLedgerAsAnotherLine() {
        authenticateAsOwner();
        StockLocation main = saveLocation("Main", "Central Warehouse",
                StockLocationType.MAIN_WAREHOUSE);
        InventoryItem item = saveItem("RPT-005", MeasurementUnit.PCS);
        StockAdjustment adjustment = new StockAdjustment(
                main,
                StockAdjustmentReason.FOUND,
                StockAdjustmentDirection.IN,
                21L,
                "reconciled stock"
        );
        adjustment.addItem(item, new BigDecimal("2"));
        StockAdjustment saved = adjustmentRepository.saveAndFlush(adjustment);
        movementRepository.saveAndFlush(new StockMovement(
                main,
                item,
                StockMovementType.ADJUSTMENT_IN,
                new BigDecimal("2.000"),
                BigDecimal.ZERO,
                new BigDecimal("2.000"),
                "STOCK_ADJUSTMENT",
                saved.getId().toString(),
                23L,
                "posted adjustment ledger entry"
        ));

        PageResponse<StockAdjustmentReportResponse> response =
                reportService.getStockAdjustments(
                        "Main",
                        item.getId(),
                        StockAdjustmentReason.FOUND,
                        StockAdjustmentStatus.DRAFT,
                        LocalDate.now(),
                        LocalDate.now(),
                        0,
                        20
                );

        assertEquals(1, response.totalElements());
        assertEquals(StockAdjustmentStatus.DRAFT,
                response.content().getFirst().status());
        assertEquals(new BigDecimal("2.000"),
                response.content().getFirst().quantity());
    }

    @Test
    void stockCountVarianceUsesHistoricalSystemSnapshotNotCurrentBalance() {
        authenticateAsOwner();
        StockLocation main = saveLocation("Main", "Central Warehouse",
                StockLocationType.MAIN_WAREHOUSE);
        InventoryItem item = saveItem("RPT-006", MeasurementUnit.KG);
        StockCount count = new StockCount(
                main,
                LocalDate.now(),
                22L,
                null
        );
        count.addItem(
                item,
                new BigDecimal("10.000"),
                5L,
                new BigDecimal("7.000"),
                null
        );
        countRepository.saveAndFlush(count);
        StockBalance current = new StockBalance(main, item);
        current.increaseQuantity(new BigDecimal("100.000"));
        balanceRepository.saveAndFlush(current);

        PageResponse<StockCountReportResponse> response =
                reportService.getStockCounts(
                        "Main",
                        item.getId(),
                        StockCountStatus.DRAFT,
                        LocalDate.now(),
                        LocalDate.now(),
                        true,
                        0,
                        20
                );

        assertEquals(1, response.totalElements());
        assertEquals(new BigDecimal("10.000"),
                response.content().getFirst().systemQuantitySnapshot());
        assertEquals(new BigDecimal("7.000"),
                response.content().getFirst().physicalQuantity());
        assertEquals(new BigDecimal("-3.000"),
                response.content().getFirst().varianceQuantity());
    }

    @Test
    void lowStockReportSeparatesCurrentStatusFromHistoricalAlertState() {
        authenticateAsOwner();
        StockLocation branch = saveLocation("BR002", "Gampaha",
                StockLocationType.BRANCH);
        InventoryItem item = saveItem("RPT-007", MeasurementUnit.L);
        StockBalance balance = new StockBalance(branch, item);
        balance.increaseQuantity(new BigDecimal("2.000"));
        balanceRepository.saveAndFlush(balance);
        reorderLevelRepository.saveAndFlush(new StockReorderLevel(
                branch,
                item,
                new BigDecimal("5.000")
        ));
        LowStockAlert alert = new LowStockAlert(
                branch,
                item,
                new BigDecimal("5.000"),
                BigDecimal.ZERO,
                AlertSeverity.OUT_OF_STOCK
        );
        alert.resolveAsRecovered("Recovered before subsequent shortage");
        alertRepository.saveAndFlush(alert);

        PageResponse<LowStockReportResponse> response =
                reportService.getLowStock(
                        "BR002",
                        item.getId(),
                        AlertStatus.RESOLVED,
                        LocalDate.now(),
                        LocalDate.now(),
                        0,
                        20
                );

        assertEquals(1, response.totalElements());
        LowStockReportResponse row = response.content().getFirst();
        assertEquals(AlertStatus.RESOLVED, row.alertStatus());
        assertEquals(AlertResolutionReason.STOCK_RECOVERED,
                row.resolutionReason());
        assertEquals(new BigDecimal("2.000"), row.currentQuantity());
        assertEquals("LOW_STOCK", row.currentStockStatus().name());
    }

    @Test
    void supplierPurchaseReportCountsOnlyAcceptedConfirmedGrnQuantities() {
        authenticateAsOwner();
        StockLocation main = saveLocation("Main", "Central Warehouse",
                StockLocationType.MAIN_WAREHOUSE);
        InventoryItem item = saveItem("RPT-008", MeasurementUnit.KG);
        Supplier supplier = supplierRepository.saveAndFlush(new Supplier(
                "SUP-RPT",
                "Report Supplier",
                "0712345678",
                null,
                null
        ));
        PurchaseRequest request = new PurchaseRequest(main, 31L, null);
        request.addItem(item, new BigDecimal("10.000"));
        purchaseRequestRepository.saveAndFlush(request);
        request.approve(32L);
        purchaseRequestRepository.saveAndFlush(request);

        PurchaseOrder purchaseOrder = new PurchaseOrder(
                request,
                supplier,
                33L,
                LocalDate.now().plusDays(3),
                null
        );
        purchaseOrder.addItem(
                item,
                new BigDecimal("10.000"),
                new BigDecimal("25.00")
        );
        request.markPurchaseOrderCreated();
        purchaseRequestRepository.saveAndFlush(request);
        purchaseOrderRepository.saveAndFlush(purchaseOrder);

        GoodsReceiptNote receipt = new GoodsReceiptNote(
                purchaseOrder,
                34L,
                LocalDate.now(),
                null,
                null
        );
        receipt.addItem(
                item,
                new BigDecimal("5.000"),
                new BigDecimal("3.000"),
                new BigDecimal("2.000")
        );
        goodsReceiptNoteRepository.saveAndFlush(receipt);
        receipt.submit();
        receipt.confirm(35L);
        goodsReceiptNoteRepository.saveAndFlush(receipt);

        var supplierPurchases = reportService.getSupplierPurchases(
                "Main",
                supplier.getId(),
                PurchaseOrderStatus.ISSUED,
                LocalDate.now(),
                LocalDate.now(),
                0,
                20
        );
        var receivedHistory = reportService.getGoodsReceived(
                "Main",
                supplier.getId(),
                GoodsReceiptStatus.CONFIRMED,
                LocalDate.now(),
                LocalDate.now(),
                0,
                20
        );
        var purchaseRequests = reportService.getPurchaseRequests(
                "Main",
                null,
                LocalDate.now(),
                LocalDate.now(),
                0,
                20
        );
        var purchaseOrders = reportService.getPurchaseOrders(
                "Main",
                supplier.getId(),
                PurchaseOrderStatus.ISSUED,
                LocalDate.now(),
                LocalDate.now(),
                0,
                20
        );
        var emptyPurchaseOrders = reportService.getPurchaseOrders(
                "Main",
                supplier.getId() + 1,
                PurchaseOrderStatus.ISSUED,
                LocalDate.now(),
                LocalDate.now(),
                0,
                20
        );

        assertEquals(1, supplierPurchases.totalElements());
        assertEquals(new BigDecimal("10.000"),
                supplierPurchases.content().getFirst().orderedQuantity());
        assertEquals(new BigDecimal("3.000"),
                supplierPurchases.content().getFirst().acceptedReceiptQuantity());
        assertEquals(1, receivedHistory.totalElements());
        assertEquals(new BigDecimal("2.000"),
                receivedHistory.content().getFirst().rejectedQuantity());
        assertEquals(1, purchaseRequests.totalElements());
        assertEquals(item.getItemCode(),
                purchaseRequests.content().getFirst().itemCode());
        assertEquals(1, purchaseOrders.totalElements());
        assertEquals(PurchaseOrderStatus.ISSUED,
                purchaseOrders.content().getFirst().status());
        assertTrue(emptyPurchaseOrders.content().isEmpty());
    }

    @Test
    void transferReportShowsReceiptHistoryAndDashboardUsesPersistedCounts() {
        authenticateAsOwner();
        StockLocation main = saveLocation("Main", "Central Warehouse",
                StockLocationType.MAIN_WAREHOUSE);
        StockLocation branch = saveLocation("BR003", "Negombo",
                StockLocationType.BRANCH);
        InventoryItem item = saveItem("RPT-009", MeasurementUnit.PCS);
        StockReorderLevel reorderLevel = new StockReorderLevel(
                branch,
                item,
                BigDecimal.ONE
        );
        reorderLevelRepository.saveAndFlush(reorderLevel);

        BranchStockRequest request = new BranchStockRequest(
                branch,
                41L,
                null
        );
        request.addItem(item, new BigDecimal("4"));
        branchStockRequestRepository.saveAndFlush(request);
        request.approve(42L);
        branchStockRequestRepository.saveAndFlush(request);

        BranchStockRequest pendingRequest = new BranchStockRequest(
                branch,
                46L,
                null
        );
        pendingRequest.addItem(item, new BigDecimal("3"));
        branchStockRequestRepository.saveAndFlush(pendingRequest);

        StockTransfer transfer = new StockTransfer(
                request,
                main,
                43L,
                null
        );
        transfer.addItem(item, new BigDecimal("2"));
        stockTransferRepository.saveAndFlush(transfer);
        transfer.dispatch(44L);
        stockTransferRepository.saveAndFlush(transfer);
        transfer.confirmReceipt(45L);
        stockTransferRepository.saveAndFlush(transfer);
        request.recordReceivedQuantity(item.getId(), new BigDecimal("2"));
        branchStockRequestRepository.saveAndFlush(request);

        var transferRows = reportService.getStockTransfers(
                "BR003",
                StockTransferStatus.RECEIVED,
                LocalDate.now(),
                LocalDate.now(),
                0,
                20
        );
        var branchRequests = reportService.getBranchStockRequests(
                "BR003",
                null,
                LocalDate.now(),
                LocalDate.now(),
                0,
                20
        );
        var dashboard = reportService.getDashboard("BR003");

        assertEquals(1, transferRows.totalElements());
        assertEquals("Main", transferRows.content().getFirst().sourceLocationCode());
        assertEquals("BR003",
                transferRows.content().getFirst().destinationLocationCode());
        assertEquals(45L, transferRows.content().getFirst().receivedByAccountId());
        assertEquals(2, branchRequests.totalElements());
        assertEquals(new BigDecimal("2.000"),
                branchRequests.content().stream()
                        .filter(row -> row.requestedByAccountId().equals(41L))
                        .findFirst()
                        .orElseThrow()
                        .receivedQuantity());
        assertEquals(1, dashboard.locations().size());
        assertEquals(1, dashboard.locations().getFirst()
                .pendingBranchStockRequestCount());
        assertEquals(1, dashboard.locations().getFirst()
                .completedTransferCount());
        assertEquals(1, dashboard.locations().getFirst().outOfStockItemCount());
    }

    private DailyRawMaterialUsage createUsage(
            StockLocation branch,
            InventoryItem item,
            String quantity
    ) {
        DailyRawMaterialUsage usage = new DailyRawMaterialUsage(
                branch,
                LocalDate.now(),
                16L,
                null
        );
        usage.addItem(item, new BigDecimal(quantity));
        DailyRawMaterialUsage saved = usageRepository.saveAndFlush(usage);
        return usageRepository.findWithItemsById(saved.getId()).orElseThrow();
    }

    private StockLocation saveLocation(
            String code,
            String name,
            StockLocationType type
    ) {
        return locationRepository.saveAndFlush(
                new StockLocation(code, name, type)
        );
    }

    private InventoryItem saveItem(String code, MeasurementUnit unit) {
        return itemRepository.saveAndFlush(new InventoryItem(
                code,
                "Report item " + code,
                null,
                InventoryItemType.RAW_MATERIAL,
                unit
        ));
    }

    private void authenticateAsOwner() {
        ProductUserPrincipal principal =
                new ProductUserPrincipal(12L, "OWNER", null);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.getAuthorities()
                )
        );
    }
}
