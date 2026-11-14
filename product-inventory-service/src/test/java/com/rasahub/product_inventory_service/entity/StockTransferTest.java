
package com.rasahub.product_inventory_service.entity;

import com.rasahub.product_inventory_service.enums.BranchStockRequestStatus;
import com.rasahub.product_inventory_service.enums.MeasurementUnit;
import com.rasahub.product_inventory_service.enums.StockTransferStatus;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StockTransferTest {

    @Test
    void draftCanBeDispatchedAndThenReceivedOnlyOnce() {
        StockTransfer transfer =
                newTransfer(MeasurementUnit.KG);

        assertEquals(
                StockTransferStatus.DRAFT,
                transfer.getStatus()
        );

        ReflectionTestUtils.setField(transfer, "id", 7L);

        transfer.dispatch(8L);

        assertEquals(
                StockTransferStatus.DISPATCHED,
                transfer.getStatus()
        );

        assertThrows(
                IllegalStateException.class,
                () -> transfer.dispatch(8L)
        );

        transfer.confirmReceipt(20L);

        assertEquals(
                StockTransferStatus.RECEIVED,
                transfer.getStatus()
        );

        assertNotNull(transfer.getReceivedAt());

        assertThrows(
                IllegalStateException.class,
                () -> transfer.confirmReceipt(20L)
        );

        assertThrows(
                IllegalStateException.class,
                () -> transfer.cancel(8L, "Too late")
        );
    }

    @Test
    void pcsCannotTransferFractionalQuantity() {
        StockLocation main = location("Main");
        StockLocation branch = location("BR002");

        BranchStockRequest request = request(branch);
        InventoryItem inventory = inventory(MeasurementUnit.PCS);

        StockTransfer transfer =
                new StockTransfer(request, main, 8L, null);

        assertThrows(
                IllegalArgumentException.class,
                () -> transfer.addItem(
                        inventory,
                        new BigDecimal("1.5")
                )
        );
    }

    private StockTransfer newTransfer(MeasurementUnit unit) {
        StockLocation main = location("Main");
        StockLocation branch = location("BR002");
        BranchStockRequest request = request(branch);

        StockTransfer transfer =
                new StockTransfer(
                        request,
                        main,
                        8L,
                        "Test transfer"
                );

        transfer.addItem(
                inventory(unit),
                new BigDecimal("60.000")
        );

        return transfer;
    }

    private StockLocation location(String code) {
        StockLocation location = mock(StockLocation.class);

        when(location.getLocationCode()).thenReturn(code);
        when(location.isActive()).thenReturn(true);

        return location;
    }

    private InventoryItem inventory(MeasurementUnit unit) {
        InventoryItem item = mock(InventoryItem.class);

        when(item.getId()).thenReturn(10L);
        when(item.isActive()).thenReturn(true);
        when(item.getMeasurementUnit()).thenReturn(unit);

        return item;
    }

    private BranchStockRequest request(StockLocation branch) {
        BranchStockRequest request =
                mock(BranchStockRequest.class);

        BranchStockRequestItem requested =
                mock(BranchStockRequestItem.class);

        InventoryItem item = inventory(MeasurementUnit.KG);

        when(request.getId()).thenReturn(101L);
        when(request.getStatus())
                .thenReturn(BranchStockRequestStatus.APPROVED);
        when(request.getBranchLocation()).thenReturn(branch);
        when(requested.getInventoryItem()).thenReturn(item);
        when(requested.getOutstandingQuantity())
                .thenReturn(new BigDecimal("100.000"));
        when(request.getItems()).thenReturn(List.of(requested));

        return request;
    }
}
