
package com.rasahub.product_inventory_service.service.impl;

import com.rasahub.product_inventory_service.dto.supplieritem.SupplierItemCreateRequest;
import com.rasahub.product_inventory_service.dto.supplieritem.SupplierItemResponse;
import com.rasahub.product_inventory_service.dto.supplieritem.SupplierItemUpdateRequest;

import com.rasahub.product_inventory_service.entity.InventoryItem;
import com.rasahub.product_inventory_service.entity.Supplier;
import com.rasahub.product_inventory_service.entity.SupplierItem;

import com.rasahub.product_inventory_service.exception.BusinessRuleException;
import com.rasahub.product_inventory_service.exception.ResourceNotFoundException;

import com.rasahub.product_inventory_service.repository.InventoryItemRepository;
import com.rasahub.product_inventory_service.repository.SupplierItemRepository;
import com.rasahub.product_inventory_service.repository.SupplierRepository;

import com.rasahub.product_inventory_service.service.SupplierItemService;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class SupplierItemServiceImpl implements SupplierItemService {

    private final SupplierItemRepository supplierItemRepository;
    private final SupplierRepository supplierRepository;
    private final InventoryItemRepository inventoryItemRepository;

    public SupplierItemServiceImpl(
            SupplierItemRepository supplierItemRepository,
            SupplierRepository supplierRepository,
            InventoryItemRepository inventoryItemRepository
    ) {
        this.supplierItemRepository = supplierItemRepository;
        this.supplierRepository = supplierRepository;
        this.inventoryItemRepository = inventoryItemRepository;
    }

    @Override
    @Transactional
    public SupplierItemResponse createSupplierItem(
            SupplierItemCreateRequest request
    ) {
        Supplier supplier = supplierRepository
                .findById(request.getSupplierId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Supplier not found"
                        )
                );

        InventoryItem inventoryItem = inventoryItemRepository
                .findById(request.getInventoryItemId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Inventory item not found"
                        )
                );

        if (!supplier.isActive()) {
            throw new BusinessRuleException(
                    "Cannot assign an inactive supplier"
            );
        }

        if (!inventoryItem.isActive()) {
            throw new BusinessRuleException(
                    "Cannot assign an inactive inventory item"
            );
        }

        boolean alreadyExists = supplierItemRepository
                .existsBySupplier_IdAndInventoryItem_Id(
                        supplier.getId(),
                        inventoryItem.getId()
                );

        if (alreadyExists) {
            throw new BusinessRuleException(
                    "Supplier item mapping already exists"
            );
        }

        SupplierItem supplierItem = new SupplierItem(
                supplier,
                inventoryItem,
                request.getUnitPrice(),
                request.getMinimumOrderQuantity(),
                request.getLeadTimeDays()
        );

        SupplierItem savedSupplierItem =
                supplierItemRepository.saveAndFlush(supplierItem);

        return new SupplierItemResponse(savedSupplierItem);
    }

    @Override
    public List<SupplierItemResponse> getSupplierItems(
            Boolean active
    ) {
        List<SupplierItem> supplierItems;

        if (active == null) {
            supplierItems = supplierItemRepository.findAll(
                    Sort.by(Sort.Direction.ASC, "id")
            );
        } else {
            supplierItems = supplierItemRepository
                    .findByActiveOrderByIdAsc(active);
        }

        return supplierItems.stream()
                .map(SupplierItemResponse::new)
                .toList();
    }

    @Override
    public SupplierItemResponse getSupplierItemById(
            Long id
    ) {
        SupplierItem supplierItem = supplierItemRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Supplier item mapping not found"
                        )
                );

        return new SupplierItemResponse(supplierItem);
    }

    @Override
    public SupplierItemResponse getSupplierItemBySupplierAndInventoryItem(
            Long supplierId,
            Long inventoryItemId
    ) {
        SupplierItem supplierItem = supplierItemRepository
                .findBySupplier_IdAndInventoryItem_Id(
                        supplierId,
                        inventoryItemId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Supplier item mapping not found"
                        )
                );

        return new SupplierItemResponse(supplierItem);
    }

    @Override
    public List<SupplierItemResponse> getSupplierItemsBySupplier(
            Long supplierId,
            Boolean active
    ) {
        if (!supplierRepository.existsById(supplierId)) {
            throw new ResourceNotFoundException(
                    "Supplier not found"
            );
        }

        List<SupplierItem> supplierItems;

        if (active == null) {
            supplierItems = supplierItemRepository
                    .findBySupplier_IdOrderByIdAsc(supplierId);
        } else {
            supplierItems = supplierItemRepository
                    .findBySupplier_IdAndActiveOrderByIdAsc(
                            supplierId,
                            active
                    );
        }

        return supplierItems.stream()
                .map(SupplierItemResponse::new)
                .toList();
    }

    @Override
    public List<SupplierItemResponse> getSupplierItemsByInventoryItem(
            Long inventoryItemId
    ) {
        if (!inventoryItemRepository.existsById(inventoryItemId)) {
            throw new ResourceNotFoundException(
                    "Inventory item not found"
            );
        }

        List<SupplierItem> supplierItems = supplierItemRepository
                .findByInventoryItem_IdOrderByIdAsc(
                        inventoryItemId
                );

        return supplierItems.stream()
                .map(SupplierItemResponse::new)
                .toList();
    }

    @Override
    @Transactional
    public SupplierItemResponse updateSupplierItem(
            Long id,
            SupplierItemUpdateRequest request
    ) {
        SupplierItem supplierItem = supplierItemRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Supplier item mapping not found"
                        )
                );

        if (request.getUnitPrice() == null
                && request.getMinimumOrderQuantity() == null
                && request.getLeadTimeDays() == null) {
            throw new BusinessRuleException(
                    "At least one field must be provided"
            );
        }

        if (request.getUnitPrice() != null) {
            supplierItem.setUnitPrice(
                    request.getUnitPrice()
            );
        }

        if (request.getMinimumOrderQuantity() != null) {
            supplierItem.setMinimumOrderQuantity(
                    request.getMinimumOrderQuantity()
            );
        }

        if (request.getLeadTimeDays() != null) {
            supplierItem.setLeadTimeDays(
                    request.getLeadTimeDays()
            );
        }

        SupplierItem updatedSupplierItem =
                supplierItemRepository.saveAndFlush(supplierItem);

        return new SupplierItemResponse(updatedSupplierItem);
    }

    @Override
    @Transactional
    public SupplierItemResponse updateSupplierItemStatus(
            Long id,
            boolean active
    ) {
        SupplierItem supplierItem = supplierItemRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Supplier item mapping not found"
                        )
                );

        if (active) {
            if (!supplierItem.getSupplier().isActive()) {
                throw new BusinessRuleException(
                        "Cannot activate mapping: supplier is inactive"
                );
            }

            if (!supplierItem.getInventoryItem().isActive()) {
                throw new BusinessRuleException(
                        "Cannot activate mapping: inventory item is inactive"
                );
            }
        }

        if (supplierItem.isActive() == active) {
            return new SupplierItemResponse(supplierItem);
        }

        supplierItem.setActive(active);

        SupplierItem updatedSupplierItem =
                supplierItemRepository.saveAndFlush(supplierItem);

        return new SupplierItemResponse(updatedSupplierItem);
    }
}
