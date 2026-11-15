package com.rasahub.product_inventory_service.service.impl;

import com.rasahub.product_inventory_service.entity.InventoryItem;
import com.rasahub.product_inventory_service.entity.LowStockAlert;
import com.rasahub.product_inventory_service.entity.StockBalance;
import com.rasahub.product_inventory_service.entity.StockLocation;
import com.rasahub.product_inventory_service.entity.StockReorderLevel;
import com.rasahub.product_inventory_service.enums.AlertSeverity;
import com.rasahub.product_inventory_service.enums.AlertStatus;
import com.rasahub.product_inventory_service.enums.StockStatus;
import com.rasahub.product_inventory_service.exception.BusinessRuleException;
import com.rasahub.product_inventory_service.exception.ResourceNotFoundException;
import com.rasahub.product_inventory_service.repository.InventoryItemRepository;
import com.rasahub.product_inventory_service.repository.LowStockAlertRepository;
import com.rasahub.product_inventory_service.repository.StockBalanceRepository;
import com.rasahub.product_inventory_service.repository.StockLocationRepository;
import com.rasahub.product_inventory_service.repository.StockReorderLevelRepository;
import com.rasahub.product_inventory_service.service.LowStockAlertEvaluationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class LowStockAlertServiceImpl implements LowStockAlertEvaluationService {

    private final StockReorderLevelRepository stockReorderLevelRepository;
    private final LowStockAlertRepository lowStockAlertRepository;
    private final StockBalanceRepository stockBalanceRepository;
    private final StockLocationRepository stockLocationRepository;
    private final InventoryItemRepository inventoryItemRepository;

    public LowStockAlertServiceImpl(
            StockReorderLevelRepository stockReorderLevelRepository,
            LowStockAlertRepository lowStockAlertRepository,
            StockBalanceRepository stockBalanceRepository,
            StockLocationRepository stockLocationRepository,
            InventoryItemRepository inventoryItemRepository
    ) {
        this.stockReorderLevelRepository = stockReorderLevelRepository;
        this.lowStockAlertRepository = lowStockAlertRepository;
        this.stockBalanceRepository = stockBalanceRepository;
        this.stockLocationRepository = stockLocationRepository;
        this.inventoryItemRepository = inventoryItemRepository;
    }

    @Override
    public List<LowStockAlert> getAlertsByLocation(String locationCode) {
        validateLocation(locationCode);
        return lowStockAlertRepository.findByStockLocation_LocationCodeOrderByDetectedAtDesc(locationCode);
    }

    @Override
    public List<LowStockAlert> getAlertsByItem(Long inventoryItemId) {
        validateInventoryItem(inventoryItemId);
        return lowStockAlertRepository.findByInventoryItem_IdOrderByDetectedAtDesc(inventoryItemId);
    }

    @Override
    public LowStockAlert acknowledgeAlert(Long alertId, Long accountId, String remarks) {
        if (alertId == null || alertId < 1) {
            throw new BusinessRuleException("Valid alert ID is required");
        }
        if (accountId == null || accountId < 1) {
            throw new BusinessRuleException("Valid account ID is required");
        }

        LowStockAlert alert = lowStockAlertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("Low stock alert not found"));

        alert.acknowledge(accountId, remarks);
        return lowStockAlertRepository.saveAndFlush(alert);
    }

    @Override
    public LowStockAlert resolveAlert(Long alertId, String remarks) {
        if (alertId == null || alertId < 1) {
            throw new BusinessRuleException("Valid alert ID is required");
        }

        LowStockAlert alert = lowStockAlertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("Low stock alert not found"));

        alert.resolve(remarks);
        return lowStockAlertRepository.saveAndFlush(alert);
    }

    @Override
    @Transactional
    public LowStockAlert evaluateItem(String locationCode, Long inventoryItemId) {
        validateLocation(locationCode);
        validateInventoryItem(inventoryItemId);

        StockReorderLevel config = stockReorderLevelRepository
                .findForUpdate(locationCode, inventoryItemId)
                .orElse(null);

        if (config == null || !config.isActive()) {
            return null;
        }

        StockBalance balance = stockBalanceRepository
                .findByStockLocation_LocationCodeAndInventoryItem_Id(locationCode, inventoryItemId)
                .orElseGet(() -> new StockBalance(findLocation(locationCode), findInventoryItem(inventoryItemId)));

        BigDecimal currentQuantity = balance.getQuantityOnHand();
        StockStatus status = StockStatus.fromQuantity(currentQuantity, config.getReorderLevel());

        List<LowStockAlert> activeAlerts = lowStockAlertRepository.findActiveForUpdate(
                locationCode,
                inventoryItemId,
                List.of(AlertStatus.OPEN, AlertStatus.ACKNOWLEDGED)
        );

        if (status == StockStatus.NORMAL) {
            if (!activeAlerts.isEmpty()) {
                LowStockAlert resolvedAlert = activeAlerts.getFirst();
                for (LowStockAlert activeAlert : activeAlerts) {
                    activeAlert.resolve("Stock returned to normal");
                }
                lowStockAlertRepository.saveAllAndFlush(activeAlerts);
                return resolvedAlert;
            }
            return null;
        }

        AlertSeverity severity = status == StockStatus.OUT_OF_STOCK
                ? AlertSeverity.OUT_OF_STOCK
                : AlertSeverity.LOW_STOCK;

        if (activeAlerts.isEmpty()) {
            LowStockAlert alert = new LowStockAlert(
                    config.getStockLocation(),
                    config.getInventoryItem(),
                    config.getReorderLevel(),
                    currentQuantity,
                    severity
            );
            alert.updateEvaluation(
                    config.getReorderLevel(),
                    currentQuantity,
                    severity,
                    LocalDateTime.now(),
                    "System-evaluated alert"
            );
            return lowStockAlertRepository.saveAndFlush(alert);
        }

        LowStockAlert activeAlert = activeAlerts.getFirst();
        if (activeAlert.getSeverity() != severity) {
            activeAlert.updateEvaluation(
                    config.getReorderLevel(),
                    currentQuantity,
                    severity,
                    LocalDateTime.now(),
                    "Severity updated due to stock movement"
            );
            return lowStockAlertRepository.saveAndFlush(activeAlert);
        }

        activeAlert.updateEvaluation(
                config.getReorderLevel(),
                currentQuantity,
                severity,
                LocalDateTime.now(),
                activeAlert.getRemarks()
        );
        return lowStockAlertRepository.saveAndFlush(activeAlert);
    }

    @Override
    @Transactional
    public List<LowStockAlert> evaluateLocation(String locationCode) {
        validateLocation(locationCode);

        List<StockReorderLevel> configs = stockReorderLevelRepository
                .findByStockLocation_LocationCodeAndActiveTrue(locationCode);

        return configs.stream()
                .map(config -> evaluateItem(config.getStockLocation().getLocationCode(), config.getInventoryItem().getId()))
                .filter(alert -> alert != null)
                .toList();
    }

    private StockLocation findLocation(String locationCode) {
        return stockLocationRepository.findById(locationCode)
                .orElseThrow(() -> new ResourceNotFoundException("Stock location not found"));
    }

    private InventoryItem findInventoryItem(Long inventoryItemId) {
        return inventoryItemRepository.findById(inventoryItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item not found"));
    }

    private void validateLocation(String locationCode) {
        if (locationCode == null || locationCode.isBlank()) {
            throw new BusinessRuleException("Location code is required");
        }
        if (!stockLocationRepository.existsById(locationCode.trim())) {
            throw new ResourceNotFoundException("Stock location not found");
        }
    }

    private void validateInventoryItem(Long inventoryItemId) {
        if (inventoryItemId == null || inventoryItemId < 1) {
            throw new BusinessRuleException("Valid inventory item ID is required");
        }
        if (!inventoryItemRepository.existsById(inventoryItemId)) {
            throw new ResourceNotFoundException("Inventory item not found");
        }
    }
}
