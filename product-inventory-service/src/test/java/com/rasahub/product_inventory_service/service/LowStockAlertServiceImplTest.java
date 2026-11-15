package com.rasahub.product_inventory_service.service;

import com.rasahub.product_inventory_service.entity.InventoryItem;
import com.rasahub.product_inventory_service.entity.LowStockAlert;
import com.rasahub.product_inventory_service.entity.StockBalance;
import com.rasahub.product_inventory_service.entity.StockLocation;
import com.rasahub.product_inventory_service.entity.StockReorderLevel;
import com.rasahub.product_inventory_service.enums.AlertSeverity;
import com.rasahub.product_inventory_service.enums.AlertStatus;
import com.rasahub.product_inventory_service.enums.InventoryItemType;
import com.rasahub.product_inventory_service.enums.MeasurementUnit;
import com.rasahub.product_inventory_service.enums.StockLocationType;
import com.rasahub.product_inventory_service.enums.StockStatus;
import com.rasahub.product_inventory_service.repository.InventoryItemRepository;
import com.rasahub.product_inventory_service.repository.LowStockAlertRepository;
import com.rasahub.product_inventory_service.repository.StockBalanceRepository;
import com.rasahub.product_inventory_service.repository.StockLocationRepository;
import com.rasahub.product_inventory_service.repository.StockReorderLevelRepository;
import com.rasahub.product_inventory_service.service.impl.LowStockAlertServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LowStockAlertServiceImplTest {

    @Mock
    private StockReorderLevelRepository stockReorderLevelRepository;

    @Mock
    private LowStockAlertRepository lowStockAlertRepository;

    @Mock
    private StockBalanceRepository stockBalanceRepository;

    @Mock
    private StockLocationRepository stockLocationRepository;

    @Mock
    private InventoryItemRepository inventoryItemRepository;

    @InjectMocks
    private LowStockAlertServiceImpl lowStockAlertService;

    private StockLocation location;
    private InventoryItem item;
    private StockReorderLevel config;

    @BeforeEach
    void setUp() {
        location = new StockLocation("Main", "Main Warehouse", StockLocationType.MAIN_WAREHOUSE);
        item = new InventoryItem("R001", "Rice", "Rice bag", InventoryItemType.RAW_MATERIAL, MeasurementUnit.KG);
        config = new StockReorderLevel(location, item, new BigDecimal("10"));
    }

    @Test
    void stockStatusUsesThresholdComparisonWithoutFloatingPoint() {
        assertEquals(StockStatus.NORMAL, StockStatus.fromQuantity(new BigDecimal("15"), new BigDecimal("10")));
        assertEquals(StockStatus.LOW_STOCK, StockStatus.fromQuantity(new BigDecimal("10"), new BigDecimal("10")));
        assertEquals(StockStatus.LOW_STOCK, StockStatus.fromQuantity(new BigDecimal("5"), new BigDecimal("10")));
        assertEquals(StockStatus.OUT_OF_STOCK, StockStatus.fromQuantity(BigDecimal.ZERO, new BigDecimal("10")));
    }

    @Test
    void evaluateItemCreatesLowStockAlertWhenBalanceFallsBelowThreshold() {
        StockBalance stockBalance = new StockBalance(location, item);
        stockBalance.increaseQuantity(new BigDecimal("7"));

        when(stockLocationRepository.existsById("Main")).thenReturn(true);
        when(inventoryItemRepository.existsById(1L)).thenReturn(true);
        when(stockReorderLevelRepository.findForUpdate(eq("Main"), eq(1L))).thenReturn(Optional.of(config));
        when(stockBalanceRepository.findByStockLocation_LocationCodeAndInventoryItem_Id("Main", 1L))
                .thenReturn(Optional.of(stockBalance));
        when(lowStockAlertRepository.findActiveForUpdate(eq("Main"), eq(1L), any()))
                .thenReturn(Collections.emptyList());
        when(lowStockAlertRepository.saveAndFlush(any(LowStockAlert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LowStockAlert alert = lowStockAlertService.evaluateItem("Main", 1L);

        assertNotNull(alert);
        assertEquals(AlertStatus.OPEN, alert.getStatus());
        assertEquals(AlertSeverity.LOW_STOCK, alert.getSeverity());
    }

    @Test
    void evaluateItemResolvesExistingAlertWhenStockReturnsToNormal() {
        StockBalance stockBalance = new StockBalance(location, item);
        stockBalance.increaseQuantity(new BigDecimal("15"));

        LowStockAlert openAlert = new LowStockAlert(location, item, new BigDecimal("10"), new BigDecimal("7"), AlertSeverity.LOW_STOCK);

        when(stockLocationRepository.existsById("Main")).thenReturn(true);
        when(inventoryItemRepository.existsById(1L)).thenReturn(true);
        when(stockReorderLevelRepository.findForUpdate(eq("Main"), eq(1L))).thenReturn(Optional.of(config));
        when(stockBalanceRepository.findByStockLocation_LocationCodeAndInventoryItem_Id("Main", 1L))
                .thenReturn(Optional.of(stockBalance));
        when(lowStockAlertRepository.findActiveForUpdate(eq("Main"), eq(1L), any()))
                .thenReturn(Collections.singletonList(openAlert));
        when(lowStockAlertRepository.saveAllAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        LowStockAlert result = lowStockAlertService.evaluateItem("Main", 1L);

        assertNotNull(result);
        assertEquals(AlertStatus.RESOLVED, result.getStatus());
    }
}
