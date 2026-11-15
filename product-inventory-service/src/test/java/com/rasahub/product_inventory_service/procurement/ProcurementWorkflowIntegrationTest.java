package com.rasahub.product_inventory_service.procurement;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rasahub.product_inventory_service.entity.GoodsReceiptNote;
import com.rasahub.product_inventory_service.entity.InventoryItem;
import com.rasahub.product_inventory_service.entity.PurchaseOrder;
import com.rasahub.product_inventory_service.entity.PurchaseRequest;
import com.rasahub.product_inventory_service.entity.StockBalance;
import com.rasahub.product_inventory_service.entity.StockLocation;
import com.rasahub.product_inventory_service.entity.StockMovement;
import com.rasahub.product_inventory_service.entity.Supplier;
import com.rasahub.product_inventory_service.entity.SupplierItem;
import com.rasahub.product_inventory_service.enums.InventoryItemType;
import com.rasahub.product_inventory_service.enums.MeasurementUnit;
import com.rasahub.product_inventory_service.enums.PurchaseOrderStatus;
import com.rasahub.product_inventory_service.enums.PurchaseRequestStatus;
import com.rasahub.product_inventory_service.enums.StockLocationType;
import com.rasahub.product_inventory_service.enums.StockMovementType;
import com.rasahub.product_inventory_service.repository.GoodsReceiptNoteRepository;
import com.rasahub.product_inventory_service.repository.InventoryItemRepository;
import com.rasahub.product_inventory_service.repository.PurchaseOrderRepository;
import com.rasahub.product_inventory_service.repository.PurchaseRequestRepository;
import com.rasahub.product_inventory_service.repository.StockBalanceRepository;
import com.rasahub.product_inventory_service.repository.StockLocationRepository;
import com.rasahub.product_inventory_service.repository.StockMovementRepository;
import com.rasahub.product_inventory_service.repository.SupplierItemRepository;
import com.rasahub.product_inventory_service.repository.SupplierRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
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
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProcurementWorkflowIntegrationTest {

    private static final String JWT_SECRET =
            "test-secret-key-for-daily-usage-module-tests-2026";

    @Autowired
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private PurchaseRequestRepository purchaseRequestRepository;
    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;
    @Autowired
    private GoodsReceiptNoteRepository goodsReceiptNoteRepository;
    @Autowired
    private SupplierItemRepository supplierItemRepository;
    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private StockMovementRepository stockMovementRepository;
    @Autowired
    private StockBalanceRepository stockBalanceRepository;
    @Autowired
    private InventoryItemRepository inventoryItemRepository;
    @Autowired
    private StockLocationRepository stockLocationRepository;

    private InventoryItem item;
    private Supplier supplier;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.datasource.url",
                () -> "jdbc:h2:mem:procurement_workflow;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
        );
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add(
                "spring.datasource.driver-class-name",
                () -> "org.h2.Driver"
        );
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("jwt.secret", () -> JWT_SECRET);
    }

    @BeforeEach
    void setUp() {
        goodsReceiptNoteRepository.deleteAll();
        purchaseOrderRepository.deleteAll();
        purchaseRequestRepository.deleteAll();
        supplierItemRepository.deleteAll();
        supplierRepository.deleteAll();
        stockMovementRepository.deleteAll();
        stockBalanceRepository.deleteAll();
        inventoryItemRepository.deleteAll();
        stockLocationRepository.deleteAll();

        stockLocationRepository.saveAndFlush(
                new StockLocation(
                        "Main",
                        "Main Warehouse",
                        StockLocationType.MAIN_WAREHOUSE
                )
        );
        item = inventoryItemRepository.saveAndFlush(
                new InventoryItem(
                        "T4RAW01",
                        "T4 procurement raw material",
                        null,
                        InventoryItemType.RAW_MATERIAL,
                        MeasurementUnit.KG
                )
        );
        supplier = supplierRepository.saveAndFlush(
                new Supplier(
                        "T4SUP01",
                        "T4 procurement supplier",
                        "0112345678",
                        "t4-procurement@example.com",
                        "Test address"
                )
        );
        supplierItemRepository.saveAndFlush(
                new SupplierItem(
                        supplier,
                        item,
                        new BigDecimal("2.50"),
                        BigDecimal.ONE,
                        3
                )
        );
    }

    @Test
    void purchaseRequestsEnforceReviewActorAndTerminalTransitions()
            throws Exception {
        long approvableRequestId = createPurchaseRequest(101L);

        mockMvc.perform(patch(
                        "/api/v1/purchase-requests/{id}/approve",
                        approvableRequestId
                )
                        .header("Authorization", token(
                                303L,
                                "ACCOUNTANT",
                                null
                        )))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch(
                        "/api/v1/purchase-requests/{id}/approve",
                        approvableRequestId
                )
                        .header("Authorization", token(
                                101L,
                                "OWNER",
                                null
                        )))
                .andExpect(status().isBadRequest());
        assertEquals(
                PurchaseRequestStatus.PENDING,
                purchaseRequestRepository.findById(approvableRequestId)
                        .orElseThrow()
                        .getStatus()
        );

        mockMvc.perform(patch(
                        "/api/v1/purchase-requests/{id}/approve",
                        approvableRequestId
                )
                        .header("Authorization", token(
                                202L,
                                "MAIN_OFFICE_MANAGER",
                                null
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.reviewedByAccountId").value(202));

        PurchaseRequest approved = purchaseRequestRepository
                .findById(approvableRequestId)
                .orElseThrow();
        assertEquals(PurchaseRequestStatus.APPROVED, approved.getStatus());
        assertEquals(202L, approved.getReviewedByAccountId());
        assertTrue(approved.getReviewedAt() != null);

        mockMvc.perform(patch(
                        "/api/v1/purchase-requests/{id}/approve",
                        approvableRequestId
                )
                        .header("Authorization", token(
                                203L,
                                "OWNER",
                                null
                        )))
                .andExpect(status().isBadRequest());

        long rejectableRequestId = createPurchaseRequest(102L);
        mockMvc.perform(patch(
                        "/api/v1/purchase-requests/{id}/approve",
                        rejectableRequestId
                )
                        .header("Authorization", token(
                                102L,
                                "OWNER",
                                null
                        )))
                .andExpect(status().isBadRequest());

        mockMvc.perform(patch(
                        "/api/v1/purchase-requests/{id}/reject",
                        rejectableRequestId
                )
                        .header("Authorization", token(
                                204L,
                                "OWNER",
                                null
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rejectionReason\":\"Budget deferred\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.reviewedByAccountId").value(204))
                .andExpect(jsonPath("$.rejectionReason")
                        .value("Budget deferred"));

        PurchaseRequest rejected = purchaseRequestRepository
                .findById(rejectableRequestId)
                .orElseThrow();
        assertEquals(PurchaseRequestStatus.REJECTED, rejected.getStatus());
        assertEquals("Budget deferred", rejected.getRejectionReason());
        assertEquals(204L, rejected.getReviewedByAccountId());

        mockMvc.perform(patch(
                        "/api/v1/purchase-requests/{id}/approve",
                        rejectableRequestId
                )
                        .header("Authorization", token(
                                205L,
                                "OWNER",
                                null
                        )))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch(
                        "/api/v1/purchase-requests/{id}/reject",
                        rejectableRequestId
                )
                        .header("Authorization", token(
                                205L,
                                "OWNER",
                                null
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rejectionReason\":\"Second review\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void confirmsOnlyAcceptedGrnQuantityIntoMainStockAndPo()
            throws Exception {
        long requestId = createPurchaseRequest(301L);
        mockMvc.perform(patch(
                        "/api/v1/purchase-requests/{id}/approve",
                        requestId
                )
                        .header("Authorization", token(
                                302L,
                                "OWNER",
                                null
                        )))
                .andExpect(status().isOk());

        String purchaseOrderBody = """
                {
                  "purchaseRequestId": %d,
                  "supplierId": %d,
                  "expectedDeliveryDate": "%s",
                  "items": [{
                    "inventoryItemId": %d,
                    "orderedQuantity": 10,
                    "unitPrice": 2.50
                  }]
                }
                """.formatted(
                requestId,
                supplier.getId(),
                LocalDate.now().plusDays(1),
                item.getId()
        );
        MvcResult orderResult = mockMvc.perform(post(
                        "/api/v1/purchase-orders"
                )
                        .header("Authorization", token(
                                303L,
                                "OWNER",
                                null
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(purchaseOrderBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ISSUED"))
                .andReturn();
        long purchaseOrderId = idFrom(orderResult);

        String excessiveReceiptBody = """
                {
                  "purchaseOrderId": %d,
                  "receivedDate": "%s",
                  "items": [{
                    "inventoryItemId": %d,
                    "deliveredQuantity": 11,
                    "acceptedQuantity": 11,
                    "rejectedQuantity": 0
                  }]
                }
                """.formatted(
                purchaseOrderId,
                LocalDate.now(),
                item.getId()
        );
        mockMvc.perform(post("/api/v1/goods-receipts")
                        .header("Authorization", token(
                                401L,
                                "STOCK_KEEPER",
                                null
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(excessiveReceiptBody))
                .andExpect(status().isBadRequest());
        assertEquals(0, goodsReceiptNoteRepository.count());

        String receiptBody = """
                {
                  "purchaseOrderId": %d,
                  "receivedDate": "%s",
                  "deliveryReference": "T4-delivery",
                  "items": [{
                    "inventoryItemId": %d,
                    "deliveredQuantity": 7,
                    "acceptedQuantity": 5,
                    "rejectedQuantity": 2
                  }]
                }
                """.formatted(
                purchaseOrderId,
                LocalDate.now(),
                item.getId()
        );
        MvcResult receiptResult = mockMvc.perform(post(
                        "/api/v1/goods-receipts"
                )
                        .header("Authorization", token(
                                401L,
                                "STOCK_KEEPER",
                                null
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(receiptBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn();
        long receiptId = idFrom(receiptResult);

        mockMvc.perform(patch(
                        "/api/v1/goods-receipts/{id}/submit",
                        receiptId
                )
                        .header("Authorization", token(
                                401L,
                                "STOCK_KEEPER",
                                null
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"));

        mockMvc.perform(patch(
                        "/api/v1/goods-receipts/{id}/confirm",
                        receiptId
                )
                        .header("Authorization", token(
                                401L,
                                "OWNER",
                                null
                        )))
                .andExpect(status().isBadRequest());
        assertFalse(stockBalanceRepository
                .findByStockLocation_LocationCodeAndInventoryItem_Id(
                        "Main",
                        item.getId()
                )
                .isPresent());

        mockMvc.perform(patch(
                        "/api/v1/goods-receipts/{id}/confirm",
                        receiptId
                )
                        .header("Authorization", token(
                                402L,
                                "MAIN_OFFICE_MANAGER",
                                null
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        StockBalance mainBalance = stockBalanceRepository
                .findByStockLocation_LocationCodeAndInventoryItem_Id(
                        "Main",
                        item.getId()
                )
                .orElseThrow();
        assertEquals(new BigDecimal("5.000"), mainBalance.getQuantityOnHand());

        PurchaseOrder purchaseOrder = purchaseOrderRepository
                .findWithItemsById(purchaseOrderId)
                .orElseThrow();
        assertEquals(
                PurchaseOrderStatus.PARTIALLY_RECEIVED,
                purchaseOrder.getStatus()
        );
        assertEquals(
                new BigDecimal("5.000"),
                purchaseOrder.getItems().get(0).getReceivedQuantity()
        );

        GoodsReceiptNote receipt = goodsReceiptNoteRepository
                .findWithItemsById(receiptId)
                .orElseThrow();
        assertEquals(402L, receipt.getReviewedByAccountId());
        assertEquals(
                new BigDecimal("5.000"),
                receipt.getItems().get(0).getAcceptedQuantity()
        );
        assertEquals(1, stockMovementRepository
                .findByReferenceTypeAndReferenceIdOrderByIdAsc(
                        "GRN",
                        Long.toString(receiptId)
                )
                .size());
        StockMovement movement = stockMovementRepository
                .findByReferenceTypeAndReferenceIdOrderByIdAsc(
                        "GRN",
                        Long.toString(receiptId)
                )
                .get(0);
        assertEquals(StockMovementType.GOODS_RECEIPT, movement.getMovementType());
        assertEquals(new BigDecimal("5.000"), movement.getQuantity());
        assertEquals(new BigDecimal("5.000"), movement.getQuantityAfter());

        mockMvc.perform(patch(
                        "/api/v1/goods-receipts/{id}/confirm",
                        receiptId
                )
                        .header("Authorization", token(
                                403L,
                                "OWNER",
                                null
                        )))
                .andExpect(status().isBadRequest());
        assertEquals(
                new BigDecimal("5.000"),
                stockBalanceRepository
                        .findByStockLocation_LocationCodeAndInventoryItem_Id(
                                "Main",
                                item.getId()
                        )
                        .orElseThrow()
                        .getQuantityOnHand()
        );
    }

    private long createPurchaseRequest(long requesterId) throws Exception {
        String body = """
                {
                  "remarks": "T4 integration request",
                  "items": [{
                    "inventoryItemId": %d,
                    "requestedQuantity": 10
                  }]
                }
                """.formatted(item.getId());
        MvcResult result = mockMvc.perform(post(
                        "/api/v1/purchase-requests"
                )
                        .header("Authorization", token(
                                requesterId,
                                "STOCK_KEEPER",
                                null
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();
        return idFrom(result);
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
}
