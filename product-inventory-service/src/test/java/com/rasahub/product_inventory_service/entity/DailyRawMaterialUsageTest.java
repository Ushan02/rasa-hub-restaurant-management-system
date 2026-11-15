package com.rasahub.product_inventory_service.entity;

import com.rasahub.product_inventory_service.enums.DailyRawMaterialUsageStatus;
import com.rasahub.product_inventory_service.enums.InventoryItemType;
import com.rasahub.product_inventory_service.enums.MeasurementUnit;
import com.rasahub.product_inventory_service.enums.StockLocationType;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DailyRawMaterialUsageTest {

    @Test
    void startsAsDraftAndNormalizesRemarks() {
        DailyRawMaterialUsage usage = usage("  daily prep  ");

        assertEquals(DailyRawMaterialUsageStatus.DRAFT, usage.getStatus());
        assertEquals("daily prep", usage.getRemarks());
        assertTrue(usage.getItems().isEmpty());
    }

    @Test
    void onlyAcceptsValidActiveBranchAndNonFutureDate() {
        StockLocation inactiveBranch = location();
        inactiveBranch.setActive(false);
        assertThrows(
                IllegalArgumentException.class,
                () -> new DailyRawMaterialUsage(
                        inactiveBranch,
                        LocalDate.now(),
                        91L,
                        null
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new DailyRawMaterialUsage(
                        location(),
                        LocalDate.now().plusDays(1),
                        91L,
                        null
                )
        );
    }

    @Test
    void rejectsDuplicateItemsAndFractionalPieces() {
        DailyRawMaterialUsage usage = usage(null);
        InventoryItem item = item(10L, MeasurementUnit.PCS);

        usage.addItem(item, new BigDecimal("2"));

        assertThrows(
                IllegalArgumentException.class,
                () -> usage.addItem(item, new BigDecimal("1"))
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> usage.addItem(
                        item(11L, MeasurementUnit.PCS),
                        new BigDecimal("1.5")
                )
        );
    }

    @Test
    void replacesDraftItemsAndPreventsChangesAfterPosting() {
        DailyRawMaterialUsage usage = usage("original");
        usage.addItem(item(10L, MeasurementUnit.KG), new BigDecimal("1"));
        DailyRawMaterialUsageItem replacement =
                new DailyRawMaterialUsageItem(
                        usage,
                        item(11L, MeasurementUnit.KG),
                        new BigDecimal("2")
                );

        usage.replaceItems(List.of(replacement));
        ReflectionTestUtils.setField(usage, "id", 100L);
        usage.post(92L);

        assertEquals(DailyRawMaterialUsageStatus.POSTED, usage.getStatus());
        assertEquals(92L, usage.getPostedByAccountId());
        assertNotNull(usage.getPostedAt());
        assertEquals(11L, usage.getItems().get(0).getInventoryItem().getId());
        assertThrows(
                IllegalStateException.class,
                () -> usage.updateDraftDetails("changed")
        );
        assertThrows(
                IllegalStateException.class,
                () -> usage.replaceItems(List.of(replacement))
        );
    }

    @Test
    void doesNotAllowPostingWithoutSavedIdOrItems() {
        DailyRawMaterialUsage unsaved = usage(null);
        unsaved.addItem(item(10L, MeasurementUnit.KG), BigDecimal.ONE);

        assertThrows(
                IllegalStateException.class,
                () -> unsaved.post(91L)
        );

        DailyRawMaterialUsage empty = usage(null);
        ReflectionTestUtils.setField(empty, "id", 101L);
        assertThrows(
                IllegalStateException.class,
                () -> empty.post(91L)
        );
    }

    private DailyRawMaterialUsage usage(String remarks) {
        return new DailyRawMaterialUsage(
                location(),
                LocalDate.now(),
                91L,
                remarks
        );
    }

    private StockLocation location() {
        return new StockLocation(
                "BR001",
                "Test branch",
                StockLocationType.BRANCH
        );
    }

    private InventoryItem item(Long id, MeasurementUnit unit) {
        InventoryItem item = new InventoryItem(
                "ITEM" + id,
                "Raw material " + id,
                null,
                InventoryItemType.RAW_MATERIAL,
                unit
        );
        ReflectionTestUtils.setField(item, "id", id);
        return item;
    }
}
