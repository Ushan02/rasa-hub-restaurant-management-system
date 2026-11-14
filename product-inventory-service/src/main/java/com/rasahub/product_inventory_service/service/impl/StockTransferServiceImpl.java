
package com.rasahub.product_inventory_service.service.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.rasahub.product_inventory_service.dto.stocktransfer.*;
import com.rasahub.product_inventory_service.entity.*;
import com.rasahub.product_inventory_service.enums.*;
import com.rasahub.product_inventory_service.exception.BusinessRuleException;
import com.rasahub.product_inventory_service.exception.ResourceNotFoundException;
import com.rasahub.product_inventory_service.repository.*;
import com.rasahub.product_inventory_service.security.ProductUserPrincipal;
import com.rasahub.product_inventory_service.service.StockTransferService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class StockTransferServiceImpl implements StockTransferService {

    private static final Set<String> BRANCH_CODES =
            Set.of("BR001", "BR002", "BR003");

    private static final String MAIN = "Main";
    private static final String MOVEMENT_REFERENCE = "STOCK_TRANSFER";

    private final StockTransferRepository transferRepository;
    private final BranchStockRequestRepository requestRepository;
    private final StockLocationRepository locationRepository;
    private final InventoryItemRepository itemRepository;
    private final StockBalanceRepository balanceRepository;
    private final StockMovementRepository movementRepository;
    private final EntityManager entityManager;
    private final HttpServletRequest servletRequest;
    private final RestClient authClient;

    public StockTransferServiceImpl(
            StockTransferRepository transferRepository,
            BranchStockRequestRepository requestRepository,
            StockLocationRepository locationRepository,
            InventoryItemRepository itemRepository,
            StockBalanceRepository balanceRepository,
            StockMovementRepository movementRepository,
            EntityManager entityManager,
            HttpServletRequest servletRequest,
            @Value("${branch.sync.auth-base-url}") String authBaseUrl
    ) {
        this.transferRepository = transferRepository;
        this.requestRepository = requestRepository;
        this.locationRepository = locationRepository;
        this.itemRepository = itemRepository;
        this.balanceRepository = balanceRepository;
        this.movementRepository = movementRepository;
        this.entityManager = entityManager;
        this.servletRequest = servletRequest;

        SimpleClientHttpRequestFactory factory =
                new SimpleClientHttpRequestFactory();

        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));

        authClient = RestClient.builder()
                .baseUrl(authBaseUrl)
                .requestFactory(factory)
                .build();
    }

    @Override
    @Transactional
    public StockTransferResponse createTransfer(
            StockTransferCreateRequest input
    ) {
        ProductUserPrincipal user = currentUser();
        requireRole(user, "STOCK_KEEPER");

        if (input == null
                || input.getBranchStockRequestId() == null
                || input.getBranchStockRequestId() <= 0
                || input.getItems() == null
                || input.getItems().isEmpty()) {
            throw new BusinessRuleException(
                    "Valid request ID and transfer items are required"
            );
        }

        BranchStockRequest request =
                lockedRequest(input.getBranchStockRequestId());

        ensureRequestTransferable(request);

        StockLocation main = locationRepository.findById(MAIN)
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

        StockTransfer transfer;

        try {
            transfer = new StockTransfer(
                    request,
                    main,
                    user.getAccountId(),
                    input.getRemarks()
            );
        } catch (IllegalArgumentException | IllegalStateException ex) {
            throw new BusinessRuleException(ex.getMessage());
        }

        Set<Long> uniqueItems = new HashSet<>();

        for (StockTransferItemRequest line : input.getItems()) {

            if (line == null
                    || line.getInventoryItemId() == null
                    || line.getInventoryItemId() <= 0) {
                throw new BusinessRuleException(
                        "Valid inventory item ID is required"
                );
            }

            if (!uniqueItems.add(line.getInventoryItemId())) {
                throw new BusinessRuleException(
                        "Duplicate inventory item in transfer"
                );
            }

            InventoryItem inventory =
                    itemRepository.findById(line.getInventoryItemId())
                            .orElseThrow(() ->
                                    new ResourceNotFoundException(
                                            "Inventory item not found: "
                                                    + line.getInventoryItemId()
                                    )
                            );

            try {
                transfer.addItem(
                        inventory,
                        line.getTransferQuantity()
                );
            } catch (IllegalArgumentException
                     | IllegalStateException ex) {
                throw new BusinessRuleException(ex.getMessage());
            }
        }

        return new StockTransferResponse(
                transferRepository.saveAndFlush(transfer)
        );
    }

    @Override
    public StockTransferResponse getTransferById(Long transferId) {
        ProductUserPrincipal user = currentUser();

        requireRole(
                user,
                "OWNER",
                "MAIN_OFFICE_MANAGER",
                "STOCK_KEEPER",
                "ACCOUNTANT",
                "BRANCH_MANAGER"
        );

        validateId(transferId);

        StockTransfer transfer =
                transferRepository.findWithItemsById(transferId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Stock transfer not found"
                                )
                        );

        enforceReadBranchScope(
                user,
                transfer.getDestinationLocation().getLocationCode()
        );

        return new StockTransferResponse(transfer);
    }

    @Override
    public Page<StockTransferResponse> getAllTransfers(
            int page,
            int size
    ) {
        requireRole(
                currentUser(),
                "OWNER",
                "MAIN_OFFICE_MANAGER",
                "STOCK_KEEPER",
                "ACCOUNTANT"
        );

        return transferRepository
                .findAllByOrderByCreatedAtDescIdDesc(pageable(page, size))
                .map(StockTransferResponse::new);
    }

    @Override
    public Page<StockTransferResponse> getTransfersByStatus(
            StockTransferStatus status,
            int page,
            int size
    ) {
        ProductUserPrincipal user = currentUser();

        requireRole(
                user,
                "OWNER",
                "MAIN_OFFICE_MANAGER",
                "STOCK_KEEPER",
                "ACCOUNTANT",
                "BRANCH_MANAGER"
        );

        validateStatus(status);

        Pageable paging = pageable(page, size);

        if ("BRANCH_MANAGER".equals(user.getRole())) {
            return transferRepository
                    .findByDestinationLocation_LocationCodeAndStatusOrderByCreatedAtDescIdDesc(
                            assignedBranchCode(user),
                            status,
                            paging
                    )
                    .map(StockTransferResponse::new);
        }

        return transferRepository
                .findByStatusOrderByCreatedAtDescIdDesc(
                        status,
                        paging
                )
                .map(StockTransferResponse::new);
    }

    @Override
    public Page<StockTransferResponse> getTransfersByBranch(
            String branchCode,
            int page,
            int size
    ) {
        ProductUserPrincipal user = currentUser();

        requireRole(
                user,
                "OWNER",
                "MAIN_OFFICE_MANAGER",
                "STOCK_KEEPER",
                "ACCOUNTANT",
                "BRANCH_MANAGER"
        );

        validateBranchCode(branchCode);
        enforceReadBranchScope(user, branchCode);

        return transferRepository
                .findByDestinationLocation_LocationCodeOrderByCreatedAtDescIdDesc(
                        branchCode,
                        pageable(page, size)
                )
                .map(StockTransferResponse::new);
    }

    @Override
    public Page<StockTransferResponse> getTransfersByBranchAndStatus(
            String branchCode,
            StockTransferStatus status,
            int page,
            int size
    ) {
        ProductUserPrincipal user = currentUser();

        requireRole(
                user,
                "OWNER",
                "MAIN_OFFICE_MANAGER",
                "STOCK_KEEPER",
                "ACCOUNTANT",
                "BRANCH_MANAGER"
        );

        validateBranchCode(branchCode);
        validateStatus(status);
        enforceReadBranchScope(user, branchCode);

        return transferRepository
                .findByDestinationLocation_LocationCodeAndStatusOrderByCreatedAtDescIdDesc(
                        branchCode,
                        status,
                        pageable(page, size)
                )
                .map(StockTransferResponse::new);
    }

    @Override
    public Page<StockTransferResponse> getMyBranchTransfers(
            int page,
            int size
    ) {
        ProductUserPrincipal user = currentUser();

        requireRole(user, "BRANCH_MANAGER");

        return transferRepository
                .findByDestinationLocation_LocationCodeOrderByCreatedAtDescIdDesc(
                        assignedBranchCode(user),
                        pageable(page, size)
                )
                .map(StockTransferResponse::new);
    }

    @Override
    @Transactional
    public StockTransferResponse dispatchTransfer(Long transferId) {
        ProductUserPrincipal user = currentUser();

        requireRole(user, "STOCK_KEEPER");
        validateId(transferId);

        StockTransfer transfer = lockedTransferAndRequest(transferId);

        if (transfer.getStatus() != StockTransferStatus.DRAFT) {
            throw new BusinessRuleException(
                    "Only DRAFT transfers can be dispatched"
            );
        }

        BranchStockRequest request = transfer.getBranchStockRequest();

        ensureRequestTransferable(request);

        if (!transfer.getSourceLocation().isActive()
                || !transfer.getDestinationLocation().isActive()) {
            throw new BusinessRuleException(
                    "Transfer source and destination must be active"
            );
        }

        List<StockTransferItem> lines = sortedItems(transfer);

        for (StockTransferItem line : lines) {
            Long itemId = line.getInventoryItem().getId();

            BranchStockRequestItem requestedLine =
                    request.getItems()
                            .stream()
                            .filter(r ->
                                    r.getInventoryItem()
                                            .getId()
                                            .equals(itemId)
                            )
                            .findFirst()
                            .orElseThrow(() ->
                                    new BusinessRuleException(
                                            "Transfer item is missing from branch stock request"
                                    )
                            );

            BigDecimal dispatchedNotReceived =
                    transferRepository.sumQuantityForRequestItemAndStatus(
                            request.getId(),
                            itemId,
                            StockTransferStatus.DISPATCHED
                    );

            if (dispatchedNotReceived == null) {
                dispatchedNotReceived = BigDecimal.ZERO;
            }

            BigDecimal stillAvailable =
                    requestedLine.getOutstandingQuantity()
                            .subtract(dispatchedNotReceived);

            if (line.getTransferQuantity().compareTo(stillAvailable) > 0) {
                throw new BusinessRuleException(
                        "Transfer would exceed outstanding quantity after accounting for in-transit stock: "
                                + itemId
                );
            }

            if (!line.getInventoryItem().isActive()) {
                throw new BusinessRuleException(
                        "Cannot dispatch an inactive inventory item: " + itemId
                );
            }
        }

        for (StockTransferItem line : lines) {
            InventoryItem inventory = line.getInventoryItem();

            StockBalance balance =
                    balanceRepository.findForUpdate(
                                    MAIN,
                                    inventory.getId()
                            )
                            .orElseThrow(() ->
                                    new BusinessRuleException(
                                            "Main stock balance is missing for item: "
                                                    + inventory.getId()
                                    )
                            );

            BigDecimal before = balance.getQuantityOnHand();

            if (before.compareTo(line.getTransferQuantity()) < 0) {
                throw new BusinessRuleException(
                        "Insufficient Main stock for item: "
                                + inventory.getId()
                );
            }

            balance.decreaseQuantity(line.getTransferQuantity());

            movementRepository.save(
                    new StockMovement(
                            transfer.getSourceLocation(),
                            inventory,
                            StockMovementType.TRANSFER_OUT,
                            line.getTransferQuantity(),
                            before,
                            balance.getQuantityOnHand(),
                            MOVEMENT_REFERENCE,
                            transfer.getId().toString(),
                            user.getAccountId(),
                            "Dispatched from Main to "
                                    + transfer.getDestinationLocation()
                                    .getLocationCode()
                    )
            );
        }

        try {
            transfer.dispatch(user.getAccountId());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            throw new BusinessRuleException(ex.getMessage());
        }

        return new StockTransferResponse(
                transferRepository.saveAndFlush(transfer)
        );
    }

    @Override
    @Transactional
    public StockTransferResponse confirmReceipt(Long transferId) {
        ProductUserPrincipal user = currentUser();

        requireRole(user, "BRANCH_MANAGER");
        validateId(transferId);

        StockTransfer preview = transferRepository
                .findById(transferId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Stock transfer not found"
                        )
                );

        String branchCode =
                preview.getDestinationLocation().getLocationCode();

        requireAssignedBranch(user, branchCode);

        StockTransfer transfer =
                lockedTransferAndRequest(transferId);

        if (transfer.getStatus() != StockTransferStatus.DISPATCHED) {
            throw new BusinessRuleException(
                    "Only DISPATCHED transfers can be received"
            );
        }

        if (!branchCode.equals(
                transfer.getDestinationLocation().getLocationCode()
        )) {
            throw new BusinessRuleException(
                    "Transfer destination changed unexpectedly"
            );
        }

        StockLocation destination = entityManager.find(
                StockLocation.class,
                branchCode,
                LockModeType.PESSIMISTIC_WRITE
        );

        if (destination == null || !destination.isActive()) {
            throw new BusinessRuleException(
                    "Destination branch is unavailable or inactive"
            );
        }

        BranchStockRequest request = transfer.getBranchStockRequest();
        List<StockTransferItem> lines = sortedItems(transfer);

        for (StockTransferItem line : lines) {
            InventoryItem inventory = line.getInventoryItem();

            StockBalance balance =
                    balanceRepository.findForUpdate(
                                    branchCode,
                                    inventory.getId()
                            )
                            .orElseGet(() ->
                                    new StockBalance(destination, inventory)
                            );

            BigDecimal before = balance.getQuantityOnHand();

            balance.increaseQuantity(line.getTransferQuantity());

            balanceRepository.saveAndFlush(balance);

            movementRepository.save(
                    new StockMovement(
                            destination,
                            inventory,
                            StockMovementType.TRANSFER_IN,
                            line.getTransferQuantity(),
                            before,
                            balance.getQuantityOnHand(),
                            MOVEMENT_REFERENCE,
                            transfer.getId().toString(),
                            user.getAccountId(),
                            "Received from Main; transfer "
                                    + transfer.getId()
                    )
            );

            try {
                request.recordReceivedQuantity(
                        inventory.getId(),
                        line.getTransferQuantity()
                );
            } catch (IllegalArgumentException
                     | IllegalStateException ex) {
                throw new BusinessRuleException(ex.getMessage());
            }
        }

        try {
            transfer.confirmReceipt(user.getAccountId());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            throw new BusinessRuleException(ex.getMessage());
        }

        return new StockTransferResponse(
                transferRepository.saveAndFlush(transfer)
        );
    }

    @Override
    @Transactional
    public StockTransferResponse cancelTransfer(
            Long transferId,
            StockTransferCancelRequest input
    ) {
        ProductUserPrincipal user = currentUser();

        requireRole(
                user,
                "STOCK_KEEPER",
                "MAIN_OFFICE_MANAGER",
                "OWNER"
        );

        validateId(transferId);

        if (input == null
                || input.getCancellationReason() == null
                || input.getCancellationReason().isBlank()
                || input.getCancellationReason().trim().length() > 300) {
            throw new BusinessRuleException(
                    "Valid cancellation reason is required"
            );
        }

        StockTransfer transfer =
                lockedTransferAndRequest(transferId);

        if ("STOCK_KEEPER".equals(user.getRole())
                && !user.getAccountId()
                .equals(transfer.getCreatedByAccountId())) {
            throw new AccessDeniedException(
                    "Stock Keeper can cancel only their own draft transfer"
            );
        }

        try {
            transfer.cancel(
                    user.getAccountId(),
                    input.getCancellationReason()
            );
        } catch (IllegalArgumentException
                 | IllegalStateException ex) {
            throw new BusinessRuleException(ex.getMessage());
        }

        return new StockTransferResponse(
                transferRepository.saveAndFlush(transfer)
        );
    }

    private StockTransfer lockedTransferAndRequest(Long transferId) {
        StockTransfer preview = transferRepository.findById(transferId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Stock transfer not found"
                        )
                );

        Long requestId = preview.getBranchStockRequest().getId();

        lockedRequest(requestId);

        StockTransfer locked =
                transferRepository.findByIdForUpdate(transferId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Stock transfer not found"
                                )
                        );

        entityManager.refresh(
                locked,
                LockModeType.PESSIMISTIC_WRITE
        );

        return locked;
    }

    private BranchStockRequest lockedRequest(Long requestId) {
        BranchStockRequest locked =
                requestRepository.findByIdForUpdate(requestId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Branch stock request not found"
                                )
                        );

        entityManager.refresh(
                locked,
                LockModeType.PESSIMISTIC_WRITE
        );

        return locked;
    }

    private void ensureRequestTransferable(
            BranchStockRequest request
    ) {
        if (request.getStatus() != BranchStockRequestStatus.APPROVED
                && request.getStatus()
                != BranchStockRequestStatus.PARTIALLY_FULFILLED) {
            throw new BusinessRuleException(
                    "Request must be APPROVED or PARTIALLY_FULFILLED"
            );
        }

        if (!request.getBranchLocation().isActive()) {
            throw new BusinessRuleException(
                    "Destination branch is inactive"
            );
        }
    }

    private List<StockTransferItem> sortedItems(
            StockTransfer transfer
    ) {
        List<StockTransferItem> lines =
                transfer.getItems()
                        .stream()
                        .sorted(
                                Comparator.comparing(
                                        line ->
                                                line.getInventoryItem()
                                                        .getId()
                                )
                        )
                        .toList();

        if (lines.isEmpty()) {
            throw new BusinessRuleException(
                    "Transfer must have at least one item"
            );
        }

        return lines;
    }

    private String assignedBranchCode(
            ProductUserPrincipal user
    ) {
        if (user.getBranchId() == null || user.getBranchId() <= 0) {
            throw new AccessDeniedException(
                    "Valid branch assignment is required"
            );
        }

        String bearer =
                servletRequest.getHeader(HttpHeaders.AUTHORIZATION);

        if (bearer == null || !bearer.startsWith("Bearer ")) {
            throw new AccessDeniedException(
                    "Valid Bearer token is required"
            );
        }

        AuthBranchData branch;

        try {
            branch = authClient
                    .get()
                    .uri(
                            "/api/v1/branches/{id}",
                            user.getBranchId()
                    )
                    .header(
                            HttpHeaders.AUTHORIZATION,
                            bearer
                    )
                    .retrieve()
                    .body(AuthBranchData.class);
        } catch (RestClientException ex) {
            throw new AccessDeniedException(
                    "Unable to verify branch assignment",
                    ex
            );
        }

        if (branch == null
                || branch.id() == null
                || !branch.id().equals(user.getBranchId())
                || !branch.active()
                || !BRANCH_CODES.contains(branch.branchCode())) {
            throw new AccessDeniedException(
                    "No valid active branch assignment"
            );
        }

        return branch.branchCode();
    }

    private void requireAssignedBranch(
            ProductUserPrincipal user,
            String branchCode
    ) {
        if (!assignedBranchCode(user).equals(branchCode)) {
            throw new AccessDeniedException(
                    "Only your assigned branch can be accessed"
            );
        }
    }

    private void enforceReadBranchScope(
            ProductUserPrincipal user,
            String branchCode
    ) {
        if ("BRANCH_MANAGER".equals(user.getRole())) {
            requireAssignedBranch(user, branchCode);
        }
    }

    private ProductUserPrincipal currentUser() {
        Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();

        if (auth == null
                || !auth.isAuthenticated()
                || !(auth.getPrincipal()
                instanceof ProductUserPrincipal user)
                || user.getAccountId() == null
                || user.getAccountId() <= 0) {
            throw new AccessDeniedException(
                    "Authenticated staff account is required"
            );
        }

        return user;
    }

    private void requireRole(
            ProductUserPrincipal user,
            String... allowed
    ) {
        for (String role : allowed) {
            if (role.equals(user.getRole())) {
                return;
            }
        }

        throw new AccessDeniedException(
                "Not authorized for this stock transfer operation"
        );
    }

    private void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessRuleException(
                    "Valid stock transfer ID is required"
            );
        }
    }

    private void validateBranchCode(String code) {
        if (code == null || !BRANCH_CODES.contains(code)) {
            throw new BusinessRuleException(
                    "Branch must be BR001, BR002, or BR003"
            );
        }
    }

    private void validateStatus(StockTransferStatus status) {
        if (status == null) {
            throw new BusinessRuleException(
                    "Stock transfer status is required"
            );
        }
    }

    private Pageable pageable(int page, int size) {
        if (page < 0
                || size < 1
                || size > 100
                || (long) page * size > Integer.MAX_VALUE) {
            throw new BusinessRuleException(
                    "Invalid pagination: page >= 0 and size 1-100 required"
            );
        }

        return PageRequest.of(page, size);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AuthBranchData(
            Long id,
            String branchCode,
            boolean active
    ) {
    }
}
