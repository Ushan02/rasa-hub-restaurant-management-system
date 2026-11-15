package com.rasahub.product_inventory_service.dto.audit;

import java.time.LocalDateTime;

public record AuditLogResponse(
        Long auditId,
        String entityType,
        Long entityId,
        String action,
        Long actorAccountId,
        String previousStatus,
        String newStatus,
        LocalDateTime occurredAt,
        String description,
        String businessReference,
        String locationCode
) {
}
