package com.rasahub.product_inventory_service.service;

import com.rasahub.product_inventory_service.entity.InventoryItem;
import com.rasahub.product_inventory_service.entity.StockLocation;
import com.rasahub.product_inventory_service.entity.StockReorderLevel;
import com.rasahub.product_inventory_service.enums.InventoryItemType;
import com.rasahub.product_inventory_service.enums.MeasurementUnit;
import com.rasahub.product_inventory_service.enums.StockLocationType;
import com.rasahub.product_inventory_service.repository.StockReorderLevelRepository;
import com.rasahub.product_inventory_service.service.impl.LowStockAlertReconciliationScheduler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LowStockAlertReconciliationSchedulerTest {

    @Mock
    private StockReorderLevelRepository stockReorderLevelRepository;
    @Mock
    private LowStockAlertEvaluationService lowStockAlertEvaluationService;

    @Test
    void reconcilesInBatchesAndContinuesAfterAnItemFailure() {
        StockLocation location = new StockLocation("Main", "Main Warehouse", StockLocationType.MAIN_WAREHOUSE);
        InventoryItem firstItem = item("R001");
        InventoryItem secondItem = item("R002");
        StockReorderLevel first = new StockReorderLevel(location, firstItem, BigDecimal.TEN);
        StockReorderLevel second = new StockReorderLevel(location, secondItem, BigDecimal.TEN);
        ReflectionTestUtils.setField(firstItem, "id", 101L);
        ReflectionTestUtils.setField(secondItem, "id", 102L);
        ReflectionTestUtils.setField(first, "id", 1L);
        ReflectionTestUtils.setField(second, "id", 2L);
        LowStockAlertReconciliationScheduler scheduler = new LowStockAlertReconciliationScheduler(
                stockReorderLevelRepository,
                lowStockAlertEvaluationService,
                2
        );

        when(stockReorderLevelRepository.findByActiveTrueAndIdGreaterThanOrderByIdAsc(eq(0L), any()))
                .thenReturn(List.of(first, second));
        when(stockReorderLevelRepository.findByActiveTrueAndIdGreaterThanOrderByIdAsc(eq(2L), any()))
                .thenReturn(List.of());
        doAnswer(invocation -> {
            if (Long.valueOf(102L).equals(invocation.getArgument(1))) {
                throw new IllegalStateException("temporary failure");
            }
            return null;
        }).when(lowStockAlertEvaluationService).evaluateItem(anyString(), anyLong());

        scheduler.reconcileActiveConfigurations();

        verify(lowStockAlertEvaluationService).evaluateItem("Main", firstItem.getId());
        verify(lowStockAlertEvaluationService).evaluateItem("Main", secondItem.getId());
    }

    private InventoryItem item(String code) {
        return new InventoryItem(
                code,
                "Item " + code,
                null,
                InventoryItemType.RAW_MATERIAL,
                MeasurementUnit.KG
        );
    }
}
