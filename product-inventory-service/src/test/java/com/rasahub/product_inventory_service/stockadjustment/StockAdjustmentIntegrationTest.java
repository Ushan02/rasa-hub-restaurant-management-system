package com.rasahub.product_inventory_service.stockadjustment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rasahub.product_inventory_service.entity.InventoryItem;
import com.rasahub.product_inventory_service.entity.StockAdjustment;
import com.rasahub.product_inventory_service.entity.StockBalance;
import com.rasahub.product_inventory_service.entity.StockLocation;
import com.rasahub.product_inventory_service.enums.InventoryItemType;
import com.rasahub.product_inventory_service.enums.MeasurementUnit;
import com.rasahub.product_inventory_service.enums.StockAdjustmentDirection;
import com.rasahub.product_inventory_service.enums.StockAdjustmentReason;
import com.rasahub.product_inventory_service.enums.StockAdjustmentStatus;
import com.rasahub.product_inventory_service.enums.StockLocationType;
import com.rasahub.product_inventory_service.enums.StockMovementType;
import com.rasahub.product_inventory_service.repository.InventoryItemRepository;
import com.rasahub.product_inventory_service.repository.StockAdjustmentRepository;
import com.rasahub.product_inventory_service.repository.StockBalanceRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StockAdjustmentIntegrationTest {

    private static final String JWT_SECRET =
            "test-secret-key-for-daily-usage-module-tests-2026";
    private static final long BRANCH_ID = 7L;
    private static HttpServer branchServer;
    private static ExecutorService branchServerExecutor;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StockAdjustmentRepository adjustmentRepository;

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
    private InventoryItem itemOne;
    private InventoryItem itemTwo;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        startBranchServer();
        registry.add(
                "spring.datasource.url",
                () -> "jdbc:h2:mem:rasa_hub_stock_adjustment;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
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
        adjustmentRepository.deleteAll();
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
        stockLocationRepository.saveAndFlush(
                new StockLocation(
                        "Main",
                        "Main Warehouse",
                        StockLocationType.MAIN_WAREHOUSE
                )
        );
        itemOne = inventoryItemRepository.saveAndFlush(item("RAW001"));
        itemTwo = inventoryItemRepository.saveAndFlush(item("RAW002"));
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
    void persistsDraftAndPostsAtomicStockAdjustmentWithMovement()
            throws Exception {
        createBalance(branchOne, itemOne, "10");
        long adjustmentId = createAdjustment(
                91L,
                "BRANCH_MANAGER",
                BRANCH_ID,
                "OUT",
                "DAMAGED",
                itemOne.getId(),
                "2"
        );

        mockMvc.perform(get(
                        "/api/v1/stock-adjustments/{id}",
                        adjustmentId
                )
                        .header("Authorization",
                                token(91L, "BRANCH_MANAGER", BRANCH_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.createdByAccountId").value(91))
                .andExpect(jsonPath("$.items.length()").value(1));

        String updateBody = """
                {
                  "locationCode": "BR001",
                  "adjustmentReason": "DAMAGED",
                  "direction": "OUT",
                  "remarks": "updated test adjustment",
                  "items": [{"inventoryItemId": %d, "adjustmentQuantity": 3}]
                }
                """.formatted(itemOne.getId());
        mockMvc.perform(put(
                        "/api/v1/stock-adjustments/{id}",
                        adjustmentId
                )
                        .header("Authorization",
                                token(91L, "BRANCH_MANAGER", BRANCH_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.remarks")
                        .value("updated test adjustment"))
                .andExpect(jsonPath(
                        "$.items[0].adjustmentQuantity"
                ).value(3));

        mockMvc.perform(post(
                        "/api/v1/stock-adjustments/{id}/submit",
                        adjustmentId
                )
                        .header("Authorization",
                                token(91L, "BRANCH_MANAGER", BRANCH_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"));

        mockMvc.perform(post(
                        "/api/v1/stock-adjustments/{id}/approve",
                        adjustmentId
                )
                        .header("Authorization",
                                token(91L, "OWNER", null)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post(
                        "/api/v1/stock-adjustments/{id}/approve",
                        adjustmentId
                )
                        .header("Authorization",
                                token(92L, "OWNER", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mockMvc.perform(post(
                        "/api/v1/stock-adjustments/{id}/post",
                        adjustmentId
                )
                        .header("Authorization",
                                token(92L, "STOCK_KEEPER", null)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post(
                        "/api/v1/stock-adjustments/{id}/post",
                        adjustmentId
                )
                        .header("Authorization",
                                token(93L, "STOCK_KEEPER", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("POSTED"))
                .andExpect(jsonPath("$.postedByAccountId").value(93));

        assertEquals(
                StockAdjustmentStatus.POSTED,
                adjustmentRepository.findWithItemsById(adjustmentId)
                        .orElseThrow().getStatus()
        );
        assertEquals(
                new BigDecimal("7.000"),
                stockQuantity("BR001", itemOne)
        );
        var movement = stockMovementRepository
                .findByReferenceTypeAndReferenceIdOrderByIdAsc(
                        "STOCK_ADJUSTMENT",
                        Long.toString(adjustmentId)
                )
                .get(0);
        assertEquals(StockMovementType.ADJUSTMENT_OUT,
                movement.getMovementType());
        assertEquals(new BigDecimal("10.000"),
                movement.getQuantityBefore());
        assertEquals(new BigDecimal("7.000"),
                movement.getQuantityAfter());
        assertEquals(93L, movement.getPerformedByAccountId());
    }

    @Test
    void insufficientStockRollsBackAndRepeatedPostingIsRejected()
            throws Exception {
        createBalance(branchOne, itemOne, "1");
        long adjustmentId = createAdjustment(
                91L,
                "BRANCH_MANAGER",
                BRANCH_ID,
                "OUT",
                "DAMAGED",
                itemOne.getId(),
                "2"
        );
        submitAndApprove(adjustmentId);

        mockMvc.perform(post(
                        "/api/v1/stock-adjustments/{id}/post",
                        adjustmentId
                )
                        .header("Authorization",
                                token(93L, "STOCK_KEEPER", null)))
                .andExpect(status().isBadRequest());

        assertEquals(new BigDecimal("1.000"),
                stockQuantity("BR001", itemOne));
        assertEquals(0, stockMovementRepository.count());
        assertEquals(StockAdjustmentStatus.APPROVED,
                adjustmentRepository.findById(adjustmentId)
                        .orElseThrow().getStatus());

        StockBalance balance = stockBalanceRepository
                .findByStockLocation_LocationCodeAndInventoryItem_Id(
                        "BR001",
                        itemOne.getId()
                )
                .orElseThrow();
        balance.increaseQuantity(new BigDecimal("5"));
        stockBalanceRepository.saveAndFlush(balance);
        mockMvc.perform(post(
                        "/api/v1/stock-adjustments/{id}/post",
                        adjustmentId
                )
                        .header("Authorization",
                                token(93L, "STOCK_KEEPER", null)))
                .andExpect(status().isOk());
        mockMvc.perform(post(
                        "/api/v1/stock-adjustments/{id}/post",
                        adjustmentId
                )
                        .header("Authorization",
                                token(93L, "STOCK_KEEPER", null)))
                .andExpect(status().isBadRequest());

        assertEquals(1, stockMovementRepository.count());
    }

    @Test
    void enforcesRoleAndBranchAuthorization() throws Exception {
        mockMvc.perform(post("/api/v1/stock-adjustments")
                        .header("Authorization",
                                token(91L, "ACCOUNTANT", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(adjustmentBody(
                                "BR001",
                                "OUT",
                                "DAMAGED",
                                itemOne.getId(),
                                "1"
                        )))
                .andExpect(status().isForbidden());

        long adjustmentId = saveDraft(branchTwo, itemOne, 91L, "1");
        mockMvc.perform(get(
                        "/api/v1/stock-adjustments/{id}",
                        adjustmentId
                )
                        .header("Authorization",
                                token(91L, "BRANCH_MANAGER", BRANCH_ID)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/stock-adjustments")
                        .header("Authorization",
                                token(91L, "BRANCH_MANAGER", 8L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(adjustmentBody(
                                "BR001",
                                "OUT",
                                "DAMAGED",
                                itemOne.getId(),
                                "1"
                        )))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsInvalidDirectionManualCorrectionAndDuplicateItems()
            throws Exception {
        mockMvc.perform(post("/api/v1/stock-adjustments")
                        .header("Authorization",
                                token(91L, "BRANCH_MANAGER", BRANCH_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(adjustmentBody(
                                "BR001",
                                "OUT",
                                "FOUND",
                                itemOne.getId(),
                                "1"
                        )))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/stock-adjustments")
                        .header("Authorization",
                                token(91L, "BRANCH_MANAGER", BRANCH_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "locationCode": "BR001",
                                  "adjustmentReason": "MANUAL_CORRECTION",
                                  "direction": "IN",
                                  "items": [{"inventoryItemId": %d,
                                             "adjustmentQuantity": 1}]
                                }
                                """.formatted(itemOne.getId())))
                .andExpect(status().isBadRequest());

        String duplicateItems = """
                {
                  "locationCode": "BR001",
                  "adjustmentReason": "DAMAGED",
                  "direction": "OUT",
                  "remarks": "duplicate fixture",
                  "items": [
                    {"inventoryItemId": %d, "adjustmentQuantity": 1},
                    {"inventoryItemId": %d, "adjustmentQuantity": 2}
                  ]
                }
                """.formatted(itemOne.getId(), itemOne.getId());
        mockMvc.perform(post("/api/v1/stock-adjustments")
                        .header("Authorization",
                                token(91L, "BRANCH_MANAGER", BRANCH_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(duplicateItems))
                .andExpect(status().isBadRequest());

        InventoryItem pcsItem = inventoryItemRepository.saveAndFlush(
                new InventoryItem(
                        "PCS001",
                        "Piece item",
                        null,
                        InventoryItemType.RAW_MATERIAL,
                        MeasurementUnit.PCS
                )
        );
        mockMvc.perform(post("/api/v1/stock-adjustments")
                        .header("Authorization",
                                token(91L, "BRANCH_MANAGER", BRANCH_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(adjustmentBody(
                                "BR001",
                                "OUT",
                                "DAMAGED",
                                pcsItem.getId(),
                                "1.5"
                        )))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postsIncomingAdjustmentAndCreatesInitialBalance()
            throws Exception {
        long adjustmentId = createAdjustmentAt(
                91L,
                "STOCK_KEEPER",
                null,
                "Main",
                "IN",
                "FOUND",
                itemOne.getId(),
                "4"
        );
        mockMvc.perform(post(
                        "/api/v1/stock-adjustments/{id}/submit",
                        adjustmentId
                )
                        .header("Authorization",
                                token(91L, "STOCK_KEEPER", null)))
                .andExpect(status().isOk());
        mockMvc.perform(post(
                        "/api/v1/stock-adjustments/{id}/approve",
                        adjustmentId
                )
                        .header("Authorization",
                                token(92L, "OWNER", null)))
                .andExpect(status().isOk());
        mockMvc.perform(post(
                        "/api/v1/stock-adjustments/{id}/post",
                        adjustmentId
                )
                        .header("Authorization",
                                token(93L, "STOCK_KEEPER", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("POSTED"));

        assertEquals(new BigDecimal("4.000"), stockQuantity("Main", itemOne));
        var movement = stockMovementRepository
                .findByReferenceTypeAndReferenceIdOrderByIdAsc(
                        "STOCK_ADJUSTMENT",
                        Long.toString(adjustmentId)
                )
                .get(0);
        assertEquals(StockMovementType.ADJUSTMENT_IN,
                movement.getMovementType());
        assertEquals(new BigDecimal("0.000"), movement.getQuantityBefore());
        assertEquals(new BigDecimal("4.000"),
                movement.getQuantityAfter());
    }

    @Test
    void returnsDateStatusAndPaginatedResults() throws Exception {
        long first = saveDraft(branchOne, itemOne, 91L, "1");
        long second = saveDraft(branchOne, itemTwo, 91L, "1");
        saveDraft(branchTwo, itemOne, 91L, "1");

        mockMvc.perform(get("/api/v1/stock-adjustments/location/BR001")
                        .header("Authorization", token(92L, "OWNER", null))
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content.length()").value(1));

        mockMvc.perform(get(
                        "/api/v1/stock-adjustments/date-range"
                )
                        .header("Authorization",
                                token(92L, "MAIN_OFFICE_MANAGER", null))
                        .param("startDate", LocalDate.now().toString())
                        .param("endDate", LocalDate.now().toString())
                        .param("locationCode", "BR001")
                        .param("status", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(get("/api/v1/stock-adjustments/status/DRAFT")
                        .header("Authorization", token(92L, "OWNER", null))
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));
        assertNotEquals(first, second);
    }

    @Test
    void concurrentPostingAppliesExactlyOneDeductionAndMovement()
            throws Exception {
        createBalance(branchOne, itemOne, "10");
        long adjustmentId = createAdjustment(
                91L,
                "BRANCH_MANAGER",
                BRANCH_ID,
                "OUT",
                "DAMAGED",
                itemOne.getId(),
                "4"
        );
        submitAndApprove(adjustmentId);

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
                                    "/api/v1/stock-adjustments/{id}/post",
                                    adjustmentId
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
            assertEquals(new BigDecimal("6.000"),
                    stockQuantity("BR001", itemOne));
            assertEquals(1, stockMovementRepository.count());
        } finally {
            callers.shutdownNow();
        }
    }

    private void submitAndApprove(long adjustmentId) throws Exception {
        mockMvc.perform(post(
                        "/api/v1/stock-adjustments/{id}/submit",
                        adjustmentId
                )
                        .header("Authorization",
                                token(91L, "BRANCH_MANAGER", BRANCH_ID)))
                .andExpect(status().isOk());
        mockMvc.perform(post(
                        "/api/v1/stock-adjustments/{id}/approve",
                        adjustmentId
                )
                        .header("Authorization",
                                token(92L, "OWNER", null)))
                .andExpect(status().isOk());
    }

    private long createAdjustment(
            long accountId,
            String role,
            Long branchId,
            String direction,
            String reason,
            Long inventoryItemId,
            String quantity
    ) throws Exception {
        return createAdjustmentAt(
                accountId,
                role,
                branchId,
                "BR001",
                direction,
                reason,
                inventoryItemId,
                quantity
        );
    }

    private long createAdjustmentAt(
            long accountId,
            String role,
            Long branchId,
            String locationCode,
            String direction,
            String reason,
            Long inventoryItemId,
            String quantity
    ) throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/api/v1/stock-adjustments")
                                .header("Authorization",
                                        token(accountId, role, branchId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(adjustmentBody(
                                        locationCode,
                                        direction,
                                        reason,
                                        inventoryItemId,
                                        quantity
                                ))
                )
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode response = objectMapper.readTree(
                result.getResponse().getContentAsString()
        );
        return response.get("id").asLong();
    }

    private String adjustmentBody(
            String locationCode,
            String direction,
            String reason,
            Long inventoryItemId,
            String quantity
    ) {
        return """
                {
                  "locationCode": "%s",
                  "adjustmentReason": "%s",
                  "direction": "%s",
                  "remarks": "test adjustment",
                  "items": [{"inventoryItemId": %d, "adjustmentQuantity": %s}]
                }
                """.formatted(
                locationCode,
                reason,
                direction,
                inventoryItemId,
                quantity
        );
    }

    private long saveDraft(
            StockLocation location,
            InventoryItem item,
            long createdBy,
            String quantity
    ) {
        StockAdjustment adjustment = new StockAdjustment(
                location,
                StockAdjustmentReason.DAMAGED,
                StockAdjustmentDirection.OUT,
                createdBy,
                "integration fixture"
        );
        adjustment.addItem(item, new BigDecimal(quantity));
        return adjustmentRepository.saveAndFlush(adjustment).getId();
    }

    private void createBalance(
            StockLocation location,
            InventoryItem item,
            String quantity
    ) {
        StockBalance balance = new StockBalance(location, item);
        balance.increaseQuantity(new BigDecimal(quantity));
        stockBalanceRepository.saveAndFlush(balance);
    }

    private BigDecimal stockQuantity(
            String locationCode,
            InventoryItem item
    ) {
        return stockBalanceRepository
                .findByStockLocation_LocationCodeAndInventoryItem_Id(
                        locationCode,
                        item.getId()
                )
                .orElseThrow()
                .getQuantityOnHand();
    }

    private InventoryItem item(String code) {
        return new InventoryItem(
                code,
                "Raw material " + code,
                null,
                InventoryItemType.RAW_MATERIAL,
                MeasurementUnit.KG
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
