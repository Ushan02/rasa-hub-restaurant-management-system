package com.rasahub.product_inventory_service.stocktransfer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rasahub.product_inventory_service.entity.BranchStockRequest;
import com.rasahub.product_inventory_service.entity.InventoryItem;
import com.rasahub.product_inventory_service.entity.StockBalance;
import com.rasahub.product_inventory_service.entity.StockLocation;
import com.rasahub.product_inventory_service.entity.StockMovement;
import com.rasahub.product_inventory_service.entity.StockTransfer;
import com.rasahub.product_inventory_service.enums.BranchStockRequestStatus;
import com.rasahub.product_inventory_service.enums.InventoryItemType;
import com.rasahub.product_inventory_service.enums.MeasurementUnit;
import com.rasahub.product_inventory_service.enums.StockLocationType;
import com.rasahub.product_inventory_service.enums.StockMovementType;
import com.rasahub.product_inventory_service.enums.StockTransferStatus;
import com.rasahub.product_inventory_service.repository.BranchStockRequestRepository;
import com.rasahub.product_inventory_service.repository.InventoryItemRepository;
import com.rasahub.product_inventory_service.repository.StockBalanceRepository;
import com.rasahub.product_inventory_service.repository.StockLocationRepository;
import com.rasahub.product_inventory_service.repository.StockMovementRepository;
import com.rasahub.product_inventory_service.repository.StockTransferRepository;
import com.sun.net.httpserver.HttpServer;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BranchStockTransferIntegrationTest {

    private static final String JWT_SECRET =
            "test-secret-key-for-daily-usage-module-tests-2026";
    private static HttpServer branchServer;
    private static ExecutorService branchServerExecutor;

    @Autowired
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private StockTransferRepository transferRepository;
    @Autowired
    private BranchStockRequestRepository requestRepository;
    @Autowired
    private StockMovementRepository movementRepository;
    @Autowired
    private StockBalanceRepository balanceRepository;
    @Autowired
    private InventoryItemRepository inventoryItemRepository;
    @Autowired
    private StockLocationRepository locationRepository;

    private StockLocation main;
    private StockLocation branchOne;
    private StockLocation branchTwo;
    private InventoryItem itemOne;
    private InventoryItem itemTwo;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        startBranchServer();
        registry.add(
                "spring.datasource.url",
                () -> "jdbc:h2:mem:branch_stock_transfer_workflow;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
        );
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add(
                "spring.datasource.driver-class-name",
                () -> "org.h2.Driver"
        );
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
        transferRepository.deleteAll();
        requestRepository.deleteAll();
        movementRepository.deleteAll();
        balanceRepository.deleteAll();
        inventoryItemRepository.deleteAll();
        locationRepository.deleteAll();

        branchOne = locationRepository.saveAndFlush(
                new StockLocation(
                        "BR001",
                        "Branch One",
                        StockLocationType.BRANCH
                )
        );
        branchTwo = locationRepository.saveAndFlush(
                new StockLocation(
                        "BR002",
                        "Branch Two",
                        StockLocationType.BRANCH
                )
        );
        main = locationRepository.saveAndFlush(
                new StockLocation(
                        "Main",
                        "Main Warehouse",
                        StockLocationType.MAIN_WAREHOUSE
                )
        );
        itemOne = inventoryItemRepository.saveAndFlush(item("T4TRF01"));
        itemTwo = inventoryItemRepository.saveAndFlush(item("T4TRF02"));
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
    void approvedBranchRequestMovesStockOnlyFromMainAtDispatchAndToBranchAtReceipt()
            throws Exception {
        long requestId = createBranchRequest(
                "BR001",
                501L,
                itemOne.getId(),
                "5"
        );

        mockMvc.perform(post("/api/v1/branch-stock-requests")
                        .header("Authorization", token(
                                501L,
                                "BRANCH_MANAGER",
                                7L
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(branchRequestBody(
                                "BR002",
                                itemOne.getId(),
                                "5"
                        )))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch(
                        "/api/v1/branch-stock-requests/{id}/approve",
                        requestId
                )
                        .header("Authorization", token(
                                601L,
                                "MAIN_OFFICE_MANAGER",
                                null
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.reviewedByAccountId").value(601));

        saveBalance(main, itemOne, "12");
        long transferId = createTransfer(
                requestId,
                701L,
                itemOne.getId(),
                "5"
        );

        mockMvc.perform(patch(
                        "/api/v1/stock-transfers/{id}/dispatch",
                        transferId
                )
                        .header("Authorization", token(
                                702L,
                                "STOCK_KEEPER",
                                null
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISPATCHED"))
                .andExpect(jsonPath("$.dispatchedByAccountId").value(702));

        assertEquals(new BigDecimal("7.000"), quantity("Main", itemOne));
        assertFalse(balanceRepository
                .findByStockLocation_LocationCodeAndInventoryItem_Id(
                        "BR001",
                        itemOne.getId()
                )
                .isPresent());
        assertEquals(1, movementRepository
                .findByReferenceTypeAndReferenceIdOrderByIdAsc(
                        "STOCK_TRANSFER",
                        Long.toString(transferId)
                )
                .size());
        assertEquals(
                StockMovementType.TRANSFER_OUT,
                movementRepository.findByReferenceTypeAndReferenceIdOrderByIdAsc(
                        "STOCK_TRANSFER",
                        Long.toString(transferId)
                ).get(0).getMovementType()
        );

        mockMvc.perform(patch(
                        "/api/v1/stock-transfers/{id}/dispatch",
                        transferId
                )
                        .header("Authorization", token(
                                702L,
                                "STOCK_KEEPER",
                                null
                        )))
                .andExpect(status().isBadRequest());

        mockMvc.perform(patch(
                        "/api/v1/stock-transfers/{id}/receive",
                        transferId
                )
                        .header("Authorization", token(
                                703L,
                                "BRANCH_MANAGER",
                                8L
                        )))
                .andExpect(status().isForbidden());
        assertFalse(balanceRepository
                .findByStockLocation_LocationCodeAndInventoryItem_Id(
                        "BR002",
                        itemOne.getId()
                )
                .isPresent());

        mockMvc.perform(patch(
                        "/api/v1/stock-transfers/{id}/receive",
                        transferId
                )
                        .header("Authorization", token(
                                704L,
                                "BRANCH_MANAGER",
                                7L
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andExpect(jsonPath("$.receivedByAccountId").value(704));

        assertEquals(new BigDecimal("7.000"), quantity("Main", itemOne));
        assertEquals(new BigDecimal("5.000"), quantity("BR001", itemOne));
        assertEquals(
                BranchStockRequestStatus.FULFILLED,
                requestRepository.findWithItemsById(requestId)
                        .orElseThrow()
                        .getStatus()
        );
        assertEquals(
                new BigDecimal("5.000"),
                requestRepository.findWithItemsById(requestId)
                        .orElseThrow()
                        .getItems().get(0).getReceivedQuantity()
        );
        assertEquals(2, movementRepository
                .findByReferenceTypeAndReferenceIdOrderByIdAsc(
                        "STOCK_TRANSFER",
                        Long.toString(transferId)
                )
                .size());
        StockMovement transferIn = movementRepository
                .findByReferenceTypeAndReferenceIdOrderByIdAsc(
                        "STOCK_TRANSFER",
                        Long.toString(transferId)
                )
                .stream()
                .filter(movement -> movement.getMovementType()
                        == StockMovementType.TRANSFER_IN)
                .findFirst()
                .orElseThrow();
        assertEquals(new BigDecimal("5.000"), transferIn.getQuantity());
        assertEquals(new BigDecimal("5.000"), transferIn.getQuantityAfter());

        mockMvc.perform(patch(
                        "/api/v1/stock-transfers/{id}/receive",
                        transferId
                )
                        .header("Authorization", token(
                                704L,
                                "BRANCH_MANAGER",
                                7L
                        )))
                .andExpect(status().isBadRequest());
        assertEquals(new BigDecimal("5.000"), quantity("BR001", itemOne));
        assertEquals(
                StockTransferStatus.RECEIVED,
                transferRepository.findById(transferId)
                        .orElseThrow()
                        .getStatus()
        );
    }

    @Test
    void branchStockRequestRejectionRequiresPendingStateAndReason()
            throws Exception {
        long requestId = createBranchRequest(
                "BR001",
                801L,
                itemOne.getId(),
                "3"
        );

        mockMvc.perform(patch(
                        "/api/v1/branch-stock-requests/{id}/reject",
                        requestId
                )
                        .header("Authorization", token(
                                802L,
                                "OWNER",
                                null
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rejectionReason\":\"  \"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(patch(
                        "/api/v1/branch-stock-requests/{id}/reject",
                        requestId
                )
                        .header("Authorization", token(
                                802L,
                                "OWNER",
                                null
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rejectionReason\":\"Branch allocation deferred\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.reviewedByAccountId").value(802))
                .andExpect(jsonPath("$.rejectionReason")
                        .value("Branch allocation deferred"));

        BranchStockRequest rejected = requestRepository.findById(requestId)
                .orElseThrow();
        assertEquals(BranchStockRequestStatus.REJECTED, rejected.getStatus());
        assertEquals(802L, rejected.getReviewedByAccountId());
        assertEquals(
                "Branch allocation deferred",
                rejected.getRejectionReason()
        );

        mockMvc.perform(patch(
                        "/api/v1/branch-stock-requests/{id}/approve",
                        requestId
                )
                        .header("Authorization", token(
                                803L,
                                "OWNER",
                                null
                        )))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch(
                        "/api/v1/branch-stock-requests/{id}/reject",
                        requestId
                )
                        .header("Authorization", token(
                                803L,
                                "OWNER",
                                null
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rejectionReason\":\"Duplicate review\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void failedMultiItemDispatchRollsBackEarlierBalanceAndMovementWrites()
            throws Exception {
        long requestId = createBranchRequest(
                "BR001",
                901L,
                itemOne.getId(),
                "4",
                itemTwo.getId(),
                "4"
        );
        mockMvc.perform(patch(
                        "/api/v1/branch-stock-requests/{id}/approve",
                        requestId
                )
                        .header("Authorization", token(
                                902L,
                                "OWNER",
                                null
                        )))
                .andExpect(status().isOk());

        saveBalance(main, itemOne, "8");
        saveBalance(main, itemTwo, "0");
        long transferId = createTransfer(
                requestId,
                903L,
                itemOne.getId(),
                "4",
                itemTwo.getId(),
                "4"
        );

        mockMvc.perform(patch(
                        "/api/v1/stock-transfers/{id}/dispatch",
                        transferId
                )
                        .header("Authorization", token(
                                904L,
                                "STOCK_KEEPER",
                                null
                        )))
                .andExpect(status().isBadRequest());

        assertEquals(new BigDecimal("8.000"), quantity("Main", itemOne));
        assertEquals(BigDecimal.ZERO.setScale(3), quantity("Main", itemTwo));
        assertEquals(0, movementRepository
                .findByReferenceTypeAndReferenceIdOrderByIdAsc(
                        "STOCK_TRANSFER",
                        Long.toString(transferId)
                )
                .size());
        StockTransfer transfer = transferRepository.findById(transferId)
                .orElseThrow();
        assertEquals(StockTransferStatus.DRAFT, transfer.getStatus());
        assertNull(transfer.getDispatchedByAccountId());
    }

    private long createBranchRequest(
            String branchCode,
            long accountId,
            Long itemId,
            String quantity,
            Object... additionalItemAndQuantity
    ) throws Exception {
        String body = branchRequestBody(
                branchCode,
                itemId,
                quantity,
                additionalItemAndQuantity
        );
        MvcResult result = mockMvc.perform(post(
                        "/api/v1/branch-stock-requests"
                )
                        .header("Authorization", token(
                                accountId,
                                "BRANCH_MANAGER",
                                7L
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();
        return idFrom(result);
    }

    private String branchRequestBody(
            String branchCode,
            Long itemId,
            String quantity,
            Object... additionalItemAndQuantity
    ) {
        StringBuilder items = new StringBuilder(
                "{\"inventoryItemId\":%d,\"requestedQuantity\":%s}"
                        .formatted(itemId, quantity)
        );
        for (int i = 0; i < additionalItemAndQuantity.length; i += 2) {
            items.append(",{\"inventoryItemId\":%s,\"requestedQuantity\":%s}"
                    .formatted(
                            additionalItemAndQuantity[i],
                            additionalItemAndQuantity[i + 1]
                    ));
        }
        return """
                {
                  "branchLocationCode": "%s",
                  "remarks": "T4 branch request",
                  "items": [%s]
                }
                """.formatted(branchCode, items);
    }

    private long createTransfer(
            long requestId,
            long accountId,
            Long itemId,
            String quantity,
            Object... additionalItemAndQuantity
    ) throws Exception {
        StringBuilder items = new StringBuilder(
                "{\"inventoryItemId\":%d,\"transferQuantity\":%s}"
                        .formatted(itemId, quantity)
        );
        for (int i = 0; i < additionalItemAndQuantity.length; i += 2) {
            items.append(",{\"inventoryItemId\":%s,\"transferQuantity\":%s}"
                    .formatted(
                            additionalItemAndQuantity[i],
                            additionalItemAndQuantity[i + 1]
                    ));
        }
        String body = """
                {
                  "branchStockRequestId": %d,
                  "items": [%s]
                }
                """.formatted(requestId, items);
        MvcResult result = mockMvc.perform(post("/api/v1/stock-transfers")
                        .header("Authorization", token(
                                accountId,
                                "STOCK_KEEPER",
                                null
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn();
        return idFrom(result);
    }

    private void saveBalance(
            StockLocation location,
            InventoryItem inventoryItem,
            String quantity
    ) {
        StockBalance balance = new StockBalance(location, inventoryItem);
        BigDecimal amount = new BigDecimal(quantity);
        if (amount.signum() > 0) {
            balance.increaseQuantity(amount);
        }
        balanceRepository.saveAndFlush(balance);
    }

    private BigDecimal quantity(String locationCode, InventoryItem inventoryItem) {
        return balanceRepository
                .findByStockLocation_LocationCodeAndInventoryItem_Id(
                        locationCode,
                        inventoryItem.getId()
                )
                .orElseThrow()
                .getQuantityOnHand();
    }

    private InventoryItem item(String code) {
        return new InventoryItem(
                code,
                "T4 transfer item " + code,
                null,
                InventoryItemType.RAW_MATERIAL,
                MeasurementUnit.KG
        );
    }

    private long idFrom(MvcResult result) throws Exception {
        JsonNode response = objectMapper.readTree(
                result.getResponse().getContentAsString()
        );
        return response.get("id").asLong();
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
                boolean branchOne = exchange.getRequestURI()
                        .getPath()
                        .endsWith("/7");
                String body = branchOne
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
