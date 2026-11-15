package com.rasahub.product_inventory_service.crud;

import com.rasahub.product_inventory_service.service.SupabaseStorageService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockPart;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProductInventoryCrudIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private SupabaseStorageService supabaseStorageService;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @BeforeEach
    void createCodeSequencesForH2() {
        jdbcTemplate.execute("CREATE SEQUENCE IF NOT EXISTS category_code_seq START WITH 1");
        jdbcTemplate.execute("CREATE SEQUENCE IF NOT EXISTS product_code_seq START WITH 1");
        jdbcTemplate.execute("CREATE SEQUENCE IF NOT EXISTS inventory_item_code_seq START WITH 1");
        jdbcTemplate.execute("CREATE SEQUENCE IF NOT EXISTS supplier_code_seq START WITH 1");
        jdbcTemplate.execute("CREATE SEQUENCE IF NOT EXISTS variant_code_seq START WITH 1");
        jdbcTemplate.execute("CREATE SEQUENCE IF NOT EXISTS offer_code_seq START WITH 1");
    }

    @Test
    void categoryCrudValidatesNamesAndEnforcesWriteRole() throws Exception {
        String owner = bearer("OWNER");
        String cashier = bearer("CASHIER");

        mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", cashier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Unauthorized"}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"   "}
                                """))
                .andExpect(status().isBadRequest());

        MvcResult created = mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"  T3 CRUD Hot Drinks  ","description":"  Tea and coffee "}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("T3 CRUD Hot Drinks"))
                .andExpect(jsonPath("$.description").value("Tea and coffee"))
                .andReturn();

        String categoryCode = json(created, "categoryCode");
        String categoryId = json(created, "id");
        mockMvc.perform(get("/api/v1/categories/by-code/{code}", categoryCode.toLowerCase())
                        .header("Authorization", owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(Integer.parseInt(categoryId)));

        mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"t3 crud hot drinks"}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/api/v1/categories/{id}/status", categoryId)
                        .header("Authorization", owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"active":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
        mockMvc.perform(get("/api/v1/categories")
                        .header("Authorization", owner)
                        .param("active", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(Integer.parseInt(categoryId))));

        mockMvc.perform(put("/api/v1/categories/{id}", categoryId)
                        .header("Authorization", owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"T3 CRUD Beverages","description":"Cold and hot"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("T3 CRUD Beverages"));
    }

    @Test
    void productVariantAndOfferWorkflowsValidateRelationshipsAndOverlaps() throws Exception {
        String owner = bearer("OWNER");
        when(supabaseStorageService.uploadProductImage(anyString(), any(MultipartFile.class)))
                .thenReturn("https://storage.example/test/product.png");

        MvcResult category = mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"T3 CRUD Prepared Food"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String categoryId = json(category, "id");

        mockMvc.perform(patch("/api/v1/categories/{id}/status", categoryId)
                        .header("Authorization", owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"active":false}
                                """))
                .andExpect(status().isOk());

        MockPart productPart = new MockPart(
                "product",
                """
                        {"name":"  T3 CRUD Rice Bowl ","description":"Lunch","categoryId":%s}
                        """.formatted(categoryId).getBytes(StandardCharsets.UTF_8)
        );
        productPart.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        MockMultipartFile image = new MockMultipartFile(
                "image", "rice.png", "image/png", new byte[]{1, 2, 3}
        );
        mockMvc.perform(multipart("/api/v1/products")
                        .file(image)
                        .part(productPart)
                        .header("Authorization", owner))
                .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/api/v1/categories/{id}/status", categoryId)
                        .header("Authorization", owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"active":true}
                                """))
                .andExpect(status().isOk());

        MvcResult product = mockMvc.perform(multipart("/api/v1/products")
                        .file(image)
                        .part(productPart)
                        .header("Authorization", owner))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("T3 CRUD Rice Bowl"))
                .andExpect(jsonPath("$.categoryId").value(Integer.parseInt(categoryId)))
                .andReturn();
        String productId = json(product, "id");

        MockPart duplicateProduct = new MockPart(
                "product",
                """
                        {"name":"t3 crud rice bowl","categoryId":%s}
                        """.formatted(categoryId).getBytes(StandardCharsets.UTF_8)
        );
        duplicateProduct.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        mockMvc.perform(multipart("/api/v1/products")
                        .file(image)
                        .part(duplicateProduct)
                        .header("Authorization", owner))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/products")
                        .header("Authorization", owner)
                        .param("categoryId", categoryId)
                        .param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(Integer.parseInt(productId))));

        MockPart productUpdate = new MockPart(
                "product",
                """
                        {"name":"T3 CRUD Rice Bowl Revised","description":"Updated lunch","categoryId":%s}
                        """.formatted(categoryId).getBytes(StandardCharsets.UTF_8)
        );
        productUpdate.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        mockMvc.perform(multipart("/api/v1/products/{id}", productId)
                        .part(productUpdate)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .header("Authorization", owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("T3 CRUD Rice Bowl Revised"));

        mockMvc.perform(patch("/api/v1/products/{id}/status", productId)
                        .header("Authorization", owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"active":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        MvcResult activeProduct = mockMvc.perform(patch("/api/v1/products/{id}/status", productId)
                        .header("Authorization", owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"active":true}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        MvcResult branchProduct = mockMvc.perform(post("/api/v1/branch-products")
                        .header("Authorization", owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"branchId":701,"productId":%s}
                                """.formatted(json(activeProduct, "id"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.branchId").value(701))
                .andReturn();
        String branchProductId = json(branchProduct, "id");
        mockMvc.perform(post("/api/v1/branch-products")
                        .header("Authorization", owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"branchId":701,"productId":%s}
                                """.formatted(json(activeProduct, "id"))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/branch-products")
                        .header("Authorization", owner)
                        .param("branchId", "701")
                        .param("productId", json(activeProduct, "id")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(Integer.parseInt(branchProductId))));
        mockMvc.perform(patch("/api/v1/branch-products/{id}/availability", branchProductId)
                        .header("Authorization", owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"available":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false));

        MvcResult variant = mockMvc.perform(post("/api/v1/product-variants")
                        .header("Authorization", owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":%s,"name":"REGULAR","price":12.50}
                                """.formatted(json(activeProduct, "id"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("REGULAR"))
                .andReturn();
        String variantId = json(variant, "id");

        mockMvc.perform(post("/api/v1/product-variants")
                        .header("Authorization", owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":%s,"name":"REGULAR","price":12.50}
                                """.formatted(json(activeProduct, "id"))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/product-variants")
                        .header("Authorization", owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":999999,"name":"LARGE","price":12.50}
                                """))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/v1/product-variants/{id}", variantId)
                        .header("Authorization", owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"REGULAR","price":14.00}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(14.0));

        LocalDateTime start = LocalDateTime.now().plusDays(2).withNano(0);
        LocalDateTime end = start.plusDays(2);
        String offerBody = """
                {
                  "name":"Lunch special",
                  "productVariantId":%s,
                  "discountType":"PERCENTAGE",
                  "discountValue":10,
                  "startAt":"%s",
                  "endAt":"%s"
                }
                """.formatted(variantId, start, end);
        MvcResult offer = mockMvc.perform(post("/api/v1/offers")
                        .header("Authorization", owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(offerBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.variantCode").exists())
                .andReturn();
        String offerId = json(offer, "id");

        mockMvc.perform(put("/api/v1/offers/{id}", offerId)
                        .header("Authorization", owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Updated lunch special",
                                  "description":"Updated terms",
                                  "discountType":"PERCENTAGE",
                                  "discountValue":15,
                                  "startAt":"%s",
                                  "endAt":"%s"
                                }
                                """.formatted(start, end)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated lunch special"));

        mockMvc.perform(post("/api/v1/offers")
                        .header("Authorization", owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(offerBody.replace("Lunch special", "Overlapping offer")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/offers")
                        .header("Authorization", owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(offerBody.replace("\"discountValue\":10", "\"discountValue\":101")
                                .replace("Lunch special", "Invalid discount")))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/offers")
                        .header("Authorization", owner)
                        .param("productVariantId", variantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(Integer.parseInt(offerId)));
        mockMvc.perform(patch("/api/v1/offers/{id}/status", offerId)
                        .header("Authorization", owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"active":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void inventoryAndSupplierItemWorkflowsValidateInputsAndRelationships() throws Exception {
        String owner = bearer("OWNER");

        MvcResult item = mockMvc.perform(post("/api/v1/inventory-items")
                        .header("Authorization", owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itemName":"  T3 CRUD Basmati Rice ","itemType":"RAW_MATERIAL","measurementUnit":"KG"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.itemName").value("T3 CRUD Basmati Rice"))
                .andReturn();
        String itemId = json(item, "id");
        mockMvc.perform(get("/api/v1/inventory-items")
                        .header("Authorization", owner)
                        .param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(Integer.parseInt(itemId))));

        mockMvc.perform(post("/api/v1/inventory-items")
                        .header("Authorization", owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itemName":"t3 crud basmati rice","itemType":"RAW_MATERIAL","measurementUnit":"KG"}
                                """))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/inventory-items")
                        .header("Authorization", owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itemName":"Missing unit","itemType":"RAW_MATERIAL"}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/api/v1/inventory-items/{id}", itemId)
                        .header("Authorization", owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itemName":"  T3 CRUD Jasmine Rice "}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemName").value("T3 CRUD Jasmine Rice"));
        mockMvc.perform(patch("/api/v1/inventory-items/{id}", itemId)
                        .header("Authorization", owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"measurementUnit":"PCS"}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/api/v1/inventory-items/{id}/status", itemId)
                        .header("Authorization", owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"active":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        MvcResult supplier = mockMvc.perform(post("/api/v1/suppliers")
                        .header("Authorization", owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"supplierName":"T3 CRUD Island Foods","phoneNumber":"0771234567","email":"sales@example.test"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.supplierName").value("T3 CRUD Island Foods"))
                .andReturn();
        String supplierId = json(supplier, "id");
        mockMvc.perform(patch("/api/v1/suppliers/{id}", supplierId)
                        .header("Authorization", owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"supplierName":"T3 CRUD Island Foods Wholesale"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.supplierName").value("T3 CRUD Island Foods Wholesale"));

        mockMvc.perform(post("/api/v1/suppliers")
                        .header("Authorization", owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"supplierName":"Bad Phone","phoneNumber":"123","email":"not-an-email"}
                                """))
                .andExpect(status().isBadRequest());

        MvcResult activeItem = mockMvc.perform(patch("/api/v1/inventory-items/{id}/status", itemId)
                        .header("Authorization", owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"active":true}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        itemId = json(activeItem, "id");

        String mappingRequest = """
                {
                  "supplierId":%s,
                  "inventoryItemId":%s,
                  "unitPrice":25.75,
                  "minimumOrderQuantity":2.000,
                  "leadTimeDays":4
                }
                """.formatted(supplierId, itemId);
        MvcResult mapping = mockMvc.perform(post("/api/v1/supplier-items")
                        .header("Authorization", owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mappingRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.supplierId").value(Integer.parseInt(supplierId)))
                .andExpect(jsonPath("$.inventoryItemId").value(Integer.parseInt(itemId)))
                .andReturn();
        String mappingId = json(mapping, "id");

        mockMvc.perform(post("/api/v1/supplier-items")
                        .header("Authorization", owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mappingRequest))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/supplier-items")
                        .header("Authorization", owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mappingRequest.replace("\"unitPrice\":25.75", "\"unitPrice\":0")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/supplier-items/by-supplier-and-item")
                        .header("Authorization", owner)
                        .param("supplierId", supplierId)
                        .param("inventoryItemId", itemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(Integer.parseInt(mappingId)));
        mockMvc.perform(get("/api/v1/supplier-items/by-supplier/{id}", supplierId)
                        .header("Authorization", owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(Integer.parseInt(mappingId))));
        mockMvc.perform(patch("/api/v1/supplier-items/{id}", mappingId)
                        .header("Authorization", owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"unitPrice":27.00,"leadTimeDays":6}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unitPrice").value(27.0));
        mockMvc.perform(patch("/api/v1/supplier-items/{id}/status", mappingId)
                        .header("Authorization", owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"active":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
        mockMvc.perform(patch("/api/v1/suppliers/{id}/status", supplierId)
                        .header("Authorization", owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"active":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
        mockMvc.perform(post("/api/v1/supplier-items")
                        .header("Authorization", owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mappingRequest))
                .andExpect(status().isBadRequest());
    }

    private String bearer(String role) {
        SecretKey key = Keys.hmacShaKeyFor(
                jwtSecret.getBytes(StandardCharsets.UTF_8)
        );
        String token = Jwts.builder()
                .subject("701")
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusSeconds(600)))
                .claim("role", role)
                .signWith(key)
                .compact();
        return "Bearer " + token;
    }

    private String json(MvcResult result, String path) throws Exception {
        return new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(result.getResponse().getContentAsString())
                .at("/" + path.replace("[", "").replace("]", "").replace(".", "/"))
                .asText();
    }
}
