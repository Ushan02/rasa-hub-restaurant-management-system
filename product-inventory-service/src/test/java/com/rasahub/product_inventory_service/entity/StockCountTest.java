package com.rasahub.product_inventory_service.entity;

import com.rasahub.product_inventory_service.enums.InventoryItemType;
import com.rasahub.product_inventory_service.enums.MeasurementUnit;
import com.rasahub.product_inventory_service.enums.StockCountStatus;
import com.rasahub.product_inventory_service.enums.StockLocationType;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class StockCountTest {

    @Test
    void derivesPositiveNegativeAndZeroVariancesFromPhysicalCount() {
        StockCount count = newCount();
        InventoryItem first = item(11L, MeasurementUnit.KG);
        InventoryItem second = item(12L, MeasurementUnit.KG);
        InventoryItem third = item(13L, MeasurementUnit.PCS);

        count.addItem(
                first,
                new BigDecimal("100.000"),
                2L,
                new BigDecimal("95.000"),
                null
        );
        count.addItem(
                second,
                new BigDecimal("10.000"),
                4L,
                new BigDecimal("13.000"),
                "overage"
        );
        count.addItem(
                third,
                BigDecimal.ZERO,
                null,
                BigDecimal.ZERO,
                null
        );

        assertEquals(new BigDecimal("-5.000"),
                count.getItems().get(0).getVarianceQuantity());
        assertEquals(new BigDecimal("3.000"),
                count.getItems().get(1).getVarianceQuantity());
        assertEquals(BigDecimal.ZERO,
                count.getItems().get(2).getVarianceQuantity());
    }

    @Test
    void rejectsNegativeAndFractionalPieceCounts() {
        StockCount count = newCount();
        InventoryItem pieces = item(21L, MeasurementUnit.PCS);

        assertThrows(IllegalArgumentException.class, () ->
                count.addItem(
                        pieces,
                        BigDecimal.ZERO,
                        null,
                        new BigDecimal("-1"),
                        null
                ));
        assertThrows(IllegalArgumentException.class, () ->
                count.addItem(
                        pieces,
                        BigDecimal.ZERO,
                        null,
                        new BigDecimal("1.5"),
                        null
                ));
    }

    @Test
    void requiresCompleteCountAndEnforcesReviewAndReconciliationTransitions() {
        StockCount count = newCount();
        count.addItem(
                item(31L, MeasurementUnit.KG),
                BigDecimal.ZERO,
                null,
                null,
                null
        );
        assertThrows(IllegalStateException.class, () -> count.submit(91L));

        count.updateItem(
                31L,
                BigDecimal.ZERO,
                null
        );
        count.submit(91L);
        assertEquals(StockCountStatus.SUBMITTED, count.getStatus());
        assertThrows(IllegalStateException.class, () -> count.approve(91L));

        count.approve(92L);
        assertEquals(StockCountStatus.APPROVED, count.getStatus());
        assertThrows(IllegalStateException.class, () -> count.reconcile(92L));

        count.reconcile(93L);
        assertEquals(StockCountStatus.RECONCILED, count.getStatus());
        assertThrows(IllegalStateException.class, () -> count.reconcile(94L));
    }

    @Test
    void rejectedCountCannotBeReconciled() {
        StockCount count = newCount();
        count.addItem(
                item(41L, MeasurementUnit.L),
                BigDecimal.ZERO,
                null,
                BigDecimal.ZERO,
                null
        );
        count.submit(91L);
        count.reject(92L, "Recount required");

        assertEquals(StockCountStatus.REJECTED, count.getStatus());
        assertEquals("Recount required", count.getRejectionReason());
        assertThrows(IllegalStateException.class, () -> count.reconcile(93L));
    }

    private StockCount newCount() {
        return new StockCount(
                new StockLocation(
                        "BR001",
                        "Test branch",
                        StockLocationType.BRANCH
                ),
                LocalDate.now(),
                91L,
                "test count"
        );
    }

    private InventoryItem item(Long id, MeasurementUnit unit) {
        InventoryItem item = new InventoryItem(
                "CNT" + id,
                "Count item " + id,
                null,
                InventoryItemType.RAW_MATERIAL,
                unit
        );
        ReflectionTestUtils.setField(item, "id", id);
        return item;
    }
}
