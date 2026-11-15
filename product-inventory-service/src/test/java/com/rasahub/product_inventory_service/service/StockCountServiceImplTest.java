package com.rasahub.product_inventory_service.service;

import com.rasahub.product_inventory_service.dto.stockcount.StockCountCreateRequest;
import com.rasahub.product_inventory_service.dto.stockcount.StockCountItemRequest;
import com.rasahub.product_inventory_service.entity.InventoryItem;
import com.rasahub.product_inventory_service.entity.StockBalance;
import com.rasahub.product_inventory_service.entity.StockLocation;
import com.rasahub.product_inventory_service.enums.InventoryItemType;
import com.rasahub.product_inventory_service.enums.MeasurementUnit;
import com.rasahub.product_inventory_service.enums.StockLocationType;
import com.rasahub.product_inventory_service.exception.BusinessRuleException;
import com.rasahub.product_inventory_service.repository.InventoryItemRepository;
import com.rasahub.product_inventory_service.repository.StockBalanceRepository;
import com.rasahub.product_inventory_service.repository.StockCountRepository;
import com.rasahub.product_inventory_service.repository.StockLocationRepository;
import com.rasahub.product_inventory_service.security.ProductUserPrincipal;
import com.rasahub.product_inventory_service.service.impl.StockCountServiceImpl;
import com.sun.net.httpserver.HttpServer;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockCountServiceImplTest {

    private static final long ACCOUNT_ID = 91L;
    private static final long BRANCH_ID = 7L;
    private static HttpServer branchServer;

    @Mock
    private StockCountRepository stockCountRepository;
    @Mock
    private InventoryItemRepository inventoryItemRepository;
    @Mock
    private StockLocationRepository stockLocationRepository;
    @Mock
    private StockBalanceRepository stockBalanceRepository;
    @Mock
    private StockAdjustmentService stockAdjustmentService;
    @Mock
    private HttpServletRequest servletRequest;

    private StockCountServiceImpl service;
    private StockLocation branch;
    private InventoryItem inventoryItem;
    private StockBalance balance;

    @org.junit.jupiter.api.BeforeAll
    static void startBranchServer() throws IOException {
        branchServer = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0),
                0
        );
        branchServer.createContext("/api/v1/branches/7", exchange -> {
            byte[] body = """
                    {"id":7,"branchCode":"BR001","active":true}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set(
                    "Content-Type",
                    "application/json"
            );
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        branchServer.start();
    }

    @org.junit.jupiter.api.AfterAll
    static void stopBranchServer() {
        branchServer.stop(0);
    }

    @BeforeEach
    void setUp() {
        branch = new StockLocation(
                "BR001",
                "Test branch",
                StockLocationType.BRANCH
        );
        inventoryItem = new InventoryItem(
                "CNT001",
                "Count item",
                null,
                InventoryItemType.RAW_MATERIAL,
                MeasurementUnit.KG
        );
        ReflectionTestUtils.setField(inventoryItem, "id", 101L);
        balance = new StockBalance(branch, inventoryItem);
        balance.increaseQuantity(new BigDecimal("12.500"));
        ReflectionTestUtils.setField(balance, "version", 4L);

        service = new StockCountServiceImpl(
                stockCountRepository,
                inventoryItemRepository,
                stockLocationRepository,
                stockBalanceRepository,
                stockAdjustmentService,
                servletRequest,
                "http://127.0.0.1:"
                        + branchServer.getAddress().getPort()
        );
        when(servletRequest.getHeader(HttpHeaders.AUTHORIZATION))
                .thenReturn("Bearer test-token");
        lenient().when(stockLocationRepository.findByIdForUpdate("BR001"))
                .thenReturn(Optional.of(branch));
        lenient().when(inventoryItemRepository.findById(101L))
                .thenReturn(Optional.of(inventoryItem));
        lenient().when(stockBalanceRepository.findForUpdate("BR001", 101L))
                .thenReturn(Optional.of(balance));
        lenient().when(stockCountRepository.saveAndFlush(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        authenticate();
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createsDraftUsingLockedSystemQuantityAndAuthenticatedAccount() {
        var response = service.createStockCount(createRequest("BR001"));

        assertEquals("BR001", response.getLocationCode());
        assertEquals(ACCOUNT_ID, response.getCreatedByAccountId());
        assertEquals("DRAFT", response.getStatus().name());
        assertEquals(new BigDecimal("12.500"),
                response.getItems().get(0).getSystemQuantity());
        assertEquals(4L,
                response.getItems().get(0).getSystemBalanceVersion());
        verify(stockBalanceRepository)
                .findForUpdate("BR001", 101L);
        verify(stockCountRepository).saveAndFlush(any());
    }

    @Test
    void rejectsAnotherBranchBeforeLookingUpItsLocation() {
        assertThrows(AccessDeniedException.class, () ->
                service.createStockCount(createRequest("BR002")));

        verify(stockLocationRepository, never())
                .findByIdForUpdate(anyString());
        verify(stockCountRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsDuplicateItemsBeforeSnapshotting() {
        StockCountCreateRequest request = createRequest("BR001");
        request.setItems(List.of(
                itemRequest(101L, BigDecimal.ONE),
                itemRequest(101L, BigDecimal.TEN)
        ));

        assertThrows(BusinessRuleException.class, () ->
                service.createStockCount(request));
        verify(stockBalanceRepository, never())
                .findForUpdate(anyString(), anyLong());
        verify(stockCountRepository, never()).saveAndFlush(any());
    }

    private StockCountCreateRequest createRequest(String locationCode) {
        StockCountCreateRequest request = new StockCountCreateRequest();
        request.setLocationCode(locationCode);
        request.setRemarks("unit test");
        request.setItems(List.of(itemRequest(101L, BigDecimal.ZERO)));
        return request;
    }

    private StockCountItemRequest itemRequest(
            Long itemId,
            BigDecimal physicalQuantity
    ) {
        StockCountItemRequest request = new StockCountItemRequest();
        request.setInventoryItemId(itemId);
        request.setPhysicalQuantity(physicalQuantity);
        return request;
    }

    private void authenticate() {
        ProductUserPrincipal principal = new ProductUserPrincipal(
                ACCOUNT_ID,
                "BRANCH_MANAGER",
                BRANCH_ID
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.getAuthorities()
                )
        );
    }
}
