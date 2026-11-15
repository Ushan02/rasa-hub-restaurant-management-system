package com.rasahub.product_inventory_service.service;

import com.rasahub.product_inventory_service.dto.stockbalance.StockBalanceMinimumLevelUpdateRequest;
import com.rasahub.product_inventory_service.entity.InventoryItem;
import com.rasahub.product_inventory_service.entity.StockBalance;
import com.rasahub.product_inventory_service.entity.StockLocation;
import com.rasahub.product_inventory_service.entity.StockReorderLevel;
import com.rasahub.product_inventory_service.enums.InventoryItemType;
import com.rasahub.product_inventory_service.enums.MeasurementUnit;
import com.rasahub.product_inventory_service.enums.StockLocationType;
import com.rasahub.product_inventory_service.enums.StockStatus;
import com.rasahub.product_inventory_service.repository.InventoryItemRepository;
import com.rasahub.product_inventory_service.repository.StockBalanceRepository;
import com.rasahub.product_inventory_service.repository.StockLocationRepository;
import com.rasahub.product_inventory_service.repository.StockReorderLevelRepository;
import com.rasahub.product_inventory_service.service.impl.StockBalanceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockBalanceServiceImplTest {

    @Mock
    private StockBalanceRepository stockBalanceRepository;
    @Mock
    private StockLocationRepository stockLocationRepository;
    @Mock
    private InventoryItemRepository inventoryItemRepository;
    @Mock
    private StockReorderLevelRepository stockReorderLevelRepository;
    @Mock
    private StockReorderLevelService stockReorderLevelService;

    private StockBalanceServiceImpl service;
    private StockLocation location;
    private InventoryItem item;

    @BeforeEach
    void setUp() {
        service = new StockBalanceServiceImpl(
                stockBalanceRepository,
                stockLocationRepository,
                inventoryItemRepository,
                stockReorderLevelRepository,
                stockReorderLevelService
        );
        location = new StockLocation(
                "Main",
                "Main Warehouse",
                StockLocationType.MAIN_WAREHOUSE
        );
        item = new InventoryItem(
                "R001",
                "Rice",
                "Rice bag",
                InventoryItemType.RAW_MATERIAL,
                MeasurementUnit.KG
        );
        ReflectionTestUtils.setField(item, "id", 1L);
    }

    @Test
    void balanceResponseUsesConfiguredThresholdAtTheInclusiveBoundary() {
        StockBalance balance = new StockBalance(location, item);
        balance.increaseQuantity(new BigDecimal("5.000"));
        ReflectionTestUtils.setField(balance, "minimumStockLevel", new BigDecimal("100.000"));
        StockReorderLevel configuration =
                new StockReorderLevel(location, item, new BigDecimal("5.000"));
        when(stockLocationRepository.findById("Main")).thenReturn(Optional.of(location));
        when(inventoryItemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(stockBalanceRepository.findByStockLocation_LocationCodeAndInventoryItem_Id("Main", 1L))
                .thenReturn(Optional.of(balance));
        when(stockReorderLevelRepository
                .findByStockLocation_LocationCodeAndInventoryItem_IdAndActiveTrue("Main", 1L))
                .thenReturn(Optional.of(configuration));

        var response = service.getBalance("Main", 1L);

        assertEquals(new BigDecimal("5.000"), response.getReorderLevel());
        assertEquals(new BigDecimal("5.000"), response.getMinimumStockLevel());
        assertEquals(StockStatus.LOW_STOCK, response.getStockStatus());
        assertTrue(response.isLowStock());
    }

    @Test
    void legacyMinimumLevelEndpointUpdatesTheAuthoritativeConfiguration() {
        StockBalance balance = new StockBalance(location, item);
        balance.increaseQuantity(new BigDecimal("5.000"));
        StockReorderLevel configuration =
                new StockReorderLevel(location, item, new BigDecimal("5.000"));
        configuration.setRecommendedReorderQuantity(new BigDecimal("20.000"));
        when(stockLocationRepository.findById("Main")).thenReturn(Optional.of(location));
        when(inventoryItemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(stockReorderLevelRepository.findForUpdate("Main", 1L))
                .thenReturn(Optional.of(configuration));
        when(stockReorderLevelService.updateConfiguration(
                eq("Main"),
                eq(1L),
                eq(new BigDecimal("7.000")),
                eq(new BigDecimal("20.000")),
                isNull()
        )).thenAnswer(invocation -> {
            configuration.setReorderLevel(invocation.getArgument(2));
            return null;
        });
        when(stockBalanceRepository.findForUpdate("Main", 1L))
                .thenReturn(Optional.of(balance));
        when(stockReorderLevelRepository
                .findByStockLocation_LocationCodeAndInventoryItem_IdAndActiveTrue("Main", 1L))
                .thenReturn(Optional.of(configuration));

        StockBalanceMinimumLevelUpdateRequest request =
                new StockBalanceMinimumLevelUpdateRequest();
        request.setMinimumStockLevel(new BigDecimal("7.000"));

        var response = service.updateMinimumStockLevel("Main", 1L, request);

        assertEquals(new BigDecimal("7.000"), response.getReorderLevel());
        assertEquals(StockStatus.LOW_STOCK, response.getStockStatus());
        assertEquals(0, ((BigDecimal) ReflectionTestUtils.getField(
                balance, "minimumStockLevel"
        )).compareTo(BigDecimal.ZERO));
        verify(stockReorderLevelService).updateConfiguration(
                "Main",
                1L,
                new BigDecimal("7.000"),
                new BigDecimal("20.000"),
                null
        );
        verify(stockBalanceRepository, never()).saveAndFlush(any(StockBalance.class));
    }

    @Test
    void inactiveOrMissingConfigurationDoesNotExposeLegacyThreshold() {
        StockBalance balance = new StockBalance(location, item);
        ReflectionTestUtils.setField(balance, "minimumStockLevel", new BigDecimal("10.000"));
        when(stockLocationRepository.findById("Main")).thenReturn(Optional.of(location));
        when(inventoryItemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(stockBalanceRepository.findByStockLocation_LocationCodeAndInventoryItem_Id("Main", 1L))
                .thenReturn(Optional.of(balance));
        when(stockReorderLevelRepository
                .findByStockLocation_LocationCodeAndInventoryItem_IdAndActiveTrue("Main", 1L))
                .thenReturn(Optional.empty());

        var response = service.getBalance("Main", 1L);

        assertNull(response.getReorderLevel());
        assertEquals(BigDecimal.ZERO, response.getMinimumStockLevel());
        assertEquals(StockStatus.OUT_OF_STOCK, response.getStockStatus());
        assertTrue(response.isLowStock());
    }
}
