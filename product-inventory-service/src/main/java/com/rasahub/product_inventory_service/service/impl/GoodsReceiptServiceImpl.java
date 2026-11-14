
package com.rasahub.product_inventory_service.service.impl;

import com.rasahub.product_inventory_service.dto.goodsreceipt.GoodsReceiptCreateRequest;
import com.rasahub.product_inventory_service.dto.goodsreceipt.GoodsReceiptItemRequest;
import com.rasahub.product_inventory_service.dto.goodsreceipt.GoodsReceiptRejectRequest;
import com.rasahub.product_inventory_service.dto.goodsreceipt.GoodsReceiptResponse;

import com.rasahub.product_inventory_service.entity.GoodsReceiptItem;
import com.rasahub.product_inventory_service.entity.GoodsReceiptNote;
import com.rasahub.product_inventory_service.entity.InventoryItem;
import com.rasahub.product_inventory_service.entity.PurchaseOrder;
import com.rasahub.product_inventory_service.entity.PurchaseOrderItem;
import com.rasahub.product_inventory_service.entity.StockBalance;
import com.rasahub.product_inventory_service.entity.StockLocation;
import com.rasahub.product_inventory_service.entity.StockMovement;

import com.rasahub.product_inventory_service.enums.GoodsReceiptStatus;
import com.rasahub.product_inventory_service.enums.PurchaseOrderStatus;
import com.rasahub.product_inventory_service.enums.StockMovementType;

import com.rasahub.product_inventory_service.exception.BusinessRuleException;
import com.rasahub.product_inventory_service.exception.ResourceNotFoundException;

import com.rasahub.product_inventory_service.repository.GoodsReceiptNoteRepository;
import com.rasahub.product_inventory_service.repository.PurchaseOrderRepository;
import com.rasahub.product_inventory_service.repository.StockBalanceRepository;
import com.rasahub.product_inventory_service.repository.StockMovementRepository;

import com.rasahub.product_inventory_service.security.ProductUserPrincipal;
import com.rasahub.product_inventory_service.service.GoodsReceiptService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class GoodsReceiptServiceImpl
        implements GoodsReceiptService {

    private final GoodsReceiptNoteRepository goodsReceiptNoteRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final StockBalanceRepository stockBalanceRepository;
    private final StockMovementRepository stockMovementRepository;

    public GoodsReceiptServiceImpl(
            GoodsReceiptNoteRepository goodsReceiptNoteRepository,
            PurchaseOrderRepository purchaseOrderRepository,
            StockBalanceRepository stockBalanceRepository,
            StockMovementRepository stockMovementRepository
    ) {
        this.goodsReceiptNoteRepository = goodsReceiptNoteRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.stockBalanceRepository = stockBalanceRepository;
        this.stockMovementRepository = stockMovementRepository;
    }

    @Override
    @Transactional
    public GoodsReceiptResponse createGoodsReceipt(
            GoodsReceiptCreateRequest request
    ) {
        ProductUserPrincipal user = currentUser();

        requireAnyRole(user, "STOCK_KEEPER");

        if (request == null) {
            throw new BusinessRuleException(
                    "Goods receipt request is required"
            );
        }

        validateId(request.getPurchaseOrderId(), "Purchase order");

        if (request.getItems() == null
                || request.getItems().isEmpty()) {
            throw new BusinessRuleException(
                    "Goods receipt must contain at least one item"
            );
        }

        PurchaseOrder purchaseOrder = purchaseOrderRepository
                .findWithItemsById(request.getPurchaseOrderId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Purchase order not found"
                        )
                );

        validateReceivablePurchaseOrder(purchaseOrder);

        GoodsReceiptNote grn;

        try {
            grn = new GoodsReceiptNote(
                    purchaseOrder,
                    user.getAccountId(),
                    request.getReceivedDate(),
                    request.getDeliveryReference(),
                    request.getRemarks()
            );
        } catch (IllegalArgumentException exception) {
            throw new BusinessRuleException(
                    exception.getMessage()
            );
        }

        Map<Long, PurchaseOrderItem> orderItems =
                buildOrderItemMap(purchaseOrder);

        Set<Long> processedItemIds = new HashSet<>();

        for (GoodsReceiptItemRequest itemRequest : request.getItems()) {

            if (itemRequest == null
                    || itemRequest.getInventoryItemId() == null
                    || itemRequest.getInventoryItemId() <= 0) {
                throw new BusinessRuleException(
                        "Valid inventory item ID is required"
                );
            }

            Long inventoryItemId = itemRequest.getInventoryItemId();

            if (!processedItemIds.add(inventoryItemId)) {
                throw new BusinessRuleException(
                        "Duplicate inventory item in GRN"
                );
            }

            PurchaseOrderItem orderItem =
                    orderItems.get(inventoryItemId);

            if (orderItem == null) {
                throw new BusinessRuleException(
                        "Inventory item is not in the purchase order"
                );
            }

            InventoryItem inventoryItem =
                    orderItem.getInventoryItem();

            try {
                grn.addItem(
                        inventoryItem,
                        itemRequest.getDeliveredQuantity(),
                        itemRequest.getAcceptedQuantity(),
                        itemRequest.getRejectedQuantity()
                );
            } catch (IllegalArgumentException
                     | IllegalStateException exception) {
                throw new BusinessRuleException(
                        exception.getMessage()
                );
            }

            if (itemRequest.getAcceptedQuantity()
                    .compareTo(orderItem.getOutstandingQuantity()) > 0) {
                throw new BusinessRuleException(
                        "Accepted quantity exceeds outstanding PO quantity"
                );
            }
        }

        GoodsReceiptNote savedGrn =
                goodsReceiptNoteRepository.saveAndFlush(grn);

        return new GoodsReceiptResponse(savedGrn);
    }

    @Override
    @Transactional
    public GoodsReceiptResponse submitGoodsReceipt(
            Long goodsReceiptId
    ) {
        ProductUserPrincipal user = currentUser();

        requireAnyRole(user, "STOCK_KEEPER");

        validateId(goodsReceiptId, "Goods receipt");

        GoodsReceiptNote grn = goodsReceiptNoteRepository
                .findByIdForUpdate(goodsReceiptId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Goods receipt not found"
                        )
                );

        requireCreator(grn, user.getAccountId());

        validateReceivablePurchaseOrder(grn.getPurchaseOrder());

        try {
            grn.submit();
        } catch (IllegalArgumentException
                 | IllegalStateException exception) {
            throw new BusinessRuleException(
                    exception.getMessage()
            );
        }

        return new GoodsReceiptResponse(
                goodsReceiptNoteRepository.saveAndFlush(grn)
        );
    }

    @Override
    @Transactional
    public GoodsReceiptResponse confirmGoodsReceipt(
            Long goodsReceiptId
    ) {
        ProductUserPrincipal user = currentUser();

        requireAnyRole(
                user,
                "OWNER",
                "MAIN_OFFICE_MANAGER"
        );

        validateId(goodsReceiptId, "Goods receipt");

        GoodsReceiptNote grn = goodsReceiptNoteRepository
                .findByIdForUpdate(goodsReceiptId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Goods receipt not found"
                        )
                );

        if (grn.getStatus() != GoodsReceiptStatus.SUBMITTED) {
            throw new BusinessRuleException(
                    "Only submitted GRNs can be confirmed"
            );
        }

        if (user.getAccountId().equals(
                grn.getReceivedByAccountId()
        )) {
            throw new BusinessRuleException(
                    "GRN creator cannot confirm their own GRN"
            );
        }

        PurchaseOrder purchaseOrder = purchaseOrderRepository
                .findByIdForUpdate(grn.getPurchaseOrder().getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Purchase order not found"
                        )
                );

        validateReceivablePurchaseOrder(purchaseOrder);

        StockLocation location = grn.getReceivingLocation();

        if (!"Main".equals(location.getLocationCode())
                || !location.getLocationCode().equals(
                purchaseOrder.getDestinationLocation()
                        .getLocationCode()
        )
                || !location.isActive()) {
            throw new BusinessRuleException(
                    "GRN receiving location must be active Main warehouse"
            );
        }

        if (grn.getItems().isEmpty()) {
            throw new BusinessRuleException(
                    "GRN must contain at least one item"
            );
        }

        Map<Long, PurchaseOrderItem> orderItems =
                buildOrderItemMap(purchaseOrder);

        List<GoodsReceiptItem> receiptItems =
                new ArrayList<>(grn.getItems());

        receiptItems.sort(
                Comparator.comparing(
                        item -> item.getInventoryItem().getId()
                )
        );

        for (GoodsReceiptItem receiptItem : receiptItems) {

            Long itemId = receiptItem.getInventoryItem().getId();

            PurchaseOrderItem orderItem = orderItems.get(itemId);

            if (orderItem == null) {
                throw new BusinessRuleException(
                        "GRN contains an item not found in the PO"
                );
            }

            BigDecimal accepted = receiptItem.getAcceptedQuantity();

            if (accepted == null || accepted.signum() < 0) {
                throw new BusinessRuleException(
                        "Invalid accepted quantity"
                );
            }

            if (accepted.compareTo(
                    orderItem.getOutstandingQuantity()
            ) > 0) {
                throw new BusinessRuleException(
                        "Accepted quantity exceeds outstanding quantity for item: "
                                + itemId
                );
            }
        }

        for (GoodsReceiptItem receiptItem : receiptItems) {

            BigDecimal accepted = receiptItem.getAcceptedQuantity();

            if (accepted.signum() == 0) {
                continue;
            }

            InventoryItem inventoryItem =
                    receiptItem.getInventoryItem();

            try {
                purchaseOrder.recordAcceptedReceipt(
                        inventoryItem.getId(),
                        accepted
                );
            } catch (IllegalArgumentException
                     | IllegalStateException exception) {
                throw new BusinessRuleException(
                        exception.getMessage()
                );
            }

            StockBalance balance = stockBalanceRepository
                    .findForUpdate(
                            location.getLocationCode(),
                            inventoryItem.getId()
                    )
                    .orElseGet(() ->
                            new StockBalance(
                                    location,
                                    inventoryItem
                            )
                    );

            BigDecimal quantityBefore =
                    balance.getQuantityOnHand();

            BigDecimal quantityAfter =
                    quantityBefore.add(accepted);

            validateStockQuantity(quantityAfter);

            try {
                balance.increaseQuantity(accepted);
            } catch (IllegalArgumentException
                     | IllegalStateException exception) {
                throw new BusinessRuleException(
                        exception.getMessage()
                );
            }

            stockBalanceRepository.save(balance);

            StockMovement movement = new StockMovement(
                    location,
                    inventoryItem,
                    StockMovementType.GOODS_RECEIPT,
                    accepted,
                    quantityBefore,
                    quantityAfter,
                    "GRN",
                    grn.getId().toString(),
                    user.getAccountId(),
                    "Stock received from confirmed GRN"
            );

            stockMovementRepository.save(movement);
        }

        try {
            grn.confirm(user.getAccountId());
        } catch (IllegalArgumentException
                 | IllegalStateException exception) {
            throw new BusinessRuleException(
                    exception.getMessage()
            );
        }

        purchaseOrderRepository.saveAndFlush(purchaseOrder);

        GoodsReceiptNote savedGrn =
                goodsReceiptNoteRepository.saveAndFlush(grn);

        stockBalanceRepository.flush();
        stockMovementRepository.flush();

        return new GoodsReceiptResponse(savedGrn);
    }

    @Override
    @Transactional
    public GoodsReceiptResponse rejectGoodsReceipt(
            Long goodsReceiptId,
            GoodsReceiptRejectRequest request
    ) {
        ProductUserPrincipal user = currentUser();

        requireAnyRole(
                user,
                "OWNER",
                "MAIN_OFFICE_MANAGER"
        );

        validateId(goodsReceiptId, "Goods receipt");

        if (request == null
                || request.getRejectionReason() == null
                || request.getRejectionReason().isBlank()
                || request.getRejectionReason().trim().length() > 500) {
            throw new BusinessRuleException(
                    "Valid rejection reason is required"
            );
        }

        GoodsReceiptNote grn = goodsReceiptNoteRepository
                .findByIdForUpdate(goodsReceiptId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Goods receipt not found"
                        )
                );

        try {
            grn.reject(
                    user.getAccountId(),
                    request.getRejectionReason()
            );
        } catch (IllegalArgumentException
                 | IllegalStateException exception) {
            throw new BusinessRuleException(
                    exception.getMessage()
            );
        }

        return new GoodsReceiptResponse(
                goodsReceiptNoteRepository.saveAndFlush(grn)
        );
    }

    @Override
    public GoodsReceiptResponse getGoodsReceiptById(
            Long goodsReceiptId
    ) {
        ProductUserPrincipal user = currentUser();

        requireAnyRole(
                user,
                "OWNER",
                "MAIN_OFFICE_MANAGER",
                "ACCOUNTANT",
                "STOCK_KEEPER"
        );

        validateId(goodsReceiptId, "Goods receipt");

        GoodsReceiptNote grn = goodsReceiptNoteRepository
                .findWithItemsById(goodsReceiptId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Goods receipt not found"
                        )
                );

        if ("STOCK_KEEPER".equals(user.getRole())) {
            requireCreator(grn, user.getAccountId());
        }

        return new GoodsReceiptResponse(grn);
    }

    @Override
    public Page<GoodsReceiptResponse> getAllGoodsReceipts(
            int page,
            int size
    ) {
        requireAnyRole(
                currentUser(),
                "OWNER",
                "MAIN_OFFICE_MANAGER",
                "ACCOUNTANT"
        );

        Pageable pageable = createPageable(page, size);

        return goodsReceiptNoteRepository
                .findAllByOrderByCreatedAtDescIdDesc(pageable)
                .map(GoodsReceiptResponse::new);
    }

    @Override
    public Page<GoodsReceiptResponse> getGoodsReceiptsByStatus(
            GoodsReceiptStatus status,
            int page,
            int size
    ) {
        requireAnyRole(
                currentUser(),
                "OWNER",
                "MAIN_OFFICE_MANAGER",
                "ACCOUNTANT"
        );

        if (status == null) {
            throw new BusinessRuleException(
                    "GRN status is required"
            );
        }

        Pageable pageable = createPageable(page, size);

        return goodsReceiptNoteRepository
                .findByStatusOrderByCreatedAtDescIdDesc(
                        status,
                        pageable
                )
                .map(GoodsReceiptResponse::new);
    }

    @Override
    public Page<GoodsReceiptResponse> getGoodsReceiptsByPurchaseOrder(
            Long purchaseOrderId,
            int page,
            int size
    ) {
        requireAnyRole(
                currentUser(),
                "OWNER",
                "MAIN_OFFICE_MANAGER",
                "ACCOUNTANT"
        );

        validateId(purchaseOrderId, "Purchase order");

        if (!purchaseOrderRepository.existsById(purchaseOrderId)) {
            throw new ResourceNotFoundException(
                    "Purchase order not found"
            );
        }

        Pageable pageable = createPageable(page, size);

        return goodsReceiptNoteRepository
                .findByPurchaseOrder_IdOrderByCreatedAtDescIdDesc(
                        purchaseOrderId,
                        pageable
                )
                .map(GoodsReceiptResponse::new);
    }

    @Override
    public Page<GoodsReceiptResponse> getMyGoodsReceipts(
            int page,
            int size
    ) {
        ProductUserPrincipal user = currentUser();

        requireAnyRole(user, "STOCK_KEEPER");

        Pageable pageable = createPageable(page, size);

        return goodsReceiptNoteRepository
                .findByReceivedByAccountIdOrderByCreatedAtDescIdDesc(
                        user.getAccountId(),
                        pageable
                )
                .map(GoodsReceiptResponse::new);
    }

    private Map<Long, PurchaseOrderItem> buildOrderItemMap(
            PurchaseOrder purchaseOrder
    ) {
        Map<Long, PurchaseOrderItem> items = new HashMap<>();

        for (PurchaseOrderItem item : purchaseOrder.getItems()) {
            items.put(
                    item.getInventoryItem().getId(),
                    item
            );
        }

        return items;
    }

    private void validateReceivablePurchaseOrder(
            PurchaseOrder purchaseOrder
    ) {
        if (purchaseOrder.getStatus() != PurchaseOrderStatus.ISSUED
                && purchaseOrder.getStatus()
                != PurchaseOrderStatus.PARTIALLY_RECEIVED) {
            throw new BusinessRuleException(
                    "Purchase order is not eligible for receiving"
            );
        }

        StockLocation location =
                purchaseOrder.getDestinationLocation();

        if (location == null
                || !"Main".equals(location.getLocationCode())
                || !location.isActive()) {
            throw new BusinessRuleException(
                    "Receiving location must be active Main warehouse"
            );
        }
    }

    private void validateStockQuantity(BigDecimal quantity) {
        if (quantity == null
                || quantity.signum() < 0
                || quantity.scale() > 3
                || quantity.precision() - quantity.scale() > 15) {
            throw new BusinessRuleException(
                    "Resulting stock quantity exceeds allowed limits"
            );
        }
    }

    private void requireCreator(
            GoodsReceiptNote grn,
            Long accountId
    ) {
        if (!grn.getReceivedByAccountId().equals(accountId)) {
            throw new AccessDeniedException(
                    "You can only access your own goods receipts"
            );
        }
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

        return PageRequest.of(page, size);
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
