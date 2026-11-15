package com.rasahub.product_inventory_service.service;

import com.rasahub.product_inventory_service.dto.dailyrawmaterialusage.DailyRawMaterialUsageCreateRequest;
import com.rasahub.product_inventory_service.dto.dailyrawmaterialusage.DailyRawMaterialUsageItemRequest;
import com.rasahub.product_inventory_service.dto.dailyrawmaterialusage.DailyRawMaterialUsageUpdateRequest;
import com.rasahub.product_inventory_service.entity.DailyRawMaterialUsage;
import com.rasahub.product_inventory_service.entity.InventoryItem;
import com.rasahub.product_inventory_service.entity.StockBalance;
import com.rasahub.product_inventory_service.entity.StockLocation;
import com.rasahub.product_inventory_service.entity.StockMovement;
import com.rasahub.product_inventory_service.enums.DailyRawMaterialUsageStatus;
import com.rasahub.product_inventory_service.enums.InventoryItemType;
import com.rasahub.product_inventory_service.enums.MeasurementUnit;
import com.rasahub.product_inventory_service.enums.StockLocationType;
import com.rasahub.product_inventory_service.enums.StockMovementType;
import com.rasahub.product_inventory_service.exception.BusinessRuleException;
import com.rasahub.product_inventory_service.exception.ResourceNotFoundException;
import com.rasahub.product_inventory_service.repository.DailyRawMaterialUsageRepository;
import com.rasahub.product_inventory_service.repository.InventoryItemRepository;
import com.rasahub.product_inventory_service.repository.StockBalanceRepository;
import com.rasahub.product_inventory_service.repository.StockLocationRepository;
import com.rasahub.product_inventory_service.repository.StockMovementRepository;
import com.rasahub.product_inventory_service.security.ProductUserPrincipal;
import com.rasahub.product_inventory_service.service.impl.DailyRawMaterialUsageServiceImpl;
import com.sun.net.httpserver.HttpServer;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailyRawMaterialUsageServiceImplTest {

    private static final long ACCOUNT_ID = 91L;
    private static final long BRANCH_ID = 7L;
    private static HttpServer branchServer;

    @Mock
    private DailyRawMaterialUsageRepository usageRepository;

    @Mock
    private InventoryItemRepository inventoryItemRepository;

    @Mock
    private StockLocationRepository stockLocationRepository;

    @Mock
    private StockBalanceRepository stockBalanceRepository;

    @Mock
    private StockMovementRepository stockMovementRepository;

    @Mock
    private HttpServletRequest servletRequest;

    private DailyRawMaterialUsageServiceImpl service;
    private StockLocation branch;

    @org.junit.jupiter.api.BeforeAll
    static void startBranchAssignmentServer() throws IOException {
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
    static void stopBranchAssignmentServer() {
        branchServer.stop(0);
    }

    @BeforeEach
    void setUp() {
        branch = new StockLocation(
                "BR001",
                "Test branch",
                StockLocationType.BRANCH
        );
        service = new DailyRawMaterialUsageServiceImpl(
                usageRepository,
                inventoryItemRepository,
                stockLocationRepository,
                stockBalanceRepository,
                stockMovementRepository,
                servletRequest,
                "http://127.0.0.1:" + branchServer.getAddress().getPort()
        );
        lenient().when(servletRequest.getHeader(HttpHeaders.AUTHORIZATION))
                .thenReturn("Bearer test-token");
        lenient().when(stockLocationRepository.findById("BR001"))
                .thenReturn(Optional.of(branch));
        lenient().when(usageRepository.saveAndFlush(any(DailyRawMaterialUsage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        authenticate("BRANCH_MANAGER", BRANCH_ID);
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createsValidDraftWithPrincipalAccountAndAssignedBranch() {
        InventoryItem item = inventory(10L, MeasurementUnit.KG, true);
        when(inventoryItemRepository.findById(10L))
                .thenReturn(Optional.of(item));

        var result = service.createDailyUsage(
                createRequest(LocalDate.now(), itemRequest(10L, "1.250"))
        );

        assertEquals(DailyRawMaterialUsageStatus.DRAFT, result.getStatus());
        assertEquals("BR001", result.getBranchLocationCode());
        assertEquals(ACCOUNT_ID, result.getCreatedByAccountId());
        assertEquals(new BigDecimal("1.250"),
                result.getItems().get(0).getUsedQuantity());
        verify(usageRepository).saveAndFlush(any(DailyRawMaterialUsage.class));
    }

    @Test
    void rejectsUnknownInventoryItem() {
        when(inventoryItemRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.createDailyUsage(
                        createRequest(LocalDate.now(),
                                itemRequest(999L, "1"))
                )
        );
    }

    @Test
    void rejectsInactiveInventoryItem() {
        InventoryItem item = inventory(10L, MeasurementUnit.KG, false);
        when(inventoryItemRepository.findById(10L))
                .thenReturn(Optional.of(item));

        assertThrows(
                BusinessRuleException.class,
                () -> service.createDailyUsage(
                        createRequest(LocalDate.now(),
                                itemRequest(10L, "1"))
                )
        );
    }

    @Test
    void rejectsDuplicateInventoryItems() {
        InventoryItem item = inventory(10L, MeasurementUnit.KG, true);
        when(inventoryItemRepository.findById(10L))
                .thenReturn(Optional.of(item));

        assertThrows(
                BusinessRuleException.class,
                () -> service.createDailyUsage(
                        createRequest(
                                LocalDate.now(),
                                itemRequest(10L, "1"),
                                itemRequest(10L, "2")
                        )
                )
        );
    }

    @Test
    void rejectsZeroAndNegativeQuantities() {
        InventoryItem item = inventory(10L, MeasurementUnit.KG, true);
        when(inventoryItemRepository.findById(10L))
                .thenReturn(Optional.of(item));

        assertThrows(
                BusinessRuleException.class,
                () -> service.createDailyUsage(
                        createRequest(LocalDate.now(),
                                itemRequest(10L, "0"))
                )
        );
        assertThrows(
                BusinessRuleException.class,
                () -> service.createDailyUsage(
                        createRequest(LocalDate.now(),
                                itemRequest(10L, "-1"))
                )
        );
    }

    @Test
    void rejectsFractionalPcsQuantity() {
        InventoryItem item = inventory(10L, MeasurementUnit.PCS, true);
        when(inventoryItemRepository.findById(10L))
                .thenReturn(Optional.of(item));

        assertThrows(
                BusinessRuleException.class,
                () -> service.createDailyUsage(
                        createRequest(LocalDate.now(),
                                itemRequest(10L, "1.5"))
                )
        );
    }

    @Test
    void rejectsFutureUsageDate() {
        assertThrows(
                BusinessRuleException.class,
                () -> service.createDailyUsage(
                        createRequest(LocalDate.now().plusDays(1),
                                itemRequest(10L, "1"))
                )
        );
        verifyNoInteractions(inventoryItemRepository);
    }

    @Test
    void updatesDraftAndReplacesItsItems() {
        DailyRawMaterialUsage usage = usageWithItem(201L, "1");
        InventoryItem replacement = inventory(11L, MeasurementUnit.KG, true);
        when(usageRepository.findWithItemsById(201L))
                .thenReturn(Optional.of(usage));
        when(inventoryItemRepository.findById(11L))
                .thenReturn(Optional.of(replacement));

        DailyRawMaterialUsageUpdateRequest request =
                new DailyRawMaterialUsageUpdateRequest();
        request.setRemarks("  adjusted  ");
        request.setItems(List.of(itemRequest(11L, "2.5")));

        var result = service.updateDraftUsage(201L, request);

        assertEquals("adjusted", result.getRemarks());
        assertEquals(11L, result.getItems().get(0).getInventoryItemId());
        assertEquals(new BigDecimal("2.5"),
                result.getItems().get(0).getUsedQuantity());
        verify(usageRepository).saveAndFlush(usage);
    }

    @Test
    void rejectsUpdateOfPostedRecord() {
        DailyRawMaterialUsage usage = usageWithItem(201L, "1");
        usage.post(ACCOUNT_ID);
        when(usageRepository.findWithItemsById(201L))
                .thenReturn(Optional.of(usage));

        DailyRawMaterialUsageUpdateRequest request =
                new DailyRawMaterialUsageUpdateRequest();
        request.setItems(List.of(itemRequest(10L, "2")));

        assertThrows(
                BusinessRuleException.class,
                () -> service.updateDraftUsage(201L, request)
        );
    }

    @Test
    void postsUsageDeductsStockAndCreatesMovement() {
        DailyRawMaterialUsage usage = usageWithItem(201L, "2.5");
        StockBalance balance = balanceWithQuantity("10");
        when(usageRepository.findByIdForUpdate(201L))
                .thenReturn(Optional.of(usage));
        when(stockBalanceRepository.findForUpdate("BR001", 10L))
                .thenReturn(Optional.of(balance));
        when(usageRepository.saveAndFlush(usage)).thenReturn(usage);

        var result = service.postDailyUsage(201L);

        assertEquals(DailyRawMaterialUsageStatus.POSTED, result.getStatus());
        assertEquals(ACCOUNT_ID, result.getPostedByAccountId());
        assertEquals(new BigDecimal("7.5"), balance.getQuantityOnHand());
        ArgumentCaptor<StockMovement> captor =
                ArgumentCaptor.forClass(StockMovement.class);
        verify(stockMovementRepository).save(captor.capture());
        StockMovement movement = captor.getValue();
        assertEquals(StockMovementType.DAILY_USAGE,
                movement.getMovementType());
        assertEquals(new BigDecimal("2.5"), movement.getQuantity());
        assertEquals(new BigDecimal("10"), movement.getQuantityBefore());
        assertEquals(new BigDecimal("7.5"), movement.getQuantityAfter());
        assertEquals("201", movement.getReferenceId());
        assertEquals(ACCOUNT_ID, movement.getPerformedByAccountId());
    }

    @Test
    void rejectsPostingWhenStockIsInsufficient() {
        DailyRawMaterialUsage usage = usageWithItem(201L, "2");
        StockBalance balance = balanceWithQuantity("1");
        when(usageRepository.findByIdForUpdate(201L))
                .thenReturn(Optional.of(usage));
        when(stockBalanceRepository.findForUpdate("BR001", 10L))
                .thenReturn(Optional.of(balance));

        assertThrows(
                BusinessRuleException.class,
                () -> service.postDailyUsage(201L)
        );
        verify(stockMovementRepository, never()).save(any());
        verify(usageRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsRepeatedPostingBeforeAccessingStock() {
        DailyRawMaterialUsage usage = usageWithItem(201L, "2");
        usage.post(ACCOUNT_ID);
        when(usageRepository.findByIdForUpdate(201L))
                .thenReturn(Optional.of(usage));

        assertThrows(
                BusinessRuleException.class,
                () -> service.postDailyUsage(201L)
        );
        verifyNoInteractions(stockBalanceRepository, stockMovementRepository);
    }

    @Test
    void rejectsUnauthorizedRoleForMutations() {
        authenticate("OWNER", null);

        assertThrows(
                AccessDeniedException.class,
                () -> service.createDailyUsage(
                        createRequest(LocalDate.now(),
                                itemRequest(10L, "1"))
                )
        );
    }

    @Test
    void branchManagerCannotReadAnotherBranch() {
        assertThrows(
                AccessDeniedException.class,
                () -> service.getDailyUsagesByBranch("BR002", 0, 20)
        );
        verify(usageRepository, never())
                .findByBranchLocation_LocationCodeOrderByUsageDateDescIdDesc(
                        anyString(),
                        any()
                );
    }

    @Test
    void retrievesUsageByIdForCentralOfficeRole() {
        authenticate("OWNER", null);
        DailyRawMaterialUsage usage = usageWithItem(201L, "1");
        when(usageRepository.findWithItemsById(201L))
                .thenReturn(Optional.of(usage));

        var result = service.getDailyUsageById(201L);

        assertEquals(201L, result.getId());
        assertEquals(1, result.getItems().size());
    }

    @Test
    void appliesBranchStatusDateAndPaginationFilters() {
        PageRequest pageable = PageRequest.of(1, 5);
        when(usageRepository
                .findByBranchLocation_LocationCodeAndStatusOrderByUsageDateDescIdDesc(
                        eq("BR001"),
                        eq(DailyRawMaterialUsageStatus.DRAFT),
                        any()
                ))
                .thenReturn(Page.empty(pageable));
        when(usageRepository
                .findByBranchLocation_LocationCodeAndUsageDateBetweenOrderByUsageDateDescIdDesc(
                        eq("BR001"),
                        eq(LocalDate.of(2026, 1, 1)),
                        eq(LocalDate.of(2026, 1, 31)),
                        any()
                ))
                .thenReturn(Page.empty(pageable));

        service.getDailyUsagesByStatus(
                DailyRawMaterialUsageStatus.DRAFT,
                1,
                5
        );
        service.getDailyUsagesByDateRange(
                "BR001",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31),
                1,
                5
        );

        verify(usageRepository)
                .findByBranchLocation_LocationCodeAndStatusOrderByUsageDateDescIdDesc(
                        "BR001",
                        DailyRawMaterialUsageStatus.DRAFT,
                        pageable
                );
        verify(usageRepository)
                .findByBranchLocation_LocationCodeAndUsageDateBetweenOrderByUsageDateDescIdDesc(
                        "BR001",
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 1, 31),
                        pageable
                );
    }

    @Test
    void rejectsReversedDateRange() {
        authenticate("OWNER", null);

        assertThrows(
                BusinessRuleException.class,
                () -> service.getDailyUsagesByDateRange(
                        "BR001",
                        LocalDate.of(2026, 2, 1),
                        LocalDate.of(2026, 1, 1),
                        0,
                        20
                )
        );
        verifyNoInteractions(usageRepository);
    }

    @Test
    void appliesCentralOfficePaginationToAllRecords() {
        authenticate("MAIN_OFFICE_MANAGER", null);
        PageRequest pageable = PageRequest.of(2, 10);
        when(usageRepository.findAllByOrderByUsageDateDescIdDesc(pageable))
                .thenReturn(Page.empty(pageable));

        service.getAllDailyUsages(2, 10);

        verify(usageRepository).findAllByOrderByUsageDateDescIdDesc(pageable);
    }

    private DailyRawMaterialUsage usageWithItem(
            Long usageId,
            String quantity
    ) {
        DailyRawMaterialUsage usage = new DailyRawMaterialUsage(
                branch,
                LocalDate.now(),
                ACCOUNT_ID,
                "usage"
        );
        usage.addItem(
                inventory(10L, MeasurementUnit.KG, true),
                new BigDecimal(quantity)
        );
        ReflectionTestUtils.setField(usage, "id", usageId);
        return usage;
    }

    private StockBalance balanceWithQuantity(String quantity) {
        StockBalance balance = new StockBalance(
                branch,
                inventory(10L, MeasurementUnit.KG, true)
        );
        balance.increaseQuantity(new BigDecimal(quantity));
        return balance;
    }

    private InventoryItem inventory(
            Long id,
            MeasurementUnit unit,
            boolean active
    ) {
        InventoryItem item = new InventoryItem(
                "ITEM" + id,
                "Item " + id,
                null,
                InventoryItemType.RAW_MATERIAL,
                unit
        );
        item.setActive(active);
        ReflectionTestUtils.setField(item, "id", id);
        return item;
    }

    private DailyRawMaterialUsageCreateRequest createRequest(
            LocalDate date,
            DailyRawMaterialUsageItemRequest... items
    ) {
        DailyRawMaterialUsageCreateRequest request =
                new DailyRawMaterialUsageCreateRequest();
        request.setUsageDate(date);
        request.setRemarks("test");
        request.setItems(List.of(items));
        return request;
    }

    private DailyRawMaterialUsageItemRequest itemRequest(
            Long id,
            String quantity
    ) {
        DailyRawMaterialUsageItemRequest request =
                new DailyRawMaterialUsageItemRequest();
        request.setInventoryItemId(id);
        request.setUsedQuantity(new BigDecimal(quantity));
        return request;
    }

    private void authenticate(String role, Long branchId) {
        ProductUserPrincipal principal = new ProductUserPrincipal(
                ACCOUNT_ID,
                role,
                branchId
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
