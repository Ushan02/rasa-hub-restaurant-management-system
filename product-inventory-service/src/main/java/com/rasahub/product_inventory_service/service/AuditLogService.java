package com.rasahub.product_inventory_service.service;

import com.rasahub.product_inventory_service.entity.AuditLog;
import com.rasahub.product_inventory_service.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuditLogService {

    private final AuditLogRepository repository;

    public AuditLogService(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(
            String entityType,
            Long entityId,
            String action,
            Long actorAccountId,
            String previousStatus,
            String newStatus,
            String description,
            String businessReference,
            String locationCode
    ) {
        validateLength(entityType, 40, "entityType");
        validateLength(action, 40, "action");
        validateLength(previousStatus, 30, "previousStatus");
        validateLength(newStatus, 30, "newStatus");
        validateLength(description, 500, "description");
        validateLength(businessReference, 120, "businessReference");
        validateLength(locationCode, 5, "locationCode");
        if (entityId == null || entityId < 1
                || actorAccountId != null && actorAccountId < 1) {
            throw new IllegalArgumentException(
                    "Valid audit entity and actor IDs are required"
            );
        }
        String eventKey = UUID.randomUUID().toString();
        repository.save(new AuditLog(
                eventKey,
                entityType,
                entityId,
                action,
                actorAccountId,
                previousStatus,
                newStatus,
                description,
                businessReference,
                locationCode
        ));
    }

    private void validateLength(String value, int maxLength, String field) {
        if (value != null && value.length() > maxLength) {
            throw new IllegalArgumentException(
                    "Audit " + field + " exceeds maximum length"
            );
        }
    }
}
