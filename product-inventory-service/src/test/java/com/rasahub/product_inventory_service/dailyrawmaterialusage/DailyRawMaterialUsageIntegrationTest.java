package com.rasahub.product_inventory_service.dailyrawmaterialusage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rasahub.product_inventory_service.entity.DailyRawMaterialUsage;
import com.rasahub.product_inventory_service.entity.InventoryItem;
import com.rasahub.product_inventory_service.entity.StockBalance;
import com.rasahub.product_inventory_service.entity.StockLocation;
import com.rasahub.product_inventory_service.enums.DailyRawMaterialUsageStatus;
import com.rasahub.product_inventory_service.enums.InventoryItemType;
import com.rasahub.product_inventory_service.enums.MeasurementUnit;
import com.rasahub.product_inventory_service.enums.StockLocationType;
import com.rasahub.product_inventory_service.enums.StockMovementType;
import com.rasahub.product_inventory_service.repository.DailyRawMaterialUsageRepository;
import com.rasahub.product_inventory_service.repository.InventoryItemRepository;
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
class DailyRawMaterialUsageIntegrationTest {

    private static final String JWT_SECRET =
            "test-secret-key-for-daily-usage-module-tests-2026";
    private static final long ACCOUNT_ID = 91L;
    private static final long BRANCH_ID = 7L;
    private static HttpServer branchServer;
    private static ExecutorService branchServerExecutor;

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private DailyRawMaterialUsageRepository usageRepository;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private StockBalanceRepository stockBalanceRepository;

    @Autowired
    private StockLocationRepository stockLocationRepository;

    @Autowired
    private StockMovementRepository stockMovementRepository;

    private StockLocation branchOne;
    private StockLocation branchTwo;
    private InventoryItem itemOne;
    private InventoryItem itemTwo;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        startBranchServer();
        registry.add(
                "spring.datasource.url",
                () -> "jdbc:h2:mem:rasa_hub_daily_usage_integration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
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
        usageRepository.deleteAll();
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
    void createsEditsPostsAndPersistsUsageWithStockMovement() throws Exception {
        createBalance(branchOne, itemOne, "10");

        long usageId = createUsage(
                "BRANCH_MANAGER",
                BRANCH_ID,
                itemOne.getId(),
                "2"
        );

        mockMvc.perform(get("/api/v1/daily-raw-material-usages/{id}", usageId)
                        .header("Authorization", token("BRANCH_MANAGER", BRANCH_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.createdByAccountId").value(ACCOUNT_ID))
                .andExpect(jsonPath("$.items.length()").value(1));

        String updateBody = """
                {
                  "remarks": "adjusted prep",
                  "items": [{"inventoryItemId": %d, "usedQuantity": 3}]
                }
                """.formatted(itemOne.getId());
        mockMvc.perform(put("/api/v1/daily-raw-material-usages/{id}", usageId)
                        .header("Authorization", token("BRANCH_MANAGER", BRANCH_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remarks").value("adjusted prep"))
                .andExpect(jsonPath("$.status").value("DRAFT"));

        mockMvc.perform(post(
                        "/api/v1/daily-raw-material-usages/{id}/post",
                        usageId
                )
                        .header("Authorization", token("BRANCH_MANAGER", BRANCH_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("POSTED"))
                .andExpect(jsonPath("$.postedByAccountId").value(ACCOUNT_ID));

        assertEquals(
                new BigDecimal("7.000"),
                stockBalanceRepository
                        .findByStockLocation_LocationCodeAndInventoryItem_Id(
                                "BR001",
                                itemOne.getId()
                        )
                        .orElseThrow()
                        .getQuantityOnHand()
        );
        assertEquals(1, stockMovementRepository.count());
        var movement = stockMovementRepository
                .findByReferenceTypeAndReferenceIdOrderByIdAsc(
                        "DAILY_USAGE",
                        Long.toString(usageId)
                )
                .get(0);
        assertEquals(StockMovementType.DAILY_USAGE, movement.getMovementType());
        assertEquals("BR001", movement.getStockLocation().getLocationCode());
        assertEquals(new BigDecimal("10.000"), movement.getQuantityBefore());
        assertEquals(new BigDecimal("7.000"), movement.getQuantityAfter());
        assertEquals(usageId, usageRepository.findWithItemsById(usageId)
                .orElseThrow().getId());

        mockMvc.perform(post(
                        "/api/v1/daily-raw-material-usages/{id}/post",
                        usageId
                )
                        .header("Authorization", token("BRANCH_MANAGER", BRANCH_ID)))
                .andExpect(status().isBadRequest());
        assertEquals(1, stockMovementRepository.count());
        assertEquals(new BigDecimal("7.000"),
                stockBalanceRepository
                        .findByStockLocation_LocationCodeAndInventoryItem_Id(
                                "BR001",
                                itemOne.getId()
                        )
                        .orElseThrow()
                        .getQuantityOnHand());
    }

    @Test
    void acceptsSmallestPositiveThreeDecimalQuantity() throws Exception {
        long usageId = createUsage(
                        "BRANCH_MANAGER",
                        BRANCH_ID,
                        itemOne.getId(),
                        "0.001"
        );

        assertEquals(
                        new BigDecimal("0.001"),
                        usageRepository.findWithItemsById(usageId)
                                .orElseThrow()
                                .getItems()
                                .get(0)
                                .getUsedQuantity()
        );
    }

    @Test
    void createEndpointRejectsMalformedAndMissingRequiredRequestData()
            throws Exception {
        String authorization = token("BRANCH_MANAGER", BRANCH_ID);

        mockMvc.perform(post("/api/v1/daily-raw-material-usages")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/daily-raw-material-usages")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        assertEquals(0, usageRepository.count());
    }

    @Test
    void insufficientStockRollsBackEveryBalanceAndMovementChange()
            throws Exception {
        createBalance(branchOne, itemOne, "5");
        createBalance(branchOne, itemTwo, "1");
        long usageId = createUsage(
                "BRANCH_MANAGER",
                BRANCH_ID,
                itemOne.getId(),
                "3",
                itemTwo.getId(),
                "2"
        );

        mockMvc.perform(post(
                        "/api/v1/daily-raw-material-usages/{id}/post",
                        usageId
                )
                        .header("Authorization", token("BRANCH_MANAGER", BRANCH_ID)))
                .andExpect(status().isBadRequest());

        assertEquals(new BigDecimal("5.000"),
                stockQuantity("BR001", itemOne));
        assertEquals(new BigDecimal("1.000"),
                stockQuantity("BR001", itemTwo));
        assertEquals(0, stockMovementRepository.count());
        assertEquals(DailyRawMaterialUsageStatus.DRAFT,
                usageRepository.findById(usageId).orElseThrow().getStatus());
    }

    @Test
    void jwtRoleAndBranchAuthorizationAreEnforced() throws Exception {
        mockMvc.perform(post("/api/v1/daily-raw-material-usages")
                        .header("Authorization", token("OWNER", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(itemOne.getId(), "1")))
                .andExpect(status().isForbidden());

        long usageId = saveUsage(branchTwo, itemOne, "1");
        mockMvc.perform(get("/api/v1/daily-raw-material-usages/{id}", usageId)
                        .header("Authorization",
                                token("BRANCH_MANAGER", BRANCH_ID)))
                .andExpect(status().isForbidden());

        String updateBody = """
                {
                  "remarks": "should be denied",
                  "items": [{"inventoryItemId": %d, "usedQuantity": 2}]
                }
                """.formatted(itemOne.getId());
        mockMvc.perform(put("/api/v1/daily-raw-material-usages/{id}", usageId)
                        .header("Authorization",
                                token("MAIN_OFFICE_MANAGER", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(
                        "/api/v1/daily-raw-material-usages/{id}/post",
                        usageId
                )
                        .header("Authorization",
                                token("MAIN_OFFICE_MANAGER", null)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/daily-raw-material-usages")
                        .header("Authorization", token("OWNER", null)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/daily-raw-material-usages")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void dateStatusBranchAndPaginationFiltersReturnExpectedRecords()
            throws Exception {
        long first = saveUsage(
                branchOne,
                itemOne,
                "1",
                LocalDate.of(2026, 1, 5),
                DailyRawMaterialUsageStatus.DRAFT
        );
        long second = saveUsage(
                branchOne,
                itemTwo,
                "1",
                LocalDate.of(2026, 1, 12),
                DailyRawMaterialUsageStatus.POSTED
        );
        saveUsage(
                branchTwo,
                itemOne,
                "1",
                LocalDate.of(2026, 1, 18),
                DailyRawMaterialUsageStatus.DRAFT
        );

        mockMvc.perform(get("/api/v1/daily-raw-material-usages/branch/BR001")
                        .header("Authorization", token("OWNER", null))
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(second));

        mockMvc.perform(get(
                        "/api/v1/daily-raw-material-usages/branch/BR001/date-range"
                )
                        .header("Authorization", token("MAIN_OFFICE_MANAGER", null))
                        .param("startDate", "2026-01-10")
                        .param("endDate", "2026-01-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(second));

        mockMvc.perform(get(
                        "/api/v1/daily-raw-material-usages/status/POSTED"
                )
                        .header("Authorization", token("OWNER", null))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(second));

        assertEquals(DailyRawMaterialUsageStatus.DRAFT,
                usageRepository.findById(first).orElseThrow().getStatus());
    }

    @Test
    void concurrentPostingDeductsStockAndWritesMovementOnlyOnce()
            throws Exception {
        createBalance(branchOne, itemOne, "10");
        long usageId = createUsage(
                "BRANCH_MANAGER",
                BRANCH_ID,
                itemOne.getId(),
                "4"
        );
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
                                    "/api/v1/daily-raw-material-usages/{id}/post",
                                    usageId
                            )
                                    .header("Authorization",
                                            token("BRANCH_MANAGER", BRANCH_ID)))
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
            assertEquals(DailyRawMaterialUsageStatus.POSTED,
                    usageRepository.findById(usageId).orElseThrow().getStatus());
        } finally {
            callers.shutdownNow();
        }
    }

    private long createUsage(
            String role,
            Long branchId,
            Long itemId,
            String quantity,
            Object... additionalItems
    ) throws Exception {
        StringBuilder body = new StringBuilder("{\"usageDate\":\"")
                .append(LocalDate.now())
                .append("\",\"remarks\":\"test\",\"items\":[")
                .append(itemJson(itemId, quantity));
        for (int i = 0; i < additionalItems.length; i += 2) {
            body.append(",")
                    .append(itemJson(
                            (Long) additionalItems[i],
                            (String) additionalItems[i + 1]
                    ));
        }
        body.append("]}");

        MvcResult result = mockMvc.perform(
                        post("/api/v1/daily-raw-material-usages")
                                .header("Authorization", token(role, branchId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body.toString())
                )
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode response = objectMapper.readTree(
                result.getResponse().getContentAsString()
        );
        return response.get("id").asLong();
    }

    private long saveUsage(
            StockLocation branch,
            InventoryItem item,
            String quantity
    ) {
        return saveUsage(
                branch,
                item,
                quantity,
                LocalDate.of(2026, 1, 5),
                DailyRawMaterialUsageStatus.DRAFT
        );
    }

    private long saveUsage(
            StockLocation branch,
            InventoryItem item,
            String quantity,
            LocalDate date,
            DailyRawMaterialUsageStatus status
    ) {
        DailyRawMaterialUsage usage = new DailyRawMaterialUsage(
                branch,
                date,
                ACCOUNT_ID,
                "filter fixture"
        );
        usage.addItem(item, new BigDecimal(quantity));
        DailyRawMaterialUsage saved = usageRepository.saveAndFlush(usage);
        if (status == DailyRawMaterialUsageStatus.POSTED) {
            saved.post(ACCOUNT_ID);
            saved = usageRepository.saveAndFlush(saved);
        }
        return saved.getId();
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

    private BigDecimal stockQuantity(String locationCode, InventoryItem item) {
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

    private String itemJson(Long itemId, String quantity) {
        return "{\"inventoryItemId\":" + itemId
                + ",\"usedQuantity\":" + quantity + "}";
    }

    private String createBody(Long itemId, String quantity) {
        return "{\"usageDate\":\"" + LocalDate.now()
                + "\",\"items\":[" + itemJson(itemId, quantity) + "]}";
    }

    private String token(String role, Long branchId) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .subject(Long.toString(ACCOUNT_ID))
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
