package com.rasahub.product_inventory_service.entity;

import com.rasahub.product_inventory_service.enums.AlertSeverity;
import com.rasahub.product_inventory_service.enums.AlertStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "low_stock_alerts",
        indexes = {
                @Index(
                        name = "idx_low_stock_alerts_active",
                        columnList = "location_code, inventory_item_id, status"
                )
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_low_stock_alerts_location_item_status",
                        columnNames = {"location_code", "inventory_item_id", "status"}
                )
        }
)
public class LowStockAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "location_code",
            referencedColumnName = "location_code",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_low_stock_alerts_location")
    )
    private StockLocation stockLocation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "inventory_item_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_low_stock_alerts_inventory_item")
    )
    private InventoryItem inventoryItem;

    @NotNull
    @DecimalMin(value = "0.000")
    @Digits(integer = 15, fraction = 3)
    @Column(name = "reorder_level_snapshot", nullable = false, precision = 18, scale = 3)
    private BigDecimal reorderLevelSnapshot = BigDecimal.ZERO;

    @NotNull
    @DecimalMin(value = "0.000")
    @Digits(integer = 15, fraction = 3)
    @Column(name = "current_quantity_snapshot", nullable = false, precision = 18, scale = 3)
    private BigDecimal currentQuantitySnapshot = BigDecimal.ZERO;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private AlertSeverity severity;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AlertStatus status = AlertStatus.OPEN;

    @Column(name = "detected_at", nullable = false)
    private LocalDateTime detectedAt;

    @Column(name = "acknowledged_by_account_id")
    private Long acknowledgedByAccountId;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "last_evaluated_at")
    private LocalDateTime lastEvaluatedAt;

    @Column(name = "remarks", length = 500)
    private String remarks;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected LowStockAlert() {
    }

    public LowStockAlert(
            StockLocation stockLocation,
            InventoryItem inventoryItem,
            BigDecimal reorderLevelSnapshot,
            BigDecimal currentQuantitySnapshot,
            AlertSeverity severity
    ) {
        this.stockLocation = stockLocation;
        this.inventoryItem = inventoryItem;
        this.reorderLevelSnapshot = reorderLevelSnapshot;
        this.currentQuantitySnapshot = currentQuantitySnapshot;
        this.severity = severity;
        this.status = AlertStatus.OPEN;
        this.detectedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.detectedAt == null) {
            this.detectedAt = now;
        }
        if (this.lastEvaluatedAt == null) {
            this.lastEvaluatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.lastEvaluatedAt == null) {
            this.lastEvaluatedAt = this.updatedAt;
        }
    }

    public Long getId() {
        return id;
    }

    public StockLocation getStockLocation() {
        return stockLocation;
    }

    public InventoryItem getInventoryItem() {
        return inventoryItem;
    }

    public BigDecimal getReorderLevelSnapshot() {
        return reorderLevelSnapshot;
    }

    public BigDecimal getCurrentQuantitySnapshot() {
        return currentQuantitySnapshot;
    }

    public AlertSeverity getSeverity() {
        return severity;
    }

    public AlertStatus getStatus() {
        return status;
    }

    public LocalDateTime getDetectedAt() {
        return detectedAt;
    }

    public Long getAcknowledgedByAccountId() {
        return acknowledgedByAccountId;
    }

    public LocalDateTime getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public LocalDateTime getLastEvaluatedAt() {
        return lastEvaluatedAt;
    }

    public String getRemarks() {
        return remarks;
    }

    public Long getVersion() {
        return version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public boolean isActive() {
        return status != AlertStatus.RESOLVED;
    }

    public void acknowledge(Long accountId, String remarks) {
        if (status == AlertStatus.RESOLVED) {
            throw new IllegalStateException("Resolved alert cannot be acknowledged");
        }
        this.acknowledgedByAccountId = accountId;
        this.acknowledgedAt = LocalDateTime.now();
        this.status = AlertStatus.ACKNOWLEDGED;
        this.remarks = remarks;
    }

    public void resolve(String remarks) {
        this.status = AlertStatus.RESOLVED;
        this.resolvedAt = LocalDateTime.now();
        this.remarks = remarks;
    }

    public void updateEvaluation(
            BigDecimal reorderLevelSnapshot,
            BigDecimal currentQuantitySnapshot,
            AlertSeverity severity,
            LocalDateTime evaluatedAt,
            String remarks
    ) {
        this.reorderLevelSnapshot = reorderLevelSnapshot;
        this.currentQuantitySnapshot = currentQuantitySnapshot;
        this.severity = severity;
        this.lastEvaluatedAt = evaluatedAt;
        if (remarks != null && !remarks.isBlank()) {
            this.remarks = remarks;
        }
        if (this.status == AlertStatus.RESOLVED) {
            this.status = AlertStatus.OPEN;
            this.detectedAt = evaluatedAt;
        }
    }
}
