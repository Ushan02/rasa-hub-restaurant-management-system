package com.rasahub.product_inventory_service.stockcount;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rasahub.product_inventory_service.entity.InventoryItem;
import com.rasahub.product_inventory_service.entity.StockBalance;
import com.rasahub.product_inventory_service.entity.StockCount;
import com.rasahub.product_inventory_service.entity.StockLocation;
import com.rasahub.product_inventory_service.entity.StockMovement;
import com.rasahub.product_inventory_service.enums.InventoryItemType;
import com.rasahub.product_inventory_service.enums.MeasurementUnit;
import com.rasahub.product_inventory_service.enums.StockAdjustmentStatus;
import com.rasahub.product_inventory_service.enums.StockCountStatus;
import com.rasahub.product_inventory_service.enums.StockLocationType;
import com.rasahub.product_inventory_service.enums.StockMovementType;
import com.rasahub.product_inventory_service.repository.InventoryItemRepository;
import com.rasahub.product_inventory_service.repository.StockAdjustmentRepository;
import com.rasahub.product_inventory_service.repository.StockBalanceRepository;
import com.rasahub.product_inventory_service.repository.StockCountRepository;
import com.rasahub.product_inventory_service.repository.StockLocationRepository;
import com.rasahub.product_inventory_service.repository.StockMovementRepository;
import com.sun.net.httpserver.HttpServer;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StockCountIntegrationTest {

    private static final String JWT_SECRET =
            "test-secret-key-for-daily-usage-module-tests-2026";
    private static final long BRANCH_ID = 7L;
    private static HttpServer branchServer;
    private static ExecutorService branchServerExecutor;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private StockCountRepository stockCountRepository;
    @Autowired
    private StockAdjustmentRepository stockAdjustmentRepository;
    @Autowired
    private InventoryItemRepository inventoryItemRepository;
    @Autowired
    private StockBalanceRepository stockBalanceRepository;
    @Autowired
    private StockLocationRepository stockLocationRepository;
    @Autowired
    private StockMovementRepository stockMovementRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private StockLocation branchOne;
    private StockLocation branchTwo;
    private StockLocation main;
    private InventoryItem itemOne;
    private InventoryItem itemTwo;
    private InventoryItem pcsItem;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        startBranchServer();
        registry.add(
                "spring.datasource.url",
                () -> "jdbc:h2:mem:rasa_hub_stock_count_integration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
        );
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("jwt.secret", () -> JWT_SECRET);
        registry.add(
                "branch.sync.internal-key",
                () -> "test-internal-key-for-isolated-test-only-123"
        );
        registry.add(
                "branch.sync.auth-base-url",
                () -> "http://127.0.0.1:"
                        + branchServer.getAddress().getPort()
        );
    }

    @BeforeEach
    void setUp() {
        stockMovementRepository.deleteAll();
        stockBalanceRepository.deleteAll();
        stockAdjustmentRepository.deleteAll();
        stockCountRepository.deleteAll();
        inventoryItemRepository.deleteAll();
        stockLocationRepository.deleteAll();

        branchOne = stockLocationRepository.saveAndFlush(
                new StockLocation(
                        "BR001",
                        "Branch One",
                        StockLocationType.BRANCH
                )
        );
        branchTwo = stockLocationRepository.saveAndFlush(
                new StockLocation(
                        "BR002",
                        "Branch Two",
                        StockLocationType.BRANCH
                )
        );
        main = stockLocationRepository.saveAndFlush(
                new StockLocation(
                        "Main",
                        "Main Warehouse",
                        StockLocationType.MAIN_WAREHOUSE
                )
        );
        itemOne = inventoryItemRepository.saveAndFlush(item(
                "CNT001",
                MeasurementUnit.KG
        ));
        itemTwo = inventoryItemRepository.saveAndFlush(item(
                "CNT002",
                MeasurementUnit.L
        ));
        pcsItem = inventoryItemRepository.saveAndFlush(item(
                "CNT003",
                MeasurementUnit.PCS
        ));
    }

    @AfterAll
    static void stopBranchServer() {
        if (branchServer != null) {
            branchServer.stop(0);
        }
        if (branchServerExecutor != null) {
            branchServerExecutor.shutdownNow();
        }
    }

    @Test
    void draftCanBeUpdatedSubmittedApprovedAndReconciledThroughOneAdjustment()
            throws Exception {
        createBalance(branchOne, itemOne, "10");
        long countId = createCount(
                91L,
                "BRANCH_MANAGER",
                BRANCH_ID,
                "BR001",
                itemLine(itemOne.getId(), null)
        );

        mockMvc.perform(get("/api/v1/stock-counts/{id}", countId)
                        .header("Authorization",
                                token(91L, "BRANCH_MANAGER", BRANCH_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.items[0].systemQuantity").value(10))
                .andExpect(jsonPath("$.items[0].physicalQuantity")
                        .value(nullValue()));

        String update = countUpdate(itemOne.getId(), "8");
        mockMvc.perform(put("/api/v1/stock-counts/{id}", countId)
                        .header("Authorization",
                                token(91L, "BRANCH_MANAGER", BRANCH_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(update))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].varianceQuantity").value(-2));
        submit(countId, 91L, "BRANCH_MANAGER", BRANCH_ID);

        mockMvc.perform(put("/api/v1/stock-counts/{id}", countId)
                        .header("Authorization",
                                token(91L, "BRANCH_MANAGER", BRANCH_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(update))
                .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/api/v1/stock-counts/{id}/approve", countId)
                        .header("Authorization", token(91L, "OWNER", null)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch("/api/v1/stock-counts/{id}/approve", countId)
                        .header("Authorization", token(92L, "OWNER", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mockMvc.perform(post(
                        "/api/v1/stock-counts/{id}/reconcile",
                        countId
                )
                        .header("Authorization",
                                token(92L, "STOCK_KEEPER", null)))
                .andExpect(status().isBadRequest());
        assertTrue(stockAdjustmentRepository
                .findBySourceStockCount_IdOrderByIdAsc(countId)
                .isEmpty());

        mockMvc.perform(post(
                        "/api/v1/stock-counts/{id}/reconcile",
                        countId
                )
                        .header("Authorization",
                                token(93L, "STOCK_KEEPER", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECONCILED"))
                .andExpect(jsonPath("$.reconciledByAccountId").value(93));

        assertEquals(new BigDecimal("8.000"), stockQuantity("BR001", itemOne));
        var adjustments = stockAdjustmentRepository
                .findBySourceStockCount_IdOrderByIdAsc(countId);
        assertEquals(1, adjustments.size());
        assertEquals(StockAdjustmentStatus.POSTED,
                adjustments.get(0).getStatus());
        assertEquals(countId, adjustments.get(0)
                .getSourceStockCount().getId());
        assertEquals(countId, adjustments.get(0).getItems().get(0)
                .getSourceStockCountItem().getStockCount().getId());
        mockMvc.perform(get(
                        "/api/v1/stock-adjustments/{id}",
                        adjustments.get(0).getId()
                )
                        .header("Authorization", token(92L, "OWNER", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceStockCountId").value(countId))
                .andExpect(jsonPath(
                        "$.items[0].sourceStockCountItemId"
                ).value(adjustments.get(0).getItems().get(0)
                        .getSourceStockCountItem().getId()));

        var movements = stockMovementRepository
                .findByReferenceTypeAndReferenceIdOrderByIdAsc(
                        "STOCK_ADJUSTMENT",
                        adjustments.get(0).getId().toString()
                );
        assertEquals(1, movements.size());
        assertEquals(StockMovementType.ADJUSTMENT_OUT,
                movements.get(0).getMovementType());
        assertEquals(new BigDecimal("10.000"),
                movements.get(0).getQuantityBefore());
        assertEquals(new BigDecimal("8.000"),
                movements.get(0).getQuantityAfter());
        assertEquals(1, stockMovementRepository.count());

        mockMvc.perform(post(
                        "/api/v1/stock-counts/{id}/reconcile",
                        countId
                )
                        .header("Authorization",
                                token(94L, "STOCK_KEEPER", null)))
                .andExpect(status().isBadRequest());
        assertEquals(1, stockMovementRepository.count());
    }

    @Test
    void appliesPositiveVarianceAndSkipsZeroVarianceMovements()
            throws Exception {
        createBalance(branchOne, itemOne, "5");
        long countId = createCount(
                91L,
                "BRANCH_MANAGER",
                BRANCH_ID,
                "BR001",
                itemLine(itemOne.getId(), "7"),
                itemLine(itemTwo.getId(), "0")
        );
        submitAndApprove(countId);
        mockMvc.perform(post(
                        "/api/v1/stock-counts/{id}/reconcile",
                        countId
                )
                        .header("Authorization",
                                token(93L, "STOCK_KEEPER", null)))
                .andExpect(status().isOk());

        assertEquals(new BigDecimal("7.000"), stockQuantity("BR001", itemOne));
        assertTrue(stockBalanceRepository
                .findByStockLocation_LocationCodeAndInventoryItem_Id(
                        "BR001",
                        itemTwo.getId()
                )
                .isEmpty());
        var adjustments = stockAdjustmentRepository
                .findBySourceStockCount_IdOrderByIdAsc(countId);
        assertEquals(1, adjustments.size());
        assertEquals("IN", adjustments.get(0).getDirection().name());
        assertEquals(1, stockMovementRepository.count());
        assertEquals(StockMovementType.ADJUSTMENT_IN,
                stockMovementRepository.findAll().get(0).getMovementType());
    }

    @Test
    void staleSnapshotReturnsConflictWithoutCreatingCorrection()
            throws Exception {
        createBalance(branchOne, itemOne, "10");
        long countId = createCount(
                91L,
                "BRANCH_MANAGER",
                BRANCH_ID,
                "BR001",
                itemLine(itemOne.getId(), "8")
        );

        StockBalance balance = stockBalanceRepository
                .findByStockLocation_LocationCodeAndInventoryItem_Id(
                        "BR001",
                        itemOne.getId()
                )
                .orElseThrow();
        BigDecimal before = balance.getQuantityOnHand();
        balance.increaseQuantity(new BigDecimal("2"));
        balance = stockBalanceRepository.saveAndFlush(balance);
        stockMovementRepository.saveAndFlush(new StockMovement(
                branchOne,
                itemOne,
                StockMovementType.GOODS_RECEIPT,
                new BigDecimal("2"),
                before,
                balance.getQuantityOnHand(),
                "TEST",
                "snapshot-race",
                95L,
                "Simulated receipt after stock count snapshot"
        ));

        submitAndApprove(countId);
        mockMvc.perform(post(
                        "/api/v1/stock-counts/{id}/reconcile",
                        countId
                )
                        .header("Authorization",
                                token(93L, "STOCK_KEEPER", null)))
                .andExpect(status().isConflict());

        assertEquals(new BigDecimal("12.000"), stockQuantity("BR001", itemOne));
        assertEquals(1, stockMovementRepository.count());
        assertTrue(stockAdjustmentRepository
                .findBySourceStockCount_IdOrderByIdAsc(countId)
                .isEmpty());
        assertEquals(StockCountStatus.APPROVED,
                stockCountRepository.findById(countId).orElseThrow()
                        .getStatus());
    }

    @Test
    void rejectsIncompleteCountsAndRejectedCountsCannotReconcile()
            throws Exception {
        long incompleteId = createCount(
                91L,
                "BRANCH_MANAGER",
                BRANCH_ID,
                "BR001",
                itemLine(itemOne.getId(), null)
        );
        mockMvc.perform(post(
                        "/api/v1/stock-counts/{id}/submit",
                        incompleteId
                )
                        .header("Authorization",
                                token(91L, "BRANCH_MANAGER", BRANCH_ID)))
                .andExpect(status().isBadRequest());

        long rejectedId = createCount(
                91L,
                "BRANCH_MANAGER",
                BRANCH_ID,
                "BR001",
                itemLine(itemOne.getId(), "0")
        );
        submit(rejectedId, 91L, "BRANCH_MANAGER", BRANCH_ID);
        mockMvc.perform(patch(
                        "/api/v1/stock-counts/{id}/reject",
                        rejectedId
                )
                        .header("Authorization", token(92L, "OWNER", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rejectionReason\":\"Please recount\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason")
                        .value("Please recount"));

        mockMvc.perform(post(
                        "/api/v1/stock-counts/{id}/reconcile",
                        rejectedId
                )
                        .header("Authorization",
                                token(93L, "STOCK_KEEPER", null)))
                .andExpect(status().isBadRequest());
        assertEquals(0, stockMovementRepository.count());
    }

    @Test
    void validatesLocationsDuplicatesNegativeAndFractionalPcs()
            throws Exception {
        mockMvc.perform(post("/api/v1/stock-counts")
                        .header("Authorization",
                                token(91L, "BRANCH_MANAGER", BRANCH_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(countBody(
                                "BAD",
                                itemLine(itemOne.getId(), "1")
                        )))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/stock-counts")
                        .header("Authorization",
                                token(91L, "BRANCH_MANAGER", BRANCH_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(countBody(
                                "BR001",
                                itemLine(itemOne.getId(), "-1")
                        )))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/stock-counts")
                        .header("Authorization",
                                token(91L, "BRANCH_MANAGER", BRANCH_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(countBody(
                                "BR001",
                                itemLine(itemOne.getId(), "1"),
                                itemLine(itemOne.getId(), "2")
                        )))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/stock-counts")
                        .header("Authorization",
                                token(91L, "BRANCH_MANAGER", BRANCH_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(countBody(
                                "BR001",
                                itemLine(pcsItem.getId(), "1.5")
                        )))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/stock-counts")
                        .header("Authorization", token(90L, "OWNER", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(countBody(
                                "BR001",
                                itemLine(itemOne.getId(), "1")
                        )))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/stock-counts")
                        .header("Authorization",
                                token(91L, "BRANCH_MANAGER", 8L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(countBody(
                                "BR001",
                                itemLine(itemOne.getId(), "1")
                        )))
                .andExpect(status().isForbidden());
    }

    @Test
    void enforcesBranchReadScopeAndInclusiveFiltersWithPagination()
            throws Exception {
        long first = saveDraft(branchOne, itemOne, "1", LocalDate.now());
        long second = saveDraft(branchOne, itemTwo, "1", LocalDate.now());
        long otherBranch = saveDraft(branchTwo, itemOne, "1", LocalDate.now());

        mockMvc.perform(get("/api/v1/stock-counts/location/BR001")
                        .header("Authorization", token(92L, "OWNER", null))
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content.length()").value(1));

        mockMvc.perform(get("/api/v1/stock-counts/date-range")
                        .header("Authorization",
                                token(92L, "MAIN_OFFICE_MANAGER", null))
                        .param("startDate", LocalDate.now().toString())
                        .param("endDate", LocalDate.now().toString())
                        .param("locationCode", "BR001")
                        .param("status", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(get("/api/v1/stock-counts/my")
                        .header("Authorization",
                                token(91L, "BRANCH_MANAGER", BRANCH_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(get(
                        "/api/v1/stock-counts/{id}",
                        otherBranch
                )
                        .header("Authorization",
                                token(91L, "BRANCH_MANAGER", BRANCH_ID)))
                .andExpect(status().isForbidden());
        assertNotEquals(first, second);
    }

    @Test
    void concurrentReconciliationPostsOnlyOneCorrection()
            throws Exception {
        createBalance(branchOne, itemOne, "10");
        long countId = createCount(
                91L,
                "BRANCH_MANAGER",
                BRANCH_ID,
                "BR001",
                itemLine(itemOne.getId(), "6")
        );
        submitAndApprove(countId);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService callers = Executors.newFixedThreadPool(2);
        try {
            List<Future<Integer>> results = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                results.add(callers.submit(() -> {
                    ready.countDown();
                    start.await();
                    return mockMvc.perform(post(
                                    "/api/v1/stock-counts/{id}/reconcile",
                                    countId
                            )
                                    .header("Authorization",
                                            token(93L, "STOCK_KEEPER", null)))
                            .andReturn()
                            .getResponse()
                            .getStatus();
                }));
            }
            ready.await();
            start.countDown();
            List<Integer> statuses = List.of(
                    results.get(0).get(),
                    results.get(1).get()
            );
            assertEquals(1, statuses.stream().filter(code -> code == 200).count());
            assertEquals(1, statuses.stream().filter(code -> code == 400).count());
        } finally {
            callers.shutdownNow();
        }

        assertEquals(new BigDecimal("6.000"), stockQuantity("BR001", itemOne));
        assertEquals(1, stockMovementRepository.count());
        assertEquals(1, stockAdjustmentRepository
                .findBySourceStockCount_IdOrderByIdAsc(countId).size());
    }

    @Test
    void mainWarehouseStockKeeperCanCreateAndReconcileCount()
            throws Exception {
        createBalance(main, itemOne, "5");
        long countId = createCount(
                91L,
                "STOCK_KEEPER",
                null,
                "Main",
                itemLine(itemOne.getId(), "4")
        );
        submit(countId, 91L, "STOCK_KEEPER", null);
        mockMvc.perform(patch("/api/v1/stock-counts/{id}/approve", countId)
                        .header("Authorization", token(92L, "OWNER", null)))
                .andExpect(status().isOk());
        mockMvc.perform(post(
                        "/api/v1/stock-counts/{id}/reconcile",
                        countId
                )
                        .header("Authorization",
                                token(93L, "STOCK_KEEPER", null)))
                .andExpect(status().isOk());

        assertEquals(new BigDecimal("4.000"), stockQuantity("Main", itemOne));
    }

    private void submitAndApprove(long countId) throws Exception {
        submit(countId, 91L, "BRANCH_MANAGER", BRANCH_ID);
        mockMvc.perform(patch(
                        "/api/v1/stock-counts/{id}/approve",
                        countId
                )
                        .header("Authorization", token(92L, "OWNER", null)))
                .andExpect(status().isOk());
    }

    private void submit(
            long countId,
            long accountId,
            String role,
            Long branchId
    ) throws Exception {
        mockMvc.perform(post(
                        "/api/v1/stock-counts/{id}/submit",
                        countId
                )
                        .header("Authorization",
                                token(accountId, role, branchId)))
                .andExpect(status().isOk());
    }

    private long createCount(
            long accountId,
            String role,
            Long branchId,
            String locationCode,
            String... itemLines
    ) throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/api/v1/stock-counts")
                                .header("Authorization",
                                        token(accountId, role, branchId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(countBody(locationCode, itemLines))
                )
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode response = objectMapper.readTree(
                result.getResponse().getContentAsString()
        );
        return response.get("id").asLong();
    }

    private String countBody(String locationCode, String... itemLines) {
        return """
                {
                  "locationCode": "%s",
                  "countDate": "%s",
                  "remarks": "integration stock count",
                  "items": [%s]
                }
                """.formatted(
                locationCode,
                LocalDate.now(),
                String.join(",", itemLines)
        );
    }

    private String countUpdate(Long itemId, String physicalQuantity) {
        return """
                {
                  "remarks": "updated count",
                  "items": [{
                    "inventoryItemId": %d,
                    "physicalQuantity": %s,
                    "itemRemarks": "counted"
                  }]
                }
                """.formatted(itemId, physicalQuantity);
    }

    private String itemLine(Long itemId, String physicalQuantity) {
        String line = "{\"inventoryItemId\":" + itemId;
        if (physicalQuantity != null) {
            line += ",\"physicalQuantity\":" + physicalQuantity;
        }
        return line + "}";
    }

    private long saveDraft(
            StockLocation location,
            InventoryItem inventoryItem,
            String physicalQuantity,
            LocalDate countDate
    ) {
        StockCount count = new StockCount(
                location,
                countDate,
                91L,
                "filter fixture"
        );
        count.addItem(
                inventoryItem,
                BigDecimal.ZERO,
                null,
                new BigDecimal(physicalQuantity),
                null
        );
        return stockCountRepository.saveAndFlush(count).getId();
    }

    private void createBalance(
            StockLocation location,
            InventoryItem inventoryItem,
            String quantity
    ) {
        StockBalance balance = new StockBalance(location, inventoryItem);
        balance.increaseQuantity(new BigDecimal(quantity));
        stockBalanceRepository.saveAndFlush(balance);
    }

    private BigDecimal stockQuantity(
            String locationCode,
            InventoryItem inventoryItem
    ) {
        return stockBalanceRepository
                .findByStockLocation_LocationCodeAndInventoryItem_Id(
                        locationCode,
                        inventoryItem.getId()
                )
                .orElseThrow()
                .getQuantityOnHand();
    }

    private InventoryItem item(String code, MeasurementUnit unit) {
        return new InventoryItem(
                code,
                "Count item " + code,
                null,
                InventoryItemType.RAW_MATERIAL,
                unit
        );
    }

    private String token(long accountId, String role, Long branchId) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .subject(Long.toString(accountId))
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(3600)));
        if (branchId != null) {
            builder.claim("branchId", branchId);
        }
        SecretKey key = Keys.hmacShaKeyFor(
                JWT_SECRET.getBytes(StandardCharsets.UTF_8)
        );
        return "Bearer " + builder.signWith(key).compact();
    }

    private static synchronized void startBranchServer() {
        if (branchServer != null) {
            return;
        }
        try {
            branchServer = HttpServer.create(
                    new InetSocketAddress("127.0.0.1", 0),
                    0
            );
            branchServer.createContext("/api/v1/branches/", exchange -> {
                String path = exchange.getRequestURI().getPath();
                String body = path.endsWith("/7")
                        ? "{\"id\":7,\"branchCode\":\"BR001\",\"active\":true}"
                        : "{\"id\":8,\"branchCode\":\"BR002\",\"active\":true}";
                byte[] response = body.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set(
                        "Content-Type",
                        "application/json"
                );
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            branchServerExecutor = Executors.newCachedThreadPool();
            branchServer.setExecutor(branchServerExecutor);
            branchServer.start();
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not start test branch assignment server",
                    exception
            );
        }
    }
}
