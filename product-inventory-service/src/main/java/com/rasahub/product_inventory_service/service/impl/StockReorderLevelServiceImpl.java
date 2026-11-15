package com.rasahub.product_inventory_service.service.impl;

import com.rasahub.product_inventory_service.entity.InventoryItem;
import com.rasahub.product_inventory_service.entity.StockLocation;
import com.rasahub.product_inventory_service.entity.StockReorderLevel;
import com.rasahub.product_inventory_service.exception.BusinessRuleException;
import com.rasahub.product_inventory_service.exception.ResourceNotFoundException;
import com.rasahub.product_inventory_service.repository.InventoryItemRepository;
import com.rasahub.product_inventory_service.repository.StockLocationRepository;
import com.rasahub.product_inventory_service.repository.StockReorderLevelRepository;
import com.rasahub.product_inventory_service.service.LowStockAlertEvaluationService;
import com.rasahub.product_inventory_service.service.StockReorderLevelService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class StockReorderLevelServiceImpl implements StockReorderLevelService {

    private final StockReorderLevelRepository stockReorderLevelRepository;
    private final StockLocationRepository stockLocationRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final LowStockAlertEvaluationService lowStockAlertEvaluationService;

    public StockReorderLevelServiceImpl(
            StockReorderLevelRepository stockReorderLevelRepository,
            StockLocationRepository stockLocationRepository,
            InventoryItemRepository inventoryItemRepository,
            LowStockAlertEvaluationService lowStockAlertEvaluationService
    ) {
        this.stockReorderLevelRepository = stockReorderLevelRepository;
        this.stockLocationRepository = stockLocationRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.lowStockAlertEvaluationService = lowStockAlertEvaluationService;
    }

    @Override
    @Transactional
    public StockReorderLevel createConfiguration(
            String locationCode,
            Long inventoryItemId,
            BigDecimal reorderLevel,
            BigDecimal recommendedReorderQuantity,
            Long createdByAccountId
    ) {
        validateLocation(locationCode);
        validateInventoryItem(inventoryItemId);
        validateReorderLevel(reorderLevel);

        if (stockReorderLevelRepository.existsByStockLocation_LocationCodeAndInventoryItem_Id(locationCode, inventoryItemId)) {
            throw new BusinessRuleException("Reorder level already exists for this location and item");
        }

        StockLocation location = findLocation(locationCode);
        InventoryItem item = findInventoryItem(inventoryItemId);

        StockReorderLevel config = new StockReorderLevel(location, item, reorderLevel);
        config.setRecommendedReorderQuantity(recommendedReorderQuantity);
        config.setCreatedByAccountId(createdByAccountId);
        config.setUpdatedByAccountId(createdByAccountId);

        StockReorderLevel saved = stockReorderLevelRepository.saveAndFlush(config);
        lowStockAlertEvaluationService.evaluateItem(locationCode, inventoryItemId);
        return saved;
    }

    @Override
    @Transactional
    public StockReorderLevel updateConfiguration(
            String locationCode,
            Long inventoryItemId,
            BigDecimal reorderLevel,
            BigDecimal recommendedReorderQuantity,
            Long updatedByAccountId
    ) {
        validateLocation(locationCode);
        validateInventoryItem(inventoryItemId);
        validateReorderLevel(reorderLevel);

        StockReorderLevel config = stockReorderLevelRepository
                .findForUpdate(locationCode, inventoryItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Reorder level configuration not found"));

        config.setReorderLevel(reorderLevel);
        config.setRecommendedReorderQuantity(recommendedReorderQuantity);
        config.setUpdatedByAccountId(updatedByAccountId);

        StockReorderLevel saved = stockReorderLevelRepository.saveAndFlush(config);
        lowStockAlertEvaluationService.evaluateItem(locationCode, inventoryItemId);
        return saved;
    }

    @Override
    @Transactional
    public StockReorderLevel deactivateConfiguration(
            String locationCode,
            Long inventoryItemId,
            Long updatedByAccountId
    ) {
        validateLocation(locationCode);
        validateInventoryItem(inventoryItemId);

        StockReorderLevel config = stockReorderLevelRepository
                .findForUpdate(locationCode, inventoryItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Reorder level configuration not found"));

        config.setActive(false);
        config.setUpdatedByAccountId(updatedByAccountId);
        StockReorderLevel saved = stockReorderLevelRepository.saveAndFlush(config);
        lowStockAlertEvaluationService.evaluateItem(locationCode, inventoryItemId);
        return saved;
    }

    @Override
    public List<StockReorderLevel> getConfigurationsByLocation(String locationCode) {
        validateLocation(locationCode);
        return stockReorderLevelRepository.findByStockLocation_LocationCodeAndActiveTrue(locationCode);
    }

    @Override
    public Optional<StockReorderLevel> getConfiguration(String locationCode, Long inventoryItemId) {
        validateLocation(locationCode);
        validateInventoryItem(inventoryItemId);
        return stockReorderLevelRepository.findByStockLocation_LocationCodeAndInventoryItem_Id(locationCode, inventoryItemId);
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

    private void validateReorderLevel(BigDecimal reorderLevel) {
        if (reorderLevel == null) {
            throw new BusinessRuleException("Reorder level is required");
        }
        if (reorderLevel.signum() < 0) {
            throw new BusinessRuleException("Reorder level cannot be negative");
        }
        if (reorderLevel.scale() > 3) {
            throw new BusinessRuleException("Invalid reorder level precision");
        }
    }

    private StockLocation findLocation(String locationCode) {
        return stockLocationRepository.findById(locationCode)
                .orElseThrow(() -> new ResourceNotFoundException("Stock location not found"));
    }

    private InventoryItem findInventoryItem(Long inventoryItemId) {
        return inventoryItemRepository.findById(inventoryItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item not found"));
    }
}
