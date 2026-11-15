package com.rasahub.product_inventory_service.service;

import com.rasahub.product_inventory_service.entity.InventoryItem;
import com.rasahub.product_inventory_service.entity.StockLocation;
import com.rasahub.product_inventory_service.entity.StockReorderLevel;
import com.rasahub.product_inventory_service.enums.InventoryItemType;
import com.rasahub.product_inventory_service.enums.MeasurementUnit;
import com.rasahub.product_inventory_service.enums.StockLocationType;
import com.rasahub.product_inventory_service.repository.InventoryItemRepository;
import com.rasahub.product_inventory_service.repository.StockBalanceRepository;
import com.rasahub.product_inventory_service.repository.StockLocationRepository;
import com.rasahub.product_inventory_service.repository.StockReorderLevelRepository;
import com.rasahub.product_inventory_service.security.ProductUserPrincipal;
import com.rasahub.product_inventory_service.service.impl.StockReorderLevelServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockReorderLevelServiceImplTest {

    @Mock
    private StockReorderLevelRepository reorderLevelRepository;
    @Mock
    private StockLocationRepository locationRepository;
    @Mock
    private InventoryItemRepository inventoryItemRepository;
    @Mock
    private LowStockAlertEvaluationService lowStockAlertEvaluationService;
    @Mock
    private StockBalanceRepository stockBalanceRepository;

    private StockReorderLevelServiceImpl service;
    private StockReorderLevel config;

    @BeforeEach
    void setUp() {
        service = new StockReorderLevelServiceImpl(
                reorderLevelRepository,
                locationRepository,
                inventoryItemRepository,
                lowStockAlertEvaluationService,
                stockBalanceRepository
        );
        StockLocation location = new StockLocation("Main", "Main Warehouse", StockLocationType.MAIN_WAREHOUSE);
        InventoryItem item = new InventoryItem(
                "R001",
                "Rice",
                "Rice bag",
                InventoryItemType.RAW_MATERIAL,
                MeasurementUnit.KG
        );
        ReflectionTestUtils.setField(item, "id", 1L);
        config = new StockReorderLevel(location, item, new BigDecimal("10"));
        authenticate();
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deactivationClosesMonitoringOnceWithoutEvaluatingRecovery() {
        when(locationRepository.existsById("Main")).thenReturn(true);
        when(inventoryItemRepository.existsById(1L)).thenReturn(true);
        when(reorderLevelRepository.findForUpdate("Main", 1L)).thenReturn(Optional.of(config));
        when(reorderLevelRepository.saveAndFlush(any(StockReorderLevel.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(stockBalanceRepository.findByStockLocation_LocationCodeAndInventoryItem_Id("Main", 1L))
                .thenReturn(Optional.empty());

        service.deactivateConfiguration("Main", 1L, null);
        service.deactivateConfiguration("Main", 1L, null);

        assertFalse(config.isActive());
        verify(lowStockAlertEvaluationService).closeForMonitoringDisabled("Main", 1L);
        verify(lowStockAlertEvaluationService, never()).evaluateItem("Main", 1L);
    }

    @Test
    void reactivationReevaluatesTheCurrentStock() {
        config.setActive(false);
        when(locationRepository.existsById("Main")).thenReturn(true);
        when(inventoryItemRepository.existsById(1L)).thenReturn(true);
        when(reorderLevelRepository.findForUpdate("Main", 1L)).thenReturn(Optional.of(config));
        when(reorderLevelRepository.saveAndFlush(any(StockReorderLevel.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(stockBalanceRepository.findByStockLocation_LocationCodeAndInventoryItem_Id("Main", 1L))
                .thenReturn(Optional.empty());

        service.reactivateConfiguration("Main", 1L, null);

        assertTrue(config.isActive());
        verify(lowStockAlertEvaluationService).evaluateItem("Main", 1L);
    }

    private void authenticate() {
        ProductUserPrincipal principal = new ProductUserPrincipal(91L, "OWNER", null);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }
}
