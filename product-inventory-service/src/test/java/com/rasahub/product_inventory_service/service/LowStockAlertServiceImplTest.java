package com.rasahub.product_inventory_service.service;

import com.rasahub.product_inventory_service.entity.InventoryItem;
import com.rasahub.product_inventory_service.entity.LowStockAlert;
import com.rasahub.product_inventory_service.entity.StockBalance;
import com.rasahub.product_inventory_service.entity.StockLocation;
import com.rasahub.product_inventory_service.entity.StockReorderLevel;
import com.rasahub.product_inventory_service.dto.lowstock.LowStockAlertResponse;
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
import com.rasahub.product_inventory_service.security.ProductUserPrincipal;
import com.rasahub.product_inventory_service.exception.ConflictException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

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
        ReflectionTestUtils.setField(item, "id", 1L);
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
        assertEquals("STOCK_RECOVERED", result.getResolutionReason().name());
        assertNotNull(result.getResolvedAt());
    }

    @Test
    void repeatedAcknowledgementIsRejectedWithoutChangingOriginalAcknowledgement() {
        LowStockAlert alert = new LowStockAlert(
                location,
                item,
                new BigDecimal("10"),
                new BigDecimal("7"),
                AlertSeverity.LOW_STOCK
        );
        alert.acknowledge(51L, "checked");
        var acknowledgedAt = alert.getAcknowledgedAt();

        assertThrows(IllegalStateException.class, () -> alert.acknowledge(52L, "again"));
        assertEquals(51L, alert.getAcknowledgedByAccountId());
        assertEquals(acknowledgedAt, alert.getAcknowledgedAt());
        assertEquals(AlertStatus.ACKNOWLEDGED, alert.getStatus());
    }

    @Test
    void recoveredAlertStaysHistoricalAndLaterShortageCreatesAnotherAlert() {
        StockBalance balance = new StockBalance(location, item);
        balance.increaseQuantity(new BigDecimal("7"));
        LowStockAlert firstAlert = new LowStockAlert(
                location,
                item,
                new BigDecimal("10"),
                new BigDecimal("7"),
                AlertSeverity.LOW_STOCK
        );

        when(stockLocationRepository.existsById("Main")).thenReturn(true);
        when(inventoryItemRepository.existsById(1L)).thenReturn(true);
        when(stockReorderLevelRepository.findForUpdate("Main", 1L)).thenReturn(Optional.of(config));
        when(stockBalanceRepository.findByStockLocation_LocationCodeAndInventoryItem_Id("Main", 1L))
                .thenReturn(Optional.of(balance));
        when(lowStockAlertRepository.findActiveForUpdate(eq("Main"), eq(1L), any()))
                .thenReturn(Collections.emptyList(), Collections.singletonList(firstAlert), Collections.emptyList());
        when(lowStockAlertRepository.saveAndFlush(any(LowStockAlert.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(lowStockAlertRepository.saveAllAndFlush(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LowStockAlert created = lowStockAlertService.evaluateItem("Main", 1L);
        balance.increaseQuantity(new BigDecimal("10"));
        lowStockAlertService.evaluateItem("Main", 1L);
        balance.decreaseQuantity(new BigDecimal("10"));
        LowStockAlert later = lowStockAlertService.evaluateItem("Main", 1L);

        assertNotSame(created, later);
        assertEquals(AlertStatus.RESOLVED, firstAlert.getStatus());
        assertEquals(AlertStatus.OPEN, later.getStatus());
        verify(lowStockAlertRepository, org.mockito.Mockito.times(2)).saveAndFlush(any(LowStockAlert.class));
    }

    @Test
    void manualResolutionCannotCloseAnActualShortage() {
        ProductUserPrincipal principal = new ProductUserPrincipal(51L, "OWNER", null);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
        try {
            LowStockAlert alert = new LowStockAlert(
                    location,
                    item,
                    new BigDecimal("10"),
                    new BigDecimal("7"),
                    AlertSeverity.LOW_STOCK
            );
            StockBalance balance = new StockBalance(location, item);
            balance.increaseQuantity(new BigDecimal("7"));
            when(lowStockAlertRepository.findById(7L)).thenReturn(Optional.of(alert));
            when(stockReorderLevelRepository.findForUpdate("Main", 1L)).thenReturn(Optional.of(config));
            when(stockBalanceRepository.findByStockLocation_LocationCodeAndInventoryItem_Id("Main", 1L))
                    .thenReturn(Optional.of(balance));

            assertThrows(ConflictException.class, () -> lowStockAlertService.resolveAlert(7L, "resolved"));
            assertEquals(AlertStatus.OPEN, alert.getStatus());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void stockKeeperItemAlertListIsFilteredToMainWarehouse() {
        ProductUserPrincipal principal = new ProductUserPrincipal(51L, "STOCK_KEEPER", null);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
        try {
            StockLocation branch = new StockLocation("BR001", "Veyangoda", StockLocationType.BRANCH);
            LowStockAlert mainAlert = new LowStockAlert(
                    location, item, BigDecimal.TEN, new BigDecimal("7"), AlertSeverity.LOW_STOCK
            );
            LowStockAlert branchAlert = new LowStockAlert(
                    branch, item, BigDecimal.TEN, new BigDecimal("7"), AlertSeverity.LOW_STOCK
            );
            when(inventoryItemRepository.existsById(1L)).thenReturn(true);
            when(lowStockAlertRepository.findByInventoryItem_IdOrderByDetectedAtDesc(1L))
                    .thenReturn(java.util.List.of(mainAlert, branchAlert));

            var responses = lowStockAlertService.getAlertsByItem(1L);

            assertEquals(1, responses.size());
            assertEquals("Main", responses.getFirst().locationCode());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void branchManagerWithoutVerifiedAssignmentCannotReadAlerts() {
        ProductUserPrincipal principal = new ProductUserPrincipal(51L, "BRANCH_MANAGER", null);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
        when(stockLocationRepository.existsById("BR001")).thenReturn(true);
        try {
            assertThrows(
                    AccessDeniedException.class,
                    () -> lowStockAlertService.getAlertsByLocation("BR001")
            );
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void repeatedEvaluationUpdatesSingleActiveAlertInsteadOfDuplicating() {
        StockBalance balance = new StockBalance(location, item);
        balance.increaseQuantity(new BigDecimal("7"));
        LowStockAlert activeAlert = new LowStockAlert(
                location,
                item,
                new BigDecimal("10"),
                new BigDecimal("9"),
                AlertSeverity.LOW_STOCK
        );

        when(stockLocationRepository.existsById("Main")).thenReturn(true);
        when(inventoryItemRepository.existsById(1L)).thenReturn(true);
        when(stockReorderLevelRepository.findForUpdate("Main", 1L)).thenReturn(Optional.of(config));
        when(stockBalanceRepository.findByStockLocation_LocationCodeAndInventoryItem_Id("Main", 1L))
                .thenReturn(Optional.of(balance));
        when(lowStockAlertRepository.findActiveForUpdate(eq("Main"), eq(1L), any()))
                .thenReturn(Collections.singletonList(activeAlert));
        when(lowStockAlertRepository.saveAndFlush(any(LowStockAlert.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LowStockAlert first = lowStockAlertService.evaluateItem("Main", 1L);
        LowStockAlert repeated = lowStockAlertService.evaluateItem("Main", 1L);

        assertSame(activeAlert, first);
        assertSame(first, repeated);
        verify(lowStockAlertRepository, org.mockito.Mockito.never())
                .saveAndFlush(org.mockito.ArgumentMatchers.argThat(alert -> alert != activeAlert));
    }

    @Test
    void alertResponseSerializesStableFlatFields() throws Exception {
        LowStockAlert alert = new LowStockAlert(
                location, item, new BigDecimal("10"), new BigDecimal("7"), AlertSeverity.LOW_STOCK
        );

        String json = new ObjectMapper().findAndRegisterModules()
                .writeValueAsString(LowStockAlertResponse.from(alert));

        assertTrue(json.contains("\"locationCode\":\"Main\""));
        assertTrue(json.contains("\"inventoryItemId\":1"));
        assertTrue(json.contains("\"itemName\":\"Rice\""));
        assertTrue(json.contains("\"currentQuantity\":7"));
        assertFalse(json.contains("\"stockLocation\""));
    }
}
