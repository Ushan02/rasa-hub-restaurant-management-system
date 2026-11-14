
package com.rasahub.product_inventory_service.service.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import com.rasahub.product_inventory_service.dto.branchstockrequest.BranchStockRequestCreateRequest;
import com.rasahub.product_inventory_service.dto.branchstockrequest.BranchStockRequestItemRequest;
import com.rasahub.product_inventory_service.dto.branchstockrequest.BranchStockRequestRejectRequest;
import com.rasahub.product_inventory_service.dto.branchstockrequest.BranchStockRequestResponse;

import com.rasahub.product_inventory_service.entity.BranchStockRequest;
import com.rasahub.product_inventory_service.entity.InventoryItem;
import com.rasahub.product_inventory_service.entity.StockLocation;

import com.rasahub.product_inventory_service.enums.BranchStockRequestStatus;

import com.rasahub.product_inventory_service.exception.BusinessRuleException;
import com.rasahub.product_inventory_service.exception.ResourceNotFoundException;

import com.rasahub.product_inventory_service.repository.BranchStockRequestRepository;
import com.rasahub.product_inventory_service.repository.InventoryItemRepository;
import com.rasahub.product_inventory_service.repository.StockLocationRepository;

import com.rasahub.product_inventory_service.security.ProductUserPrincipal;
import com.rasahub.product_inventory_service.service.BranchStockRequestService;

import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import org.springframework.http.HttpHeaders;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class BranchStockRequestServiceImpl
        implements BranchStockRequestService {

    private static final Set<String> BRANCH_CODES =
            Set.of("BR001", "BR002", "BR003");

    private final BranchStockRequestRepository branchStockRequestRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final StockLocationRepository stockLocationRepository;
    private final EntityManager entityManager;
    private final HttpServletRequest servletRequest;
    private final RestClient authClient;

    public BranchStockRequestServiceImpl(
            BranchStockRequestRepository branchStockRequestRepository,
            InventoryItemRepository inventoryItemRepository,
            StockLocationRepository stockLocationRepository,
            EntityManager entityManager,
            HttpServletRequest servletRequest,
            @Value("${branch.sync.auth-base-url}") String authBaseUrl
    ) {
        this.branchStockRequestRepository =
                branchStockRequestRepository;

        this.inventoryItemRepository =
                inventoryItemRepository;

        this.stockLocationRepository =
                stockLocationRepository;

        this.entityManager = entityManager;
        this.servletRequest = servletRequest;

        this.authClient = RestClient.builder()
                .baseUrl(authBaseUrl)
                .build();
    }

    @Override
    @Transactional
    public BranchStockRequestResponse createBranchStockRequest(
            BranchStockRequestCreateRequest request
    ) {
        ProductUserPrincipal user = currentUser();

        requireAnyRole(user, "BRANCH_MANAGER");

        if (request == null) {
            throw new BusinessRuleException(
                    "Branch stock request is required"
            );
        }

        validateBranchCode(request.getBranchLocationCode());

        requireAssignedBranch(
                user,
                request.getBranchLocationCode()
        );

        if (request.getItems() == null
                || request.getItems().isEmpty()) {
            throw new BusinessRuleException(
                    "Branch stock request must contain at least one item"
            );
        }

        StockLocation branchLocation =
                stockLocationRepository
                        .findById(request.getBranchLocationCode())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Branch stock location not found"
                                )
                        );

        if (!branchLocation.isActive()) {
            throw new BusinessRuleException(
                    "Cannot request stock for an inactive branch"
            );
        }

        BranchStockRequest branchStockRequest;

        try {
            branchStockRequest = new BranchStockRequest(
                    branchLocation,
                    user.getAccountId(),
                    request.getRemarks()
            );
        } catch (IllegalArgumentException exception) {
            throw new BusinessRuleException(
                    exception.getMessage()
            );
        }

        Set<Long> processedItemIds = new HashSet<>();

        for (BranchStockRequestItemRequest itemRequest
                : request.getItems()) {

            if (itemRequest == null
                    || itemRequest.getInventoryItemId() == null
                    || itemRequest.getInventoryItemId() <= 0) {
                throw new BusinessRuleException(
                        "Valid inventory item ID is required"
                );
            }

            Long itemId = itemRequest.getInventoryItemId();

            if (!processedItemIds.add(itemId)) {
                throw new BusinessRuleException(
                        "Duplicate inventory item in request"
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
                branchStockRequest.addItem(
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

        BranchStockRequest savedRequest =
                branchStockRequestRepository.saveAndFlush(
                        branchStockRequest
                );

        return new BranchStockRequestResponse(savedRequest);
    }

    @Override
    public BranchStockRequestResponse getBranchStockRequestById(
            Long branchStockRequestId
    ) {
        ProductUserPrincipal user = currentUser();

        requireAnyRole(
                user,
                "OWNER",
                "MAIN_OFFICE_MANAGER",
                "BRANCH_MANAGER"
        );

        validateId(branchStockRequestId);

        BranchStockRequest request =
                branchStockRequestRepository
                        .findWithItemsById(branchStockRequestId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Branch stock request not found"
                                )
                        );

        if ("BRANCH_MANAGER".equals(user.getRole())) {
            requireAssignedBranch(
                    user,
                    request.getBranchLocation().getLocationCode()
            );
        }

        return new BranchStockRequestResponse(request);
    }

    @Override
    public Page<BranchStockRequestResponse> getAllBranchStockRequests(
            int page,
            int size
    ) {
        requireAnyRole(
                currentUser(),
                "OWNER",
                "MAIN_OFFICE_MANAGER"
        );

        Pageable pageable = createPageable(page, size);

        return branchStockRequestRepository
                .findAllByOrderByCreatedAtDescIdDesc(pageable)
                .map(BranchStockRequestResponse::new);
    }

    @Override
    public Page<BranchStockRequestResponse> getBranchStockRequestsByStatus(
            BranchStockRequestStatus status,
            int page,
            int size
    ) {
        ProductUserPrincipal user = currentUser();

        requireAnyRole(
                user,
                "OWNER",
                "MAIN_OFFICE_MANAGER",
                "BRANCH_MANAGER"
        );

        validateStatus(status);

        Pageable pageable = createPageable(page, size);

        if ("BRANCH_MANAGER".equals(user.getRole())) {

            String branchCode = getAssignedBranchCode(user);

            return branchStockRequestRepository
                    .findByBranchLocation_LocationCodeAndStatusOrderByCreatedAtDescIdDesc(
                            branchCode,
                            status,
                            pageable
                    )
                    .map(BranchStockRequestResponse::new);
        }

        return branchStockRequestRepository
                .findByStatusOrderByCreatedAtDescIdDesc(
                        status,
                        pageable
                )
                .map(BranchStockRequestResponse::new);
    }

    @Override
    public Page<BranchStockRequestResponse> getBranchStockRequestsByBranch(
            String branchLocationCode,
            int page,
            int size
    ) {
        ProductUserPrincipal user = currentUser();

        requireAnyRole(
                user,
                "OWNER",
                "MAIN_OFFICE_MANAGER",
                "BRANCH_MANAGER"
        );

        validateBranchCode(branchLocationCode);

        if ("BRANCH_MANAGER".equals(user.getRole())) {
            requireAssignedBranch(
                    user,
                    branchLocationCode
            );
        }

        Pageable pageable = createPageable(page, size);

        return branchStockRequestRepository
                .findByBranchLocation_LocationCodeOrderByCreatedAtDescIdDesc(
                        branchLocationCode,
                        pageable
                )
                .map(BranchStockRequestResponse::new);
    }

    @Override
    public Page<BranchStockRequestResponse>
    getBranchStockRequestsByBranchAndStatus(
            String branchLocationCode,
            BranchStockRequestStatus status,
            int page,
            int size
    ) {
        ProductUserPrincipal user = currentUser();

        requireAnyRole(
                user,
                "OWNER",
                "MAIN_OFFICE_MANAGER",
                "BRANCH_MANAGER"
        );

        validateBranchCode(branchLocationCode);
        validateStatus(status);

        if ("BRANCH_MANAGER".equals(user.getRole())) {
            requireAssignedBranch(
                    user,
                    branchLocationCode
            );
        }

        Pageable pageable = createPageable(page, size);

        return branchStockRequestRepository
                .findByBranchLocation_LocationCodeAndStatusOrderByCreatedAtDescIdDesc(
                        branchLocationCode,
                        status,
                        pageable
                )
                .map(BranchStockRequestResponse::new);
    }

    @Override
    public Page<BranchStockRequestResponse> getMyBranchStockRequests(
            int page,
            int size
    ) {
        ProductUserPrincipal user = currentUser();

        requireAnyRole(user, "BRANCH_MANAGER");

        String branchCode = getAssignedBranchCode(user);

        Pageable pageable = createPageable(page, size);

        List<BranchStockRequest> requests =
                entityManager.createQuery(
                                """
                                SELECT bsr
                                FROM BranchStockRequest bsr
                                WHERE bsr.requestedByAccountId = :accountId
                                AND bsr.branchLocation.locationCode = :branchCode
                                ORDER BY bsr.createdAt DESC, bsr.id DESC
                                """,
                                BranchStockRequest.class
                        )
                        .setParameter("accountId", user.getAccountId())
                        .setParameter("branchCode", branchCode)
                        .setFirstResult(Math.toIntExact(pageable.getOffset()))
                        .setMaxResults(size)
                        .getResultList();

        Long total = entityManager.createQuery(
                        """
                        SELECT COUNT(bsr)
                        FROM BranchStockRequest bsr
                        WHERE bsr.requestedByAccountId = :accountId
                        AND bsr.branchLocation.locationCode = :branchCode
                        """,
                        Long.class
                )
                .setParameter("accountId", user.getAccountId())
                .setParameter("branchCode", branchCode)
                .getSingleResult();

        List<BranchStockRequestResponse> responses =
                requests.stream()
                        .map(BranchStockRequestResponse::new)
                        .toList();

        return new PageImpl<>(
                responses,
                pageable,
                total
        );
    }

    @Override
    @Transactional
    public BranchStockRequestResponse approveBranchStockRequest(
            Long branchStockRequestId
    ) {
        ProductUserPrincipal user = currentUser();

        requireAnyRole(
                user,
                "OWNER",
                "MAIN_OFFICE_MANAGER"
        );

        validateId(branchStockRequestId);

        BranchStockRequest request =
                branchStockRequestRepository
                        .findByIdForUpdate(branchStockRequestId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Branch stock request not found"
                                )
                        );

        if (!request.getBranchLocation().isActive()) {
            throw new BusinessRuleException(
                    "Cannot approve a request for an inactive branch"
            );
        }

        try {
            request.approve(user.getAccountId());
        } catch (IllegalArgumentException
                 | IllegalStateException exception) {
            throw new BusinessRuleException(
                    exception.getMessage()
            );
        }

        BranchStockRequest savedRequest =
                branchStockRequestRepository.saveAndFlush(request);

        return new BranchStockRequestResponse(savedRequest);
    }

    @Override
    @Transactional
    public BranchStockRequestResponse rejectBranchStockRequest(
            Long branchStockRequestId,
            BranchStockRequestRejectRequest rejectRequest
    ) {
        ProductUserPrincipal user = currentUser();

        requireAnyRole(
                user,
                "OWNER",
                "MAIN_OFFICE_MANAGER"
        );

        validateId(branchStockRequestId);

        if (rejectRequest == null
                || rejectRequest.getRejectionReason() == null
                || rejectRequest.getRejectionReason().isBlank()
                || rejectRequest.getRejectionReason()
                .trim().length() > 500) {
            throw new BusinessRuleException(
                    "Valid rejection reason is required"
            );
        }

        BranchStockRequest request =
                branchStockRequestRepository
                        .findByIdForUpdate(branchStockRequestId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Branch stock request not found"
                                )
                        );

        try {
            request.reject(
                    user.getAccountId(),
                    rejectRequest.getRejectionReason()
            );
        } catch (IllegalArgumentException
                 | IllegalStateException exception) {
            throw new BusinessRuleException(
                    exception.getMessage()
            );
        }

        BranchStockRequest savedRequest =
                branchStockRequestRepository.saveAndFlush(request);

        return new BranchStockRequestResponse(savedRequest);
    }

    private String getAssignedBranchCode(
            ProductUserPrincipal user
    ) {
        if (user.getBranchId() == null
                || user.getBranchId() <= 0) {
            throw new AccessDeniedException(
                    "Valid branch assignment is required"
            );
        }

        String authorizationHeader =
                servletRequest.getHeader(
                        HttpHeaders.AUTHORIZATION
                );

        if (authorizationHeader == null
                || !authorizationHeader.startsWith("Bearer ")) {
            throw new AccessDeniedException(
                    "Valid authentication token is required"
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
                            authorizationHeader
                    )
                    .retrieve()
                    .body(AuthBranchData.class);
        } catch (RestClientException exception) {
            throw new AccessDeniedException(
                    "Unable to verify branch assignment",
                    exception
            );
        }

        if (branch == null
                || branch.id() == null
                || !user.getBranchId().equals(branch.id())
                || !branch.active()
                || !BRANCH_CODES.contains(branch.branchCode())) {
            throw new AccessDeniedException(
                    "Valid active branch assignment is required"
            );
        }

        return branch.branchCode();
    }

    private void requireAssignedBranch(
            ProductUserPrincipal user,
            String requestedBranchCode
    ) {
        String assignedBranchCode =
                getAssignedBranchCode(user);

        if (!assignedBranchCode.equals(requestedBranchCode)) {
            throw new AccessDeniedException(
                    "You can only access your assigned branch"
            );
        }
    }

    private void validateBranchCode(String branchCode) {
        if (branchCode == null
                || !BRANCH_CODES.contains(branchCode)) {
            throw new BusinessRuleException(
                    "Branch must be BR001, BR002 or BR003"
            );
        }
    }

    private void validateStatus(
            BranchStockRequestStatus status
    ) {
        if (status == null) {
            throw new BusinessRuleException(
                    "Branch stock request status is required"
            );
        }
    }

    private void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessRuleException(
                    "Valid branch stock request ID is required"
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

        if ((long) page * size > Integer.MAX_VALUE) {
            throw new BusinessRuleException(
                    "Page offset exceeds supported range"
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AuthBranchData(
            Long id,
            String branchCode,
            boolean active
    ) {
    }
}
