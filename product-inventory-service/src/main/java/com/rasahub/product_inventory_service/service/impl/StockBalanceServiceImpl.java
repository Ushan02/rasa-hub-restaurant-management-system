
package com.rasahub.product_inventory_service.service.impl;

import com.rasahub.product_inventory_service.dto.stockbalance.StockBalanceMinimumLevelUpdateRequest;
import com.rasahub.product_inventory_service.dto.stockbalance.StockBalanceResponse;
import com.rasahub.product_inventory_service.entity.InventoryItem;
import com.rasahub.product_inventory_service.entity.StockBalance;
import com.rasahub.product_inventory_service.entity.StockLocation;
import com.rasahub.product_inventory_service.entity.StockReorderLevel;
import com.rasahub.product_inventory_service.enums.MeasurementUnit;
import com.rasahub.product_inventory_service.enums.StockStatus;
import com.rasahub.product_inventory_service.exception.BusinessRuleException;
import com.rasahub.product_inventory_service.exception.ResourceNotFoundException;
import com.rasahub.product_inventory_service.repository.InventoryItemRepository;
import com.rasahub.product_inventory_service.repository.StockBalanceRepository;
import com.rasahub.product_inventory_service.repository.StockLocationRepository;
import com.rasahub.product_inventory_service.repository.StockReorderLevelRepository;
import com.rasahub.product_inventory_service.service.StockBalanceService;
import com.rasahub.product_inventory_service.service.StockReorderLevelService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class StockBalanceServiceImpl implements StockBalanceService {

    private final StockBalanceRepository stockBalanceRepository;
    private final StockLocationRepository stockLocationRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final StockReorderLevelRepository stockReorderLevelRepository;
    private final StockReorderLevelService stockReorderLevelService;

    public StockBalanceServiceImpl(
            StockBalanceRepository stockBalanceRepository,
            StockLocationRepository stockLocationRepository,
            InventoryItemRepository inventoryItemRepository,
            StockReorderLevelRepository stockReorderLevelRepository,
            StockReorderLevelService stockReorderLevelService
    ) {
        this.stockBalanceRepository = stockBalanceRepository;
        this.stockLocationRepository = stockLocationRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.stockReorderLevelRepository = stockReorderLevelRepository;
        this.stockReorderLevelService = stockReorderLevelService;
    }

    @Override
    public List<StockBalanceResponse> getBalancesByLocation(
            String locationCode
    ) {
        StockLocation location = findLocation(locationCode);

        return toResponses(
                stockBalanceRepository.findByStockLocation_LocationCode(location.getLocationCode()),
                stockReorderLevelRepository.findByStockLocation_LocationCodeAndActiveTrue(
                        location.getLocationCode()
                ).stream().collect(Collectors.toMap(
                        config -> config.getInventoryItem().getId(),
                        StockReorderLevel::getReorderLevel
                )),
                balance -> balance.getInventoryItem().getId()
        );
    }

    @Override
    public List<StockBalanceResponse> getBalancesByInventoryItem(
            Long inventoryItemId
    ) {
        InventoryItem item = findInventoryItem(inventoryItemId);

        return toResponses(
                stockBalanceRepository.findByInventoryItem_Id(item.getId()),
                stockReorderLevelRepository.findByInventoryItem_IdAndActiveTrue(item.getId())
                        .stream()
                        .collect(Collectors.toMap(
                                config -> config.getStockLocation().getLocationCode(),
                                StockReorderLevel::getReorderLevel
                        )),
                balance -> balance.getStockLocation().getLocationCode()
        );
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

        return toResponse(
                balance,
                stockReorderLevelRepository
                        .findByStockLocation_LocationCodeAndInventoryItem_IdAndActiveTrue(
                                location.getLocationCode(),
                                item.getId()
                        )
                        .map(StockReorderLevel::getReorderLevel)
                        .orElse(null)
        );
    }

    @Override
    public List<StockBalanceResponse> getLowStockBalances(
            String locationCode
    ) {
        StockLocation location = findLocation(locationCode);

        List<StockBalance> balances =
                stockBalanceRepository.findByStockLocation_LocationCode(location.getLocationCode());
        Map<Long, BigDecimal> reorderLevels = stockReorderLevelRepository
                .findByStockLocation_LocationCodeAndActiveTrue(location.getLocationCode())
                .stream()
                .collect(Collectors.toMap(
                        config -> config.getInventoryItem().getId(),
                        StockReorderLevel::getReorderLevel
                ));
        return balances.stream()
                .filter(balance -> StockStatus.fromQuantity(
                        balance.getQuantityOnHand(),
                        reorderLevels.get(balance.getInventoryItem().getId())
                ) != StockStatus.NORMAL)
                .map(balance -> toResponse(
                        balance,
                        reorderLevels.get(balance.getInventoryItem().getId())
                ))
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

        StockReorderLevel configuration = stockReorderLevelRepository
                .findForUpdate(
                        location.getLocationCode(),
                        item.getId()
                )
                .orElse(null);
        if (configuration == null) {
            stockReorderLevelService.createConfiguration(
                    location.getLocationCode(),
                    item.getId(),
                    minimumLevel,
                    null,
                    null
            );
        } else {
            stockReorderLevelService.updateConfiguration(
                    location.getLocationCode(),
                    item.getId(),
                    minimumLevel,
                    configuration.getRecommendedReorderQuantity(),
                    null
            );
        }

        StockBalance balance = stockBalanceRepository
                .findForUpdate(
                        location.getLocationCode(),
                        item.getId()
                )
                .orElseGet(() -> stockBalanceRepository.saveAndFlush(new StockBalance(location, item)));
        return toResponse(
                balance,
                stockReorderLevelRepository
                        .findByStockLocation_LocationCodeAndInventoryItem_IdAndActiveTrue(
                                location.getLocationCode(),
                                item.getId()
                        )
                        .map(StockReorderLevel::getReorderLevel)
                        .orElse(null)
        );
    }

    private <K> List<StockBalanceResponse> toResponses(
            List<StockBalance> balances,
            Map<K, BigDecimal> reorderLevels,
            Function<StockBalance, K> keyExtractor
    ) {
        return balances.stream()
                .map(balance -> toResponse(balance, reorderLevels.get(keyExtractor.apply(balance))))
                .toList();
    }

    private StockBalanceResponse toResponse(StockBalance balance, BigDecimal reorderLevel) {
        return new StockBalanceResponse(balance, reorderLevel);
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
