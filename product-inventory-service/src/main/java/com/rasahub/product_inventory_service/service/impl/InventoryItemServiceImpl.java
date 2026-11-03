
package com.rasahub.product_inventory_service.service.impl;

import com.rasahub.product_inventory_service.dto.inventory.InventoryItemCreateRequest;
import com.rasahub.product_inventory_service.dto.inventory.InventoryItemUpdateRequest;
import com.rasahub.product_inventory_service.dto.inventory.InventoryItemResponse;
import com.rasahub.product_inventory_service.entity.InventoryItem;
import com.rasahub.product_inventory_service.exception.BusinessRuleException;
import com.rasahub.product_inventory_service.exception.ResourceNotFoundException;
import com.rasahub.product_inventory_service.repository.InventoryItemRepository;
import com.rasahub.product_inventory_service.service.InventoryItemService;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class InventoryItemServiceImpl implements InventoryItemService {

    private final InventoryItemRepository inventoryItemRepository;

    public InventoryItemServiceImpl(
            InventoryItemRepository inventoryItemRepository
    ) {
        this.inventoryItemRepository = inventoryItemRepository;
    }

    @Override
    public InventoryItemResponse createInventoryItem(
            InventoryItemCreateRequest request
    ) {

        String itemName = normalizeItemName(
                request.getItemName()
        );

        String description = normalizeOptionalText(
                request.getDescription()
        );

        if (request.getItemType() == null ||
                request.getMeasurementUnit() == null) {
            throw new BusinessRuleException(
                    "Item type and measurement unit are required"
            );
        }

        if (inventoryItemRepository.existsByItemNameIgnoreCase(itemName)) {
            throw new BusinessRuleException(
                    "Inventory item already exists"
            );
        }

        Long sequenceValue = inventoryItemRepository
                .getNextInventoryItemCodeSequenceValue();

        if (sequenceValue == null || sequenceValue < 1) {
            throw new BusinessRuleException(
                    "Unable to generate inventory item code"
            );
        }

        String itemCode = String.format(
                "INV%03d",
                sequenceValue
        );

        if (itemCode.length() > 10) {
            throw new BusinessRuleException(
                    "Inventory item code exceeds 10 characters"
            );
        }

        if (inventoryItemRepository.existsByItemCode(itemCode)) {
            throw new BusinessRuleException(
                    "Generated item code already exists. Check the database sequence"
            );
        }

        InventoryItem inventoryItem = new InventoryItem(
                itemCode,
                itemName,
                description,
                request.getItemType(),
                request.getMeasurementUnit()
        );

        InventoryItem savedItem =
                inventoryItemRepository.save(inventoryItem);

        return new InventoryItemResponse(savedItem);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryItemResponse> getInventoryItems(
            Boolean active
    ) {

        List<InventoryItem> items;

        if (active == null) {
            items = inventoryItemRepository.findAll(
                    Sort.by(Sort.Direction.ASC, "id")
            );
        } else {
            items = inventoryItemRepository
                    .findByActiveOrderByIdAsc(active);
        }

        return items.stream()
                .map(InventoryItemResponse::new)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryItemResponse getInventoryItemById(
            Long id
    ) {

        InventoryItem item = findInventoryItemById(id);

        return new InventoryItemResponse(item);
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryItemResponse getInventoryItemByCode(
            String itemCode
    ) {

        if (itemCode == null || itemCode.isBlank()) {
            throw new BusinessRuleException(
                    "Inventory item code is required"
            );
        }

        String normalizedCode = itemCode.trim()
                .toUpperCase(Locale.ROOT);

        InventoryItem item = inventoryItemRepository
                .findByItemCodeIgnoreCase(normalizedCode)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Inventory item not found"
                        )
                );

        return new InventoryItemResponse(item);
    }

    @Override
    public InventoryItemResponse updateInventoryItem(
            Long id,
            InventoryItemUpdateRequest request
    ) {

        InventoryItem item = findInventoryItemById(id);

        boolean hasUpdate = false;

        if (request.getItemName() != null) {

            String itemName = normalizeItemName(
                    request.getItemName()
            );

            if (inventoryItemRepository
                    .existsByItemNameIgnoreCaseAndIdNot(
                            itemName,
                            id
                    )) {
                throw new BusinessRuleException(
                        "Inventory item name already exists"
                );
            }

            item.setItemName(itemName);
            hasUpdate = true;
        }

        if (request.getDescription() != null) {
            item.setDescription(
                    normalizeOptionalText(request.getDescription())
            );
            hasUpdate = true;
        }

        if (request.getItemType() != null) {
            item.setItemType(request.getItemType());
            hasUpdate = true;
        }

        if (request.getMeasurementUnit() != null) {

            if (request.getMeasurementUnit() != item.getMeasurementUnit()) {
                throw new BusinessRuleException(
                        "Measurement unit cannot be changed after item creation"
                );
            }

            hasUpdate = true;
        }

        if (!hasUpdate) {
            throw new BusinessRuleException(
                    "Provide at least one field to update"
            );
        }

        InventoryItem updatedItem =
                inventoryItemRepository.save(item);

        return new InventoryItemResponse(updatedItem);
    }

    @Override
    public InventoryItemResponse updateInventoryItemStatus(
            Long id,
            boolean active
    ) {

        InventoryItem item = findInventoryItemById(id);

        if (item.isActive() == active) {
            throw new BusinessRuleException(
                    active
                            ? "Inventory item is already active"
                            : "Inventory item is already inactive"
            );
        }

        item.setActive(active);

        InventoryItem updatedItem =
                inventoryItemRepository.save(item);

        return new InventoryItemResponse(updatedItem);
    }

    private InventoryItem findInventoryItemById(Long id) {

        return inventoryItemRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Inventory item not found"
                        )
                );
    }

    private String normalizeItemName(String itemName) {

        if (itemName == null || itemName.isBlank()) {
            throw new BusinessRuleException(
                    "Inventory item name is required"
            );
        }

        String normalized = itemName.trim();

        if (normalized.length() > 150) {
            throw new BusinessRuleException(
                    "Item name must not exceed 150 characters"
            );
        }

        return normalized;
    }

    private String normalizeOptionalText(String text) {

        if (text == null || text.isBlank()) {
            return null;
        }

        String normalized = text.trim();

        if (normalized.length() > 500) {
            throw new BusinessRuleException(
                    "Description must not exceed 500 characters"
            );
        }

        return normalized;
    }
}
