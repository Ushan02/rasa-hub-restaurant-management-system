package com.rasahub.product_inventory_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Index;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "audit_logs",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_audit_logs_event_key",
                columnNames = "event_key"
        ),
        indexes = {
                @Index(name = "idx_audit_entity", columnList = "entity_type, entity_id, occurred_at"),
                @Index(name = "idx_audit_actor", columnList = "actor_account_id, occurred_at"),
                @Index(name = "idx_audit_location", columnList = "location_code, occurred_at"),
                @Index(name = "idx_audit_action", columnList = "action, occurred_at")
        }
)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_key", nullable = false, updatable = false, length = 250)
    private String eventKey;

    @Column(name = "entity_type", nullable = false, updatable = false, length = 40)
    private String entityType;

    @Column(name = "entity_id", nullable = false, updatable = false)
    private Long entityId;

    @Column(name = "action", nullable = false, updatable = false, length = 40)
    private String action;

    @Column(name = "actor_account_id", updatable = false)
    private Long actorAccountId;

    @Column(name = "previous_status", updatable = false, length = 30)
    private String previousStatus;

    @Column(name = "new_status", updatable = false, length = 30)
    private String newStatus;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private LocalDateTime occurredAt;

    @Column(name = "description", updatable = false, length = 500)
    private String description;

    @Column(name = "business_reference", updatable = false, length = 120)
    private String businessReference;

    @Column(name = "location_code", updatable = false, length = 5)
    private String locationCode;

    protected AuditLog() {
    }

    public AuditLog(
            String eventKey,
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
        this.eventKey = eventKey;
        this.entityType = entityType;
        this.entityId = entityId;
        this.action = action;
        this.actorAccountId = actorAccountId;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.description = description;
        this.businessReference = businessReference;
        this.locationCode = locationCode;
    }

    @PrePersist
    protected void onCreate() {
        if (eventKey == null || eventKey.isBlank()
                || entityType == null || entityType.isBlank()
                || entityId == null || entityId <= 0
                || action == null || action.isBlank()
                || actorAccountId != null && actorAccountId <= 0) {
            throw new IllegalStateException("Valid audit event fields are required");
        }
        occurredAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void rejectUpdate() {
        throw new UnsupportedOperationException("Audit records are immutable");
    }

    @PreRemove
    protected void rejectDelete() {
        throw new UnsupportedOperationException("Audit records are immutable");
    }

    public Long getId() { return id; }
    public String getEventKey() { return eventKey; }
    public String getEntityType() { return entityType; }
    public Long getEntityId() { return entityId; }
    public String getAction() { return action; }
    public Long getActorAccountId() { return actorAccountId; }
    public String getPreviousStatus() { return previousStatus; }
    public String getNewStatus() { return newStatus; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public String getDescription() { return description; }
    public String getBusinessReference() { return businessReference; }
    public String getLocationCode() { return locationCode; }
}
