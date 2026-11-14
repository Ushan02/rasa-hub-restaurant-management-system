
package com.rasahub.product_inventory_service.service.impl;

import com.rasahub.product_inventory_service.dto.purchaserequest.PurchaseRequestCreateRequest;
import com.rasahub.product_inventory_service.dto.purchaserequest.PurchaseRequestItemRequest;
import com.rasahub.product_inventory_service.dto.purchaserequest.PurchaseRequestRejectRequest;
import com.rasahub.product_inventory_service.dto.purchaserequest.PurchaseRequestResponse;

import com.rasahub.product_inventory_service.entity.InventoryItem;
import com.rasahub.product_inventory_service.entity.PurchaseRequest;
import com.rasahub.product_inventory_service.entity.StockLocation;

import com.rasahub.product_inventory_service.enums.PurchaseRequestStatus;

import com.rasahub.product_inventory_service.exception.BusinessRuleException;
import com.rasahub.product_inventory_service.exception.ResourceNotFoundException;

import com.rasahub.product_inventory_service.repository.InventoryItemRepository;
import com.rasahub.product_inventory_service.repository.PurchaseRequestRepository;
import com.rasahub.product_inventory_service.repository.StockLocationRepository;

import com.rasahub.product_inventory_service.security.ProductUserPrincipal;
import com.rasahub.product_inventory_service.service.PurchaseRequestService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class PurchaseRequestServiceImpl
        implements PurchaseRequestService {

    private final PurchaseRequestRepository purchaseRequestRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final StockLocationRepository stockLocationRepository;

    public PurchaseRequestServiceImpl(
            PurchaseRequestRepository purchaseRequestRepository,
            InventoryItemRepository inventoryItemRepository,
            StockLocationRepository stockLocationRepository
    ) {
        this.purchaseRequestRepository = purchaseRequestRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.stockLocationRepository = stockLocationRepository;
    }

    @Override
    @Transactional
    public PurchaseRequestResponse createPurchaseRequest(
            PurchaseRequestCreateRequest request
    ) {
        ProductUserPrincipal user = currentUser();

        requireAnyRole(user, "STOCK_KEEPER");

        if (request == null
                || request.getItems() == null
                || request.getItems().isEmpty()) {
            throw new BusinessRuleException(
                    "Purchase request must contain at least one item"
            );
        }

        StockLocation main = stockLocationRepository
                .findById("Main")
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Main warehouse not found"
                        )
                );

        if (!main.isActive()) {
            throw new BusinessRuleException(
                    "Main warehouse is inactive"
            );
        }

        PurchaseRequest purchaseRequest;

        try {
            purchaseRequest = new PurchaseRequest(
                    main,
                    user.getAccountId(),
                    request.getRemarks()
            );
        } catch (IllegalArgumentException exception) {
            throw new BusinessRuleException(
                    exception.getMessage()
            );
        }

        Set<Long> itemIds = new HashSet<>();

        for (PurchaseRequestItemRequest itemRequest
                : request.getItems()) {

            if (itemRequest == null
                    || itemRequest.getInventoryItemId() == null
                    || itemRequest.getInventoryItemId() <= 0) {
                throw new BusinessRuleException(
                        "Valid inventory item ID is required"
                );
            }

            Long itemId = itemRequest.getInventoryItemId();

            if (!itemIds.add(itemId)) {
                throw new BusinessRuleException(
                        "Duplicate inventory item in purchase request"
                );
            }

            InventoryItem inventoryItem =
                    inventoryItemRepository
                            .findById(itemId)
                            .orElseThrow(() ->
                                    new ResourceNotFoundException(
                                            "Inventory item not found: " + itemId
                                    )
                            );

            if (!inventoryItem.isActive()) {
                throw new BusinessRuleException(
                        "Cannot request an inactive inventory item"
                );
            }

            try {
                purchaseRequest.addItem(
                        inventoryItem,
                        itemRequest.getRequestedQuantity()
                );
            } catch (IllegalArgumentException
                     | IllegalStateException exception) {
                throw new BusinessRuleException(
                        exception.getMessage()
                );
            }
        }

        PurchaseRequest savedRequest =
                purchaseRequestRepository.saveAndFlush(
                        purchaseRequest
                );

        return new PurchaseRequestResponse(savedRequest);
    }

    @Override
    public PurchaseRequestResponse getPurchaseRequestById(
            Long purchaseRequestId
    ) {
        ProductUserPrincipal user = currentUser();

        requireAnyRole(
                user,
                "OWNER",
                "MAIN_OFFICE_MANAGER",
                "ACCOUNTANT",
                "STOCK_KEEPER"
        );

        validatePurchaseRequestId(purchaseRequestId);

        PurchaseRequest purchaseRequest =
                purchaseRequestRepository
                        .findWithItemsById(purchaseRequestId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Purchase request not found"
                                )
                        );

        if ("STOCK_KEEPER".equals(user.getRole())
                && !purchaseRequest.getRequestedByAccountId()
                .equals(user.getAccountId())) {
            throw new AccessDeniedException(
                    "You cannot view another user's purchase request"
            );
        }

        return new PurchaseRequestResponse(purchaseRequest);
    }

    @Override
    public Page<PurchaseRequestResponse> getAllPurchaseRequests(
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

        return purchaseRequestRepository
                .findAllByOrderByCreatedAtDescIdDesc(pageable)
                .map(PurchaseRequestResponse::new);
    }

    @Override
    public Page<PurchaseRequestResponse> getPurchaseRequestsByStatus(
            PurchaseRequestStatus status,
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
                    "Purchase request status is required"
            );
        }

        Pageable pageable = createPageable(page, size);

        return purchaseRequestRepository
                .findByStatusOrderByCreatedAtDescIdDesc(
                        status,
                        pageable
                )
                .map(PurchaseRequestResponse::new);
    }

    @Override
    public Page<PurchaseRequestResponse> getMyPurchaseRequests(
            int page,
            int size
    ) {
        ProductUserPrincipal user = currentUser();

        requireAnyRole(user, "STOCK_KEEPER");

        Pageable pageable = createPageable(page, size);

        return purchaseRequestRepository
                .findByRequestedByAccountIdOrderByCreatedAtDescIdDesc(
                        user.getAccountId(),
                        pageable
                )
                .map(PurchaseRequestResponse::new);
    }

    @Override
    @Transactional
    public PurchaseRequestResponse approvePurchaseRequest(
            Long purchaseRequestId
    ) {
        ProductUserPrincipal user = currentUser();

        requireAnyRole(
                user,
                "OWNER",
                "MAIN_OFFICE_MANAGER"
        );

        validatePurchaseRequestId(purchaseRequestId);

        PurchaseRequest purchaseRequest =
                purchaseRequestRepository
                        .findByIdForUpdate(purchaseRequestId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Purchase request not found"
                                )
                        );

        validateReview(purchaseRequest, user.getAccountId());

        purchaseRequest.approve(user.getAccountId());

        PurchaseRequest savedRequest =
                purchaseRequestRepository.saveAndFlush(
                        purchaseRequest
                );

        return new PurchaseRequestResponse(savedRequest);
    }

    @Override
    @Transactional
    public PurchaseRequestResponse rejectPurchaseRequest(
            Long purchaseRequestId,
            PurchaseRequestRejectRequest request
    ) {
        ProductUserPrincipal user = currentUser();

        requireAnyRole(
                user,
                "OWNER",
                "MAIN_OFFICE_MANAGER"
        );

        validatePurchaseRequestId(purchaseRequestId);

        if (request == null
                || request.getRejectionReason() == null
                || request.getRejectionReason().isBlank()
                || request.getRejectionReason().trim().length() > 500) {
            throw new BusinessRuleException(
                    "Valid rejection reason is required"
            );
        }

        PurchaseRequest purchaseRequest =
                purchaseRequestRepository
                        .findByIdForUpdate(purchaseRequestId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Purchase request not found"
                                )
                        );

        validateReview(purchaseRequest, user.getAccountId());

        purchaseRequest.reject(
                user.getAccountId(),
                request.getRejectionReason()
        );

        PurchaseRequest savedRequest =
                purchaseRequestRepository.saveAndFlush(
                        purchaseRequest
                );

        return new PurchaseRequestResponse(savedRequest);
    }

    private void validateReview(
            PurchaseRequest purchaseRequest,
            Long managerAccountId
    ) {
        if (purchaseRequest.getStatus()
                != PurchaseRequestStatus.PENDING) {
            throw new BusinessRuleException(
                    "Only pending purchase requests can be reviewed"
            );
        }

        if (purchaseRequest.getRequestedByAccountId()
                .equals(managerAccountId)) {
            throw new BusinessRuleException(
                    "Requester cannot review their own purchase request"
            );
        }

        if (purchaseRequest.getItems().isEmpty()) {
            throw new BusinessRuleException(
                    "Purchase request must contain items"
            );
        }
    }

    private void validatePurchaseRequestId(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessRuleException(
                    "Valid purchase request ID is required"
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
