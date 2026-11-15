package com.rasahub.product_inventory_service.stockalert;

import com.rasahub.product_inventory_service.entity.InventoryItem;
import com.rasahub.product_inventory_service.entity.LowStockAlert;
import com.rasahub.product_inventory_service.entity.StockLocation;
import com.rasahub.product_inventory_service.entity.StockReorderLevel;
import com.rasahub.product_inventory_service.enums.AlertSeverity;
import com.rasahub.product_inventory_service.enums.AlertStatus;
import com.rasahub.product_inventory_service.enums.InventoryItemType;
import com.rasahub.product_inventory_service.enums.MeasurementUnit;
import com.rasahub.product_inventory_service.enums.StockLocationType;
import com.rasahub.product_inventory_service.repository.InventoryItemRepository;
import com.rasahub.product_inventory_service.repository.LowStockAlertRepository;
import com.rasahub.product_inventory_service.repository.StockBalanceRepository;
import com.rasahub.product_inventory_service.repository.StockLocationRepository;
import com.rasahub.product_inventory_service.repository.StockReorderLevelRepository;
import com.rasahub.product_inventory_service.security.ProductUserPrincipal;
import com.rasahub.product_inventory_service.service.LowStockAlertEvaluationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
class LowStockAlertTransactionIntegrationTest {

    @Autowired
    private LowStockAlertEvaluationService lowStockAlertService;
    @Autowired
    private LowStockAlertRepository alertRepository;
    @Autowired
    private StockReorderLevelRepository reorderLevelRepository;
    @Autowired
    private StockBalanceRepository stockBalanceRepository;
    @Autowired
    private InventoryItemRepository inventoryItemRepository;
    @Autowired
    private StockLocationRepository locationRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;

    private LowStockAlert alert;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                () -> "jdbc:h2:mem:low_stock_alert_transaction;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("low-stock.reconciliation.initial-delay-ms", () -> "3600000");
        registry.add("low-stock.reconciliation.interval-ms", () -> "3600000");
    }

    @BeforeEach
    void setUp() {
        alertRepository.deleteAll();
        stockBalanceRepository.deleteAll();
        reorderLevelRepository.deleteAll();
        inventoryItemRepository.deleteAll();
        locationRepository.deleteAll();
        StockLocation location = locationRepository.saveAndFlush(
                new StockLocation("Main", "Main Warehouse", StockLocationType.MAIN_WAREHOUSE)
        );
        InventoryItem item = inventoryItemRepository.saveAndFlush(
                new InventoryItem("T001", "Transaction Test Item", null,
                        InventoryItemType.RAW_MATERIAL, MeasurementUnit.KG)
        );
        reorderLevelRepository.saveAndFlush(
                new StockReorderLevel(location, item, BigDecimal.TEN)
        );
        alert = alertRepository.saveAndFlush(
                new LowStockAlert(location, item, BigDecimal.TEN, new BigDecimal("5"), AlertSeverity.LOW_STOCK)
        );
        ProductUserPrincipal principal = new ProductUserPrincipal(91L, "OWNER", null);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void acknowledgementRollsBackWithAnEnclosingTransaction() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        assertThrows(IllegalStateException.class, () -> transaction.execute(status -> {
            lowStockAlertService.acknowledgeAlert(alert.getId(), null, "will roll back");
            throw new IllegalStateException("rollback test");
        }));

        assertEquals(AlertStatus.OPEN, alertRepository.findById(alert.getId()).orElseThrow().getStatus());
    }
}
