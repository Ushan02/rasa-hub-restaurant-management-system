package com.rasahub.product_inventory_service.security;

import com.rasahub.product_inventory_service.entity.InventoryItem;
import com.rasahub.product_inventory_service.entity.StockLocation;
import com.rasahub.product_inventory_service.enums.InventoryItemType;
import com.rasahub.product_inventory_service.enums.MeasurementUnit;
import com.rasahub.product_inventory_service.enums.StockLocationType;
import com.rasahub.product_inventory_service.repository.InventoryItemRepository;
import com.rasahub.product_inventory_service.repository.StockLocationRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class JwtSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StockLocationRepository stockLocationRepository;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Test
    void realSignedJwtPassesFilterAndRoleRulesAreEnforced()
            throws Exception {
        stockLocationRepository.saveAndFlush(
                new StockLocation(
                        "Main",
                        "Test Main Warehouse",
                        StockLocationType.MAIN_WAREHOUSE
                )
        );

        String owner = signedToken("101", "OWNER", null, Map.of());
        String manager = signedToken("102", "MAIN_OFFICE_MANAGER", null, Map.of());
        String stockKeeper = signedToken("103", "STOCK_KEEPER", null, Map.of());
        String accountant = signedToken("104", "ACCOUNTANT", null, Map.of());
        String branchManager = signedToken("105", "BRANCH_MANAGER", 7L, Map.of());

        mockMvc.perform(get("/api/v1/stock-locations")
                        .header("Authorization", bearer(owner)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/stock-locations")
                        .header("Authorization", bearer(manager)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/stock-locations")
                        .header("Authorization", bearer(stockKeeper)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/stock-locations")
                        .header("Authorization", bearer(accountant)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/stock-locations")
                        .header("Authorization", bearer(branchManager)))
                .andExpect(status().isOk());

        String locationUpdate = """
                {"locationName":"Updated Test Warehouse"}
                """;
        mockMvc.perform(patch("/api/v1/stock-locations/Main")
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(locationUpdate))
                .andExpect(status().isOk());

        for (String token : new String[]{stockKeeper, accountant, branchManager}) {
            mockMvc.perform(patch("/api/v1/stock-locations/Main")
                            .header("Authorization", bearer(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(locationUpdate))
                    .andExpect(status().isForbidden());
        }

        mockMvc.perform(patch("/api/v1/stock-locations/Main")
                        .header("Authorization", bearer(manager))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"locationName":"Updated Test Warehouse"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void missingAndInvalidBearerTokensAreUnauthorized()
            throws Exception {
        mockMvc.perform(get("/api/v1/stock-locations"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/stock-locations")
                        .header("Authorization", "Bearer malformed-token"))
                .andExpect(status().isUnauthorized());

        String expired = Jwts.builder()
                .subject("101")
                .issuedAt(Date.from(Instant.now().minusSeconds(120)))
                .expiration(Date.from(Instant.now().minusSeconds(60)))
                .claim("role", "OWNER")
                .signWith(signingKey(jwtSecret))
                .compact();
        mockMvc.perform(get("/api/v1/stock-locations")
                        .header("Authorization", bearer(expired)))
                .andExpect(status().isUnauthorized());

        String invalidSignature = signedToken(
                "101",
                "OWNER",
                null,
                Map.of(),
                "different-test-signing-key-with-at-least-32-bytes"
        );
        mockMvc.perform(get("/api/v1/stock-locations")
                        .header("Authorization", bearer(invalidSignature)))
                .andExpect(status().isUnauthorized());

        String valid = signedToken("101", "OWNER", null, Map.of());
        String[] parts = valid.split("\\.");
        parts[1] = (parts[1].charAt(0) == 'A' ? "B" : "A")
                + parts[1].substring(1);
        mockMvc.perform(get("/api/v1/stock-locations")
                        .header("Authorization", bearer(String.join(".", parts))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void missingOrMalformedAccountClaimsAndUnknownRoleCannotAccessProtectedApi()
            throws Exception {
        String missingSubject = Jwts.builder()
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusSeconds(600)))
                .claim("role", "OWNER")
                .signWith(signingKey(jwtSecret))
                .compact();
        mockMvc.perform(get("/api/v1/stock-locations")
                        .header("Authorization", bearer(missingSubject)))
                .andExpect(status().isUnauthorized());

        String malformedSubject = signedToken(
                "not-an-account-id",
                "OWNER",
                null,
                Map.of()
        );
        mockMvc.perform(get("/api/v1/stock-locations")
                        .header("Authorization", bearer(malformedSubject)))
                .andExpect(status().isUnauthorized());

        String missingRole = Jwts.builder()
                .subject("101")
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusSeconds(600)))
                .signWith(signingKey(jwtSecret))
                .compact();
        mockMvc.perform(get("/api/v1/stock-locations")
                        .header("Authorization", bearer(missingRole)))
                .andExpect(status().isUnauthorized());

        String missingExpiration = Jwts.builder()
                .subject("101")
                .issuedAt(new Date())
                .claim("role", "OWNER")
                .signWith(signingKey(jwtSecret))
                .compact();
        mockMvc.perform(get("/api/v1/stock-locations")
                        .header("Authorization", bearer(missingExpiration)))
                .andExpect(status().isUnauthorized());

        String unknownRole = signedToken(
                "101",
                "NOT_A_RASA_ROLE",
                null,
                Map.of()
        );
        mockMvc.perform(post("/api/v1/stock-adjustments")
                        .header("Authorization", bearer(unknownRole))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"locationCode":"Main","adjustmentReason":"FOUND","direction":"IN","items":[{"inventoryItemId":1,"adjustmentQuantity":1.000}]}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void alertReportAndAuditEndpointsDenyUnsupportedRoles()
            throws Exception {
        String unknownRole = signedToken(
                "101",
                "NOT_A_RASA_ROLE",
                null,
                Map.of()
        );
        String authorization = bearer(unknownRole);

        mockMvc.perform(get("/api/v1/low-stock-alerts/location/Main")
                        .header("Authorization", authorization))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/inventory-reports/stock-balances")
                        .header("Authorization", authorization))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/audit-history")
                        .header("Authorization", authorization))
                .andExpect(status().isForbidden());
    }

    @Test
    void alertReportAndAuditEndpointsRejectInvalidRequestFilters()
            throws Exception {
        String authorization = bearer(signedToken(
                "101",
                "OWNER",
                null,
                Map.of()
        ));

        mockMvc.perform(patch("/api/v1/low-stock-alerts/1/acknowledge")
                        .header("Authorization", authorization)
                        .param("remarks", "x".repeat(501)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/inventory-reports/stock-balances")
                        .header("Authorization", authorization)
                        .param("size", "101"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/inventory-reports/stock-movements")
                        .header("Authorization", authorization)
                        .param("movementType", "NOT_A_MOVEMENT_TYPE"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/audit-history")
                        .header("Authorization", authorization)
                        .param("entityType", "invalid-type"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/audit-history")
                        .header("Authorization", authorization)
                        .param("size", "101"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void actorIdentityComesFromSignedSubjectNotClientSuppliedAccountId()
            throws Exception {
        StockLocation main = stockLocationRepository.saveAndFlush(
                new StockLocation(
                        "Main",
                        "Test Main Warehouse",
                        StockLocationType.MAIN_WAREHOUSE
                )
        );
        InventoryItem item = inventoryItemRepository.saveAndFlush(
                new InventoryItem(
                        "TST001",
                        "Test Raw Material",
                        null,
                        InventoryItemType.RAW_MATERIAL,
                        MeasurementUnit.KG
                )
        );

        String token = signedToken(
                "314",
                "STOCK_KEEPER",
                null,
                Map.of("accountId", 999L)
        );
        String request = """
                {
                  "locationCode": "Main",
                  "adjustmentReason": "FOUND",
                  "direction": "IN",
                  "createdByAccountId": 999,
                  "items": [
                    {"inventoryItemId": %d, "adjustmentQuantity": 1.000}
                  ]
                }
                """.formatted(item.getId());

        mockMvc.perform(post("/api/v1/stock-adjustments")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.createdByAccountId").value(314));
    }

    private String signedToken(
            String subject,
            String role,
            Long branchId,
            Map<String, Object> extraClaims
    ) {
        return signedToken(subject, role, branchId, extraClaims, jwtSecret);
    }

    private String signedToken(
            String subject,
            String role,
            Long branchId,
            Map<String, Object> extraClaims,
            String secret
    ) {
        var builder = Jwts.builder()
                .subject(subject)
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusSeconds(600)))
                .claim("role", role);
        if (branchId != null) {
            builder.claim("branchId", branchId);
        }
        extraClaims.forEach(builder::claim);
        return builder.signWith(signingKey(secret)).compact();
    }

    private SecretKey signingKey(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
