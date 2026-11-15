package com.rasahub.product_inventory_service.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rasahub.product_inventory_service.dto.audit.AuditLogResponse;
import com.rasahub.product_inventory_service.repository.AuditLogRepository;
import com.rasahub.product_inventory_service.security.ProductUserPrincipal;
import com.rasahub.product_inventory_service.service.AuditHistoryService;
import com.rasahub.product_inventory_service.service.AuditLogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class AuditHistoryIntegrationTest {

    private static final AtomicLong ENTITY_IDS = new AtomicLong(900_000);

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private AuditHistoryService auditHistoryService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private final ObjectMapper objectMapper =
            new ObjectMapper().registerModule(new JavaTimeModule());

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void recordsAuthenticatedAndSystemActorsAndReturnsFlatDtos() throws Exception {
        long entityId = ENTITY_IDS.incrementAndGet();
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            auditLogService.record(
                    "TEST_ENTITY",
                    entityId,
                    "UPDATED",
                    42L,
                    "DRAFT",
                    "APPROVED",
                    null,
                    null,
                    "Main"
            );
            auditLogService.record(
                    "TEST_ENTITY",
                    entityId,
                    "RECONCILED",
                    null,
                    "APPROVED",
                    "RECONCILED",
                    "System reconciliation",
                    null,
                    "Main"
            );
        });

        Page<AuditLogResponse> records = auditLogRepository.findReportRows(
                "TEST_ENTITY",
                entityId,
                null,
                null,
                null,
                null,
                null,
                PageRequest.of(0, 10)
        );

        assertEquals(2, records.getTotalElements());
        assertEquals(42L, records.getContent().get(1).actorAccountId());
        assertNull(records.getContent().get(0).actorAccountId());
        assertTrue(objectMapper.writeValueAsString(records.getContent().get(0))
                .contains("\"entityType\":\"TEST_ENTITY\""));
    }

    @Test
    void auditInsertRollsBackWithBusinessTransaction() {
        long entityId = ENTITY_IDS.incrementAndGet();
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            auditLogService.record(
                    "TEST_ENTITY",
                    entityId,
                    "UPDATED",
                    42L,
                    "DRAFT",
                    "APPROVED",
                    null,
                    null,
                    "Main"
            );
            status.setRollbackOnly();
        });

        Page<AuditLogResponse> records = auditLogRepository.findReportRows(
                "TEST_ENTITY",
                entityId,
                null,
                null,
                null,
                null,
                null,
                PageRequest.of(0, 10)
        );
        assertTrue(records.isEmpty());
    }

    @Test
    void historySearchIncludesEventsAcrossWholeEndDate() {
        long entityId = ENTITY_IDS.incrementAndGet();
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                auditLogService.record(
                        "TEST_ENTITY",
                        entityId,
                        "UPDATED",
                        42L,
                        "DRAFT",
                        "APPROVED",
                        null,
                        null,
                        "Main"
                )
        );
        authenticateAsOwner();

        var records = auditHistoryService.search(
                "test_entity",
                entityId,
                null,
                null,
                null,
                LocalDate.now(),
                LocalDate.now(),
                0,
                10
        );

        assertEquals(1, records.totalElements());
        assertEquals(entityId, records.content().getFirst().entityId());
    }

    @Test
    void repeatedMatchingTransitionsRemainSeparateHistoricalEvents() {
        long entityId = ENTITY_IDS.incrementAndGet();
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            for (int i = 0; i < 2; i++) {
                auditLogService.record(
                        "TEST_ENTITY",
                        entityId,
                        "DEACTIVATED",
                        42L,
                        "ACTIVE",
                        "INACTIVE",
                        null,
                        null,
                        "Main"
                );
            }
        });

        Page<AuditLogResponse> records = auditLogRepository.findReportRows(
                "TEST_ENTITY",
                entityId,
                null,
                null,
                "Main",
                null,
                null,
                PageRequest.of(0, 10)
        );
        assertEquals(2, records.getTotalElements());
    }

    private void authenticateAsOwner() {
        ProductUserPrincipal principal =
                new ProductUserPrincipal(42L, "OWNER", null);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.getAuthorities()
                )
        );
    }
}
