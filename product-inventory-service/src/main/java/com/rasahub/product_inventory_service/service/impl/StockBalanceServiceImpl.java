
package com.rasahub.product_inventory_service.service.impl;

import com.rasahub.product_inventory_service.dto.stockbalance.StockBalanceMinimumLevelUpdateRequest;
import com.rasahub.product_inventory_service.dto.stockbalance.StockBalanceResponse;
import com.rasahub.product_inventory_service.entity.InventoryItem;
import com.rasahub.product_inventory_service.entity.StockBalance;
import com.rasahub.product_inventory_service.entity.StockLocation;
import com.rasahub.product_inventory_service.enums.MeasurementUnit;
import com.rasahub.product_inventory_service.exception.BusinessRuleException;
import com.rasahub.product_inventory_service.exception.ResourceNotFoundException;
import com.rasahub.product_inventory_service.repository.InventoryItemRepository;
import com.rasahub.product_inventory_service.repository.StockBalanceRepository;
import com.rasahub.product_inventory_service.repository.StockLocationRepository;
import com.rasahub.product_inventory_service.service.StockBalanceService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class StockBalanceServiceImpl implements StockBalanceService {

    private final StockBalanceRepository stockBalanceRepository;
    private final StockLocationRepository stockLocationRepository;
    private final InventoryItemRepository inventoryItemRepository;

    public StockBalanceServiceImpl(
            StockBalanceRepository stockBalanceRepository,
            StockLocationRepository stockLocationRepository,
            InventoryItemRepository inventoryItemRepository
    ) {
        this.stockBalanceRepository = stockBalanceRepository;
        this.stockLocationRepository = stockLocationRepository;
        this.inventoryItemRepository = inventoryItemRepository;
    }

    @Override
    public List<StockBalanceResponse> getBalancesByLocation(
            String locationCode
    ) {
        StockLocation location = findLocation(locationCode);

        return stockBalanceRepository
                .findByStockLocation_LocationCode(
                        location.getLocationCode()
                )
                .stream()
                .map(StockBalanceResponse::new)
                .toList();
    }

    @Override
    public List<StockBalanceResponse> getBalancesByInventoryItem(
            Long inventoryItemId
    ) {
        InventoryItem item = findInventoryItem(inventoryItemId);

        return stockBalanceRepository
                .findByInventoryItem_Id(item.getId())
                .stream()
                .map(StockBalanceResponse::new)
                .toList();
    }

    @Override
    public StockBalanceResponse getBalance(
            String locationCode,
            Long inventoryItemId
    ) {
        StockLocation location = findLocation(locationCode);
        InventoryItem item = findInventoryItem(inventoryItemId);

        StockBalance balance = stockBalanceRepository
                .findByStockLocation_LocationCodeAndInventoryItem_Id(
                        location.getLocationCode(),
                        item.getId()
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Stock balance not found"
                        )
                );

        return new StockBalanceResponse(balance);
    }

    @Override
    public List<StockBalanceResponse> getLowStockBalances(
            String locationCode
    ) {
        StockLocation location = findLocation(locationCode);

        return stockBalanceRepository
                .findByStockLocation_LocationCode(
                        location.getLocationCode()
                )
                .stream()
                .filter(StockBalance::isLowStock)
                .map(StockBalanceResponse::new)
                .toList();
    }

    @Override
    @Transactional
    public StockBalanceResponse updateMinimumStockLevel(
            String locationCode,
            Long inventoryItemId,
            StockBalanceMinimumLevelUpdateRequest request
    ) {
        if (request == null) {
            throw new BusinessRuleException(
                    "Minimum stock level request is required"
            );
        }

        BigDecimal minimumLevel = request.getMinimumStockLevel();

        validateMinimumLevel(minimumLevel);

        StockLocation location = findLocation(locationCode);
        InventoryItem item = findInventoryItem(inventoryItemId);

        if (!location.isActive()) {
            throw new BusinessRuleException(
                    "Cannot configure stock for an inactive location"
            );
        }

        if (!item.isActive()) {
            throw new BusinessRuleException(
                    "Cannot configure an inactive inventory item"
            );
        }

        if (item.getMeasurementUnit() == MeasurementUnit.PCS
                && minimumLevel.stripTrailingZeros().scale() > 0) {
            throw new BusinessRuleException(
                    "PCS minimum stock level must be a whole number"
            );
        }

        StockBalance balance = stockBalanceRepository
                .findForUpdate(
                        location.getLocationCode(),
                        item.getId()
                )
                .orElseGet(() ->
                        new StockBalance(location, item)
                );

        balance.setMinimumStockLevel(minimumLevel);

        StockBalance savedBalance =
                stockBalanceRepository.saveAndFlush(balance);

        return new StockBalanceResponse(savedBalance);
    }

    private StockLocation findLocation(String locationCode) {

        if (locationCode == null || locationCode.isBlank()) {
            throw new BusinessRuleException(
                    "Location code is required"
            );
        }

        return stockLocationRepository
                .findById(locationCode.trim())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Stock location not found"
                        )
                );
    }

    private InventoryItem findInventoryItem(Long inventoryItemId) {

        if (inventoryItemId == null || inventoryItemId < 1) {
            throw new BusinessRuleException(
                    "Valid inventory item ID is required"
            );
        }

        return inventoryItemRepository
                .findById(inventoryItemId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Inventory item not found"
                        )
                );
    }

    private void validateMinimumLevel(BigDecimal minimumLevel) {

        if (minimumLevel == null
                || minimumLevel.signum() < 0
                || minimumLevel.scale() > 3
                || minimumLevel.precision() - minimumLevel.scale() > 15) {

            throw new BusinessRuleException(
                    "Invalid minimum stock level"
            );
        }
    }
}
