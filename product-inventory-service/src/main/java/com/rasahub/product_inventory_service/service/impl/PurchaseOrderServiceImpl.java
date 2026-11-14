
package com.rasahub.product_inventory_service.service.impl;

import com.rasahub.product_inventory_service.dto.purchaseorder.PurchaseOrderCancelRequest;
import com.rasahub.product_inventory_service.dto.purchaseorder.PurchaseOrderCreateRequest;
import com.rasahub.product_inventory_service.dto.purchaseorder.PurchaseOrderItemRequest;
import com.rasahub.product_inventory_service.dto.purchaseorder.PurchaseOrderResponse;

import com.rasahub.product_inventory_service.entity.InventoryItem;
import com.rasahub.product_inventory_service.entity.PurchaseOrder;
import com.rasahub.product_inventory_service.entity.PurchaseOrderItem;
import com.rasahub.product_inventory_service.entity.PurchaseRequest;
import com.rasahub.product_inventory_service.entity.PurchaseRequestItem;
import com.rasahub.product_inventory_service.entity.Supplier;
import com.rasahub.product_inventory_service.entity.SupplierItem;

import com.rasahub.product_inventory_service.enums.PurchaseOrderStatus;
import com.rasahub.product_inventory_service.enums.PurchaseRequestStatus;

import com.rasahub.product_inventory_service.exception.BusinessRuleException;
import com.rasahub.product_inventory_service.exception.ResourceNotFoundException;

import com.rasahub.product_inventory_service.repository.PurchaseOrderRepository;
import com.rasahub.product_inventory_service.repository.PurchaseRequestRepository;
import com.rasahub.product_inventory_service.repository.SupplierRepository;
import com.rasahub.product_inventory_service.repository.SupplierItemRepository;

import com.rasahub.product_inventory_service.security.ProductUserPrincipal;
import com.rasahub.product_inventory_service.service.PurchaseOrderService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class PurchaseOrderServiceImpl
        implements PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseRequestRepository purchaseRequestRepository;
    private final SupplierRepository supplierRepository;
    private final SupplierItemRepository supplierItemRepository;

    public PurchaseOrderServiceImpl(
            PurchaseOrderRepository purchaseOrderRepository,
            PurchaseRequestRepository purchaseRequestRepository,
            SupplierRepository supplierRepository,
            SupplierItemRepository supplierItemRepository
    ) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.purchaseRequestRepository = purchaseRequestRepository;
        this.supplierRepository = supplierRepository;
        this.supplierItemRepository = supplierItemRepository;
    }

    @Override
    @Transactional
    public PurchaseOrderResponse createPurchaseOrder(
            PurchaseOrderCreateRequest request
    ) {
        ProductUserPrincipal user = currentUser();

        requireAnyRole(
                user,
                "OWNER",
                "MAIN_OFFICE_MANAGER"
        );

        if (request == null
                || request.getPurchaseRequestId() == null
                || request.getPurchaseRequestId() <= 0
                || request.getSupplierId() == null
                || request.getSupplierId() <= 0) {
            throw new BusinessRuleException(
                    "Valid purchase request and supplier IDs are required"
            );
        }

        if (request.getExpectedDeliveryDate() == null
                || request.getExpectedDeliveryDate()
                .isBefore(LocalDate.now())) {
            throw new BusinessRuleException(
                    "Expected delivery date cannot be in the past"
            );
        }

        if (request.getRemarks() != null
                && request.getRemarks().trim().length() > 300) {
            throw new BusinessRuleException(
                    "Remarks must not exceed 300 characters"
            );
        }

        if (request.getItems() == null
                || request.getItems().isEmpty()) {
            throw new BusinessRuleException(
                    "Purchase order must contain at least one item"
            );
        }

        PurchaseRequest purchaseRequest =
                purchaseRequestRepository
                        .findByIdForUpdate(
                                request.getPurchaseRequestId()
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Purchase request not found"
                                )
                        );

        if (purchaseRequest.getStatus()
                != PurchaseRequestStatus.APPROVED) {
            throw new BusinessRuleException(
                    "Only approved purchase requests can create a PO"
            );
        }

        if (purchaseOrderRepository
                .existsByPurchaseRequest_Id(
                        purchaseRequest.getId()
                )) {
            throw new BusinessRuleException(
                    "Purchase order already exists for this PR"
            );
        }

        Supplier supplier = supplierRepository
                .findById(request.getSupplierId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Supplier not found"
                        )
                );

        if (!supplier.isActive()) {
            throw new BusinessRuleException(
                    "Cannot create a PO for an inactive supplier"
            );
        }

        Map<Long, PurchaseRequestItem> requestedItems =
                new HashMap<>();

        for (PurchaseRequestItem item
                : purchaseRequest.getItems()) {

            requestedItems.put(
                    item.getInventoryItem().getId(),
                    item
            );
        }

        if (requestedItems.isEmpty()
                || requestedItems.size()
                != request.getItems().size()) {
            throw new BusinessRuleException(
                    "PO items must match all approved PR items"
            );
        }

        PurchaseOrder purchaseOrder;

        try {
            purchaseOrder = new PurchaseOrder(
                    purchaseRequest,
                    supplier,
                    user.getAccountId(),
                    request.getExpectedDeliveryDate(),
                    request.getRemarks()
            );
        } catch (IllegalArgumentException exception) {
            throw new BusinessRuleException(
                    exception.getMessage()
            );
        }

        Set<Long> processedItemIds = new HashSet<>();

        for (PurchaseOrderItemRequest itemRequest
                : request.getItems()) {

            if (itemRequest == null
                    || itemRequest.getInventoryItemId() == null
                    || itemRequest.getInventoryItemId() <= 0) {
                throw new BusinessRuleException(
                        "Valid inventory item ID is required"
                );
            }

            Long inventoryItemId =
                    itemRequest.getInventoryItemId();

            if (!processedItemIds.add(inventoryItemId)) {
                throw new BusinessRuleException(
                        "Duplicate inventory item in purchase order"
                );
            }

            PurchaseRequestItem requestedItem =
                    requestedItems.get(inventoryItemId);

            if (requestedItem == null) {
                throw new BusinessRuleException(
                        "Inventory item is not in the approved PR"
                );
            }

            InventoryItem inventoryItem =
                    requestedItem.getInventoryItem();

            if (!inventoryItem.isActive()) {
                throw new BusinessRuleException(
                        "Cannot order an inactive inventory item"
                );
            }

            BigDecimal orderedQuantity =
                    itemRequest.getOrderedQuantity();

            if (orderedQuantity == null
                    || requestedItem.getRequestedQuantity()
                    .compareTo(orderedQuantity) != 0) {
                throw new BusinessRuleException(
                        "Ordered quantity must match approved PR quantity"
                );
            }

            SupplierItem supplierItem =
                    supplierItemRepository
                            .findBySupplier_IdAndInventoryItem_Id(
                                    supplier.getId(),
                                    inventoryItemId
                            )
                            .orElseThrow(() ->
                                    new BusinessRuleException(
                                            "Supplier does not supply item: "
                                                    + inventoryItemId
                                    )
                            );

            if (!supplierItem.isActive()) {
                throw new BusinessRuleException(
                        "Supplier item mapping is inactive: "
                                + inventoryItemId
                );
            }

            BigDecimal minimumQuantity =
                    supplierItem.getMinimumOrderQuantity();

            if (minimumQuantity == null
                    || orderedQuantity.compareTo(minimumQuantity) < 0) {
                throw new BusinessRuleException(
                        "Ordered quantity is below supplier minimum for item: "
                                + inventoryItemId
                );
            }

            BigDecimal supplierUnitPrice =
                    supplierItem.getUnitPrice();

            if (supplierUnitPrice == null
                    || itemRequest.getUnitPrice() == null
                    || supplierUnitPrice.compareTo(
                    itemRequest.getUnitPrice()
            ) != 0) {
                throw new BusinessRuleException(
                        "Unit price does not match supplier price for item: "
                                + inventoryItemId
                );
            }

            try {
                purchaseOrder.addItem(
                        inventoryItem,
                        orderedQuantity,
                        supplierUnitPrice
                );
            } catch (IllegalArgumentException
                     | IllegalStateException exception) {
                throw new BusinessRuleException(
                        exception.getMessage()
                );
            }
        }

        if (processedItemIds.size() != requestedItems.size()) {
            throw new BusinessRuleException(
                    "All approved PR items must be included in PO"
            );
        }

        purchaseRequest.markPurchaseOrderCreated();

        purchaseRequestRepository.saveAndFlush(
                purchaseRequest
        );

        PurchaseOrder savedPurchaseOrder =
                purchaseOrderRepository.saveAndFlush(
                        purchaseOrder
                );

        return new PurchaseOrderResponse(
                savedPurchaseOrder
        );
    }

    @Override
    public PurchaseOrderResponse getPurchaseOrderById(
            Long purchaseOrderId
    ) {
        requireAnyRole(
                currentUser(),
                "OWNER",
                "MAIN_OFFICE_MANAGER",
                "ACCOUNTANT",
                "STOCK_KEEPER"
        );

        validateId(purchaseOrderId, "Purchase order");

        PurchaseOrder purchaseOrder =
                purchaseOrderRepository
                        .findWithItemsById(purchaseOrderId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Purchase order not found"
                                )
                        );

        return new PurchaseOrderResponse(
                purchaseOrder
        );
    }

    @Override
    public Page<PurchaseOrderResponse> getAllPurchaseOrders(
            int page,
            int size
    ) {
        requireAnyRole(
                currentUser(),
                "OWNER",
                "MAIN_OFFICE_MANAGER",
                "ACCOUNTANT",
                "STOCK_KEEPER"
        );

        Pageable pageable = createPageable(page, size);

        return purchaseOrderRepository
                .findAllByOrderByCreatedAtDescIdDesc(
                        pageable
                )
                .map(PurchaseOrderResponse::new);
    }

    @Override
    public Page<PurchaseOrderResponse> getPurchaseOrdersByStatus(
            PurchaseOrderStatus status,
            int page,
            int size
    ) {
        requireAnyRole(
                currentUser(),
                "OWNER",
                "MAIN_OFFICE_MANAGER",
                "ACCOUNTANT",
                "STOCK_KEEPER"
        );

        if (status == null) {
            throw new BusinessRuleException(
                    "Purchase order status is required"
            );
        }

        Pageable pageable = createPageable(page, size);

        return purchaseOrderRepository
                .findByStatusOrderByCreatedAtDescIdDesc(
                        status,
                        pageable
                )
                .map(PurchaseOrderResponse::new);
    }

    @Override
    public Page<PurchaseOrderResponse> getPurchaseOrdersBySupplier(
            Long supplierId,
            int page,
            int size
    ) {
        requireAnyRole(
                currentUser(),
                "OWNER",
                "MAIN_OFFICE_MANAGER",
                "ACCOUNTANT",
                "STOCK_KEEPER"
        );

        validateId(supplierId, "Supplier");

        if (!supplierRepository.existsById(supplierId)) {
            throw new ResourceNotFoundException(
                    "Supplier not found"
            );
        }

        Pageable pageable = createPageable(page, size);

        return purchaseOrderRepository
                .findBySupplier_IdOrderByCreatedAtDescIdDesc(
                        supplierId,
                        pageable
                )
                .map(PurchaseOrderResponse::new);
    }

    @Override
    public Page<PurchaseOrderResponse> getMyPurchaseOrders(
            int page,
            int size
    ) {
        ProductUserPrincipal user = currentUser();

        requireAnyRole(
                user,
                "OWNER",
                "MAIN_OFFICE_MANAGER"
        );

        Pageable pageable = createPageable(page, size);

        return purchaseOrderRepository
                .findByCreatedByAccountIdOrderByCreatedAtDescIdDesc(
                        user.getAccountId(),
                        pageable
                )
                .map(PurchaseOrderResponse::new);
    }

    @Override
    public PurchaseOrderResponse getPurchaseOrderByPurchaseRequestId(
            Long purchaseRequestId
    ) {
        requireAnyRole(
                currentUser(),
                "OWNER",
                "MAIN_OFFICE_MANAGER",
                "ACCOUNTANT",
                "STOCK_KEEPER"
        );

        validateId(purchaseRequestId, "Purchase request");

        PurchaseOrder purchaseOrder =
                purchaseOrderRepository
                        .findByPurchaseRequest_Id(
                                purchaseRequestId
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Purchase order not found for PR"
                                )
                        );

        return new PurchaseOrderResponse(
                purchaseOrder
        );
    }

    @Override
    @Transactional
    public PurchaseOrderResponse cancelPurchaseOrder(
            Long purchaseOrderId,
            PurchaseOrderCancelRequest request
    ) {
        requireAnyRole(
                currentUser(),
                "OWNER",
                "MAIN_OFFICE_MANAGER"
        );

        validateId(purchaseOrderId, "Purchase order");

        if (request == null
                || request.getCancellationReason() == null
                || request.getCancellationReason().isBlank()
                || request.getCancellationReason().trim().length() > 300) {
            throw new BusinessRuleException(
                    "Valid cancellation reason is required"
            );
        }

        PurchaseOrder purchaseOrder =
                purchaseOrderRepository
                        .findByIdForUpdate(purchaseOrderId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Purchase order not found"
                                )
                        );

        if (purchaseOrder.getStatus()
                != PurchaseOrderStatus.ISSUED) {
            throw new BusinessRuleException(
                    "Only unreceived issued POs can be cancelled"
            );
        }

        boolean hasReceivedItems =
                purchaseOrder.getItems()
                        .stream()
                        .anyMatch(item ->
                                item.getReceivedQuantity()
                                        .compareTo(BigDecimal.ZERO) > 0
                        );

        if (hasReceivedItems) {
            throw new BusinessRuleException(
                    "Cannot cancel a PO with received quantities"
            );
        }

        try {
            purchaseOrder.cancel(
                    request.getCancellationReason()
            );
        } catch (IllegalArgumentException
                 | IllegalStateException exception) {
            throw new BusinessRuleException(
                    exception.getMessage()
            );
        }

        PurchaseOrder savedPurchaseOrder =
                purchaseOrderRepository.saveAndFlush(
                        purchaseOrder
                );

        return new PurchaseOrderResponse(
                savedPurchaseOrder
        );
    }

    private void validateId(
            Long id,
            String fieldName
    ) {
        if (id == null || id <= 0) {
            throw new BusinessRuleException(
                    "Valid " + fieldName + " ID is required"
            );
        }
    }

    private Pageable createPageable(
            int page,
            int size
    ) {
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

        return PageRequest.of(
                page,
                size
        );
    }

    private ProductUserPrincipal currentUser() {
        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal()
                instanceof ProductUserPrincipal user)) {
            throw new AccessDeniedException(
                    "Authenticated staff account is required"
            );
        }

        if (user.getAccountId() == null
                || user.getAccountId() <= 0) {
            throw new AccessDeniedException(
                    "Invalid authenticated account"
            );
        }

        return user;
    }

    private void requireAnyRole(
            ProductUserPrincipal user,
            String... allowedRoles
    ) {
        for (String role : allowedRoles) {
            if (role.equals(user.getRole())) {
                return;
            }
        }

        throw new AccessDeniedException(
                "You do not have permission for this operation"
        );
    }
}
