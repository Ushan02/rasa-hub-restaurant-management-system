
package com.rasahub.product_inventory_service.service.impl;

import com.rasahub.product_inventory_service.dto.stockmovement.StockMovementResponse;
import com.rasahub.product_inventory_service.enums.StockMovementType;
import com.rasahub.product_inventory_service.exception.BusinessRuleException;
import com.rasahub.product_inventory_service.exception.ResourceNotFoundException;
import com.rasahub.product_inventory_service.repository.InventoryItemRepository;
import com.rasahub.product_inventory_service.repository.StockLocationRepository;
import com.rasahub.product_inventory_service.repository.StockMovementRepository;
import com.rasahub.product_inventory_service.service.StockMovementService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class StockMovementServiceImpl
        implements StockMovementService {

    private final StockMovementRepository stockMovementRepository;
    private final StockLocationRepository stockLocationRepository;
    private final InventoryItemRepository inventoryItemRepository;

    public StockMovementServiceImpl(
            StockMovementRepository stockMovementRepository,
            StockLocationRepository stockLocationRepository,
            InventoryItemRepository inventoryItemRepository
    ) {
        this.stockMovementRepository = stockMovementRepository;
        this.stockLocationRepository = stockLocationRepository;
        this.inventoryItemRepository = inventoryItemRepository;
    }

    @Override
    public Page<StockMovementResponse> getMovementsByLocation(
            String locationCode,
            int page,
            int size
    ) {
        String code = validateLocation(locationCode);
        Pageable pageable = createPageable(page, size);

        return stockMovementRepository
                .findByStockLocation_LocationCodeOrderByCreatedAtDescIdDesc(
                        code,
                        pageable
                )
                .map(StockMovementResponse::new);
    }

    @Override
    public Page<StockMovementResponse> getMovementsByInventoryItem(
            Long inventoryItemId,
            int page,
            int size
    ) {
        validateInventoryItem(inventoryItemId);
        Pageable pageable = createPageable(page, size);

        return stockMovementRepository
                .findByInventoryItem_IdOrderByCreatedAtDescIdDesc(
                        inventoryItemId,
                        pageable
                )
                .map(StockMovementResponse::new);
    }

    @Override
    public Page<StockMovementResponse> getMovementsByLocationAndItem(
            String locationCode,
            Long inventoryItemId,
            int page,
            int size
    ) {
        String code = validateLocation(locationCode);
        validateInventoryItem(inventoryItemId);

        Pageable pageable = createPageable(page, size);

        return stockMovementRepository
                .findByStockLocation_LocationCodeAndInventoryItem_IdOrderByCreatedAtDescIdDesc(
                        code,
                        inventoryItemId,
                        pageable
                )
                .map(StockMovementResponse::new);
    }

    @Override
    public List<StockMovementResponse> getMovementsByReference(
            String referenceType,
            String referenceId
    ) {
        if (referenceType == null || referenceType.isBlank()) {
            throw new BusinessRuleException(
                    "Reference type is required"
            );
        }

        if (referenceId == null || referenceId.isBlank()) {
            throw new BusinessRuleException(
                    "Reference ID is required"
            );
        }

        String normalizedType = referenceType
                .trim()
                .toUpperCase(Locale.ROOT);

        String normalizedId = referenceId.trim();

        if (normalizedType.length() > 30
                || !normalizedType.matches("[A-Z_]+")) {
            throw new BusinessRuleException(
                    "Invalid reference type"
            );
        }

        if (normalizedId.length() > 64) {
            throw new BusinessRuleException(
                    "Reference ID must not exceed 64 characters"
            );
        }

        return stockMovementRepository
                .findByReferenceTypeAndReferenceIdOrderByIdAsc(
                        normalizedType,
                        normalizedId
                )
                .stream()
                .map(StockMovementResponse::new)
                .toList();
    }

    @Override
    public Page<StockMovementResponse> getMovementsByType(
            StockMovementType movementType,
            int page,
            int size
    ) {
        if (movementType == null) {
            throw new BusinessRuleException(
                    "Movement type is required"
            );
        }

        Pageable pageable = createPageable(page, size);

        return stockMovementRepository
                .findByMovementTypeOrderByCreatedAtDescIdDesc(
                        movementType,
                        pageable
                )
                .map(StockMovementResponse::new);
    }

    private String validateLocation(String locationCode) {

        if (locationCode == null || locationCode.isBlank()) {
            throw new BusinessRuleException(
                    "Location code is required"
            );
        }

        String code = locationCode.trim();

        if (!stockLocationRepository.existsById(code)) {
            throw new ResourceNotFoundException(
                    "Stock location not found"
            );
        }

        return code;
    }

    private void validateInventoryItem(Long inventoryItemId) {

        if (inventoryItemId == null || inventoryItemId < 1) {
            throw new BusinessRuleException(
                    "Valid inventory item ID is required"
            );
        }

        if (!inventoryItemRepository.existsById(inventoryItemId)) {
            throw new ResourceNotFoundException(
                    "Inventory item not found"
            );
        }
    }

    private Pageable createPageable(int page, int size) {

        if (page < 0) {
            throw new BusinessRuleException(
                    "Page number cannot be negative"
            );
        }

        if (size < 1 || size > 100) {
            throw new BusinessRuleException(
                    "Page size must be between 1 and 100"
            );
        }

        return PageRequest.of(page, size);
    }
}
