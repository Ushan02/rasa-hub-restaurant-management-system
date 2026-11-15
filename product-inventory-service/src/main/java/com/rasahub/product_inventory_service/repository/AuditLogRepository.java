package com.rasahub.product_inventory_service.repository;

import com.rasahub.product_inventory_service.dto.audit.AuditLogResponse;
import com.rasahub.product_inventory_service.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AuditLogRepository extends Repository<AuditLog, Long> {

    AuditLog save(AuditLog auditLog);

    @Query(
            value = """
                    SELECT new com.rasahub.product_inventory_service.dto.audit.AuditLogResponse(
                        audit.id,
                        audit.entityType,
                        audit.entityId,
                        audit.action,
                        audit.actorAccountId,
                        audit.previousStatus,
                        audit.newStatus,
                        audit.occurredAt,
                        audit.description,
                        audit.businessReference,
                        audit.locationCode
                    )
                    FROM AuditLog audit
                    WHERE (:entityType IS NULL OR audit.entityType = :entityType)
                      AND (:entityId IS NULL OR audit.entityId = :entityId)
                      AND (:actorAccountId IS NULL
                           OR audit.actorAccountId = :actorAccountId)
                      AND (:action IS NULL OR audit.action = :action)
                      AND (:locationCode IS NULL
                           OR audit.locationCode = :locationCode)
                      AND (:from IS NULL OR audit.occurredAt >= :from)
                      AND (:until IS NULL OR audit.occurredAt < :until)
                    ORDER BY audit.occurredAt DESC, audit.id DESC
                    """,
            countQuery = """
                    SELECT COUNT(audit)
                    FROM AuditLog audit
                    WHERE (:entityType IS NULL OR audit.entityType = :entityType)
                      AND (:entityId IS NULL OR audit.entityId = :entityId)
                      AND (:actorAccountId IS NULL
                           OR audit.actorAccountId = :actorAccountId)
                      AND (:action IS NULL OR audit.action = :action)
                      AND (:locationCode IS NULL
                           OR audit.locationCode = :locationCode)
                      AND (:from IS NULL OR audit.occurredAt >= :from)
                      AND (:until IS NULL OR audit.occurredAt < :until)
                    """
    )
    Page<AuditLogResponse> findReportRows(
            @Param("entityType") String entityType,
            @Param("entityId") Long entityId,
            @Param("actorAccountId") Long actorAccountId,
            @Param("action") String action,
            @Param("locationCode") String locationCode,
            @Param("from") LocalDateTime from,
            @Param("until") LocalDateTime until,
            Pageable pageable
    );
}
