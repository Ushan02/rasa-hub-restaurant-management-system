package com.rasahub.product_inventory_service.service.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.rasahub.product_inventory_service.dto.stockcount.StockCountCreateRequest;
import com.rasahub.product_inventory_service.dto.stockcount.StockCountItemRequest;
import com.rasahub.product_inventory_service.dto.stockcount.StockCountRejectRequest;
import com.rasahub.product_inventory_service.dto.stockcount.StockCountResponse;
import com.rasahub.product_inventory_service.dto.stockcount.StockCountUpdateRequest;
import com.rasahub.product_inventory_service.entity.InventoryItem;
import com.rasahub.product_inventory_service.entity.StockBalance;
import com.rasahub.product_inventory_service.entity.StockCount;
import com.rasahub.product_inventory_service.entity.StockCountItem;
import com.rasahub.product_inventory_service.entity.StockLocation;
import com.rasahub.product_inventory_service.enums.StockCountStatus;
import com.rasahub.product_inventory_service.exception.BusinessRuleException;
import com.rasahub.product_inventory_service.exception.ConflictException;
import com.rasahub.product_inventory_service.exception.ResourceNotFoundException;
import com.rasahub.product_inventory_service.repository.InventoryItemRepository;
import com.rasahub.product_inventory_service.repository.StockBalanceRepository;
import com.rasahub.product_inventory_service.repository.StockCountRepository;
import com.rasahub.product_inventory_service.repository.StockLocationRepository;
import com.rasahub.product_inventory_service.security.ProductUserPrincipal;
import com.rasahub.product_inventory_service.service.StockAdjustmentService;
import com.rasahub.product_inventory_service.service.StockCountService;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class StockCountServiceImpl implements StockCountService {

    private static final Set<String> BRANCH_CODES =
            Set.of("BR001", "BR002", "BR003");
    private static final String MAIN_LOCATION = "Main";

    private final StockCountRepository stockCountRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final StockLocationRepository stockLocationRepository;
    private final StockBalanceRepository stockBalanceRepository;
    private final StockAdjustmentService stockAdjustmentService;
    private final HttpServletRequest servletRequest;
    private final RestClient authClient;

    public StockCountServiceImpl(
            StockCountRepository stockCountRepository,
            InventoryItemRepository inventoryItemRepository,
            StockLocationRepository stockLocationRepository,
            StockBalanceRepository stockBalanceRepository,
            StockAdjustmentService stockAdjustmentService,
            HttpServletRequest servletRequest,
            @Value("${branch.sync.auth-base-url}") String authBaseUrl
    ) {
        this.stockCountRepository = stockCountRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.stockLocationRepository = stockLocationRepository;
        this.stockBalanceRepository = stockBalanceRepository;
        this.stockAdjustmentService = stockAdjustmentService;
        this.servletRequest = servletRequest;

        SimpleClientHttpRequestFactory requestFactory =
                new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        authClient = RestClient.builder()
                .baseUrl(authBaseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    @Transactional
    public StockCountResponse createStockCount(
            StockCountCreateRequest request
    ) {
        ProductUserPrincipal user = currentUser();
        if (request == null) {
            throw new BusinessRuleException("Stock count request is required");
        }
        validateLocationCode(request.getLocationCode());
        requireManageLocation(user, request.getLocationCode());
        validateItemRequests(request.getItems());

        StockLocation location = stockLocationRepository
                .findByIdForUpdate(request.getLocationCode())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Stock location not found: " + request.getLocationCode()
                ));
        if (!location.isActive()) {
            throw new BusinessRuleException(
                    "Cannot count stock at an inactive location"
            );
        }

        LocalDate countDate = request.getCountDate() == null
                ? LocalDate.now()
                : request.getCountDate();
        StockCount stockCount;
        try {
            stockCount = new StockCount(
                    location,
                    countDate,
                    user.getAccountId(),
                    request.getRemarks()
            );
        } catch (IllegalArgumentException exception) {
            throw new BusinessRuleException(exception.getMessage());
        }

        List<StockCountItemRequest> sortedRequests =
                request.getItems().stream()
                        .sorted(Comparator.comparing(
                                StockCountItemRequest::getInventoryItemId
                        ))
                        .toList();
        for (StockCountItemRequest itemRequest : sortedRequests) {
            Long itemId = itemRequest.getInventoryItemId();
            InventoryItem item = inventoryItemRepository.findById(itemId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Inventory item not found: " + itemId
                    ));
            if (!item.isActive()) {
                throw new BusinessRuleException(
                        "Cannot count an inactive inventory item: " + itemId
                );
            }

            Optional<StockBalance> balance =
                    stockBalanceRepository.findForUpdate(
                            location.getLocationCode(),
                            itemId
                    );
            BigDecimal systemQuantity = balance
                    .map(StockBalance::getQuantityOnHand)
                    .orElse(BigDecimal.ZERO);
            Long systemVersion = balance
                    .map(StockBalance::getVersion)
                    .orElse(null);
            try {
                stockCount.addItem(
                        item,
                        systemQuantity,
                        systemVersion,
                        itemRequest.getPhysicalQuantity(),
                        itemRequest.getItemRemarks()
                );
            } catch (IllegalArgumentException | IllegalStateException exception) {
                throw new BusinessRuleException(exception.getMessage());
            }
        }

        return new StockCountResponse(
                stockCountRepository.saveAndFlush(stockCount)
        );
    }

    @Override
    @Transactional
    public StockCountResponse updateDraftStockCount(
            Long stockCountId,
            StockCountUpdateRequest request
    ) {
        ProductUserPrincipal user = currentUser();
        validateId(stockCountId);
        if (request == null) {
            throw new BusinessRuleException(
                    "Stock count update request is required"
            );
        }
        validateItemRequests(request.getItems());

        StockCount stockCount = lockStockCount(stockCountId);
        requireCreatorAndManageLocation(user, stockCount);
        if (stockCount.getStatus() != StockCountStatus.DRAFT) {
            throw new BusinessRuleException(
                    "Only DRAFT stock counts can be updated"
            );
        }
        ensureSameItemSet(stockCount, request.getItems());

        try {
            stockCount.updateDraft(request.getRemarks());
            for (StockCountItemRequest itemRequest : request.getItems()) {
                stockCount.updateItem(
                        itemRequest.getInventoryItemId(),
                        itemRequest.getPhysicalQuantity(),
                        itemRequest.getItemRemarks()
                );
            }
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw new BusinessRuleException(exception.getMessage());
        }
        return new StockCountResponse(
                stockCountRepository.saveAndFlush(stockCount)
        );
    }

    @Override
    @Transactional
    public StockCountResponse submitStockCount(Long stockCountId) {
        ProductUserPrincipal user = currentUser();
        validateId(stockCountId);
        StockCount stockCount = lockStockCount(stockCountId);
        requireCreatorAndManageLocation(user, stockCount);
        try {
            stockCount.submit(user.getAccountId());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw new BusinessRuleException(exception.getMessage());
        }
        return new StockCountResponse(
                stockCountRepository.saveAndFlush(stockCount)
        );
    }

    @Override
    @Transactional
    public StockCountResponse approveStockCount(Long stockCountId) {
        ProductUserPrincipal user = currentUser();
        requireAnyRole(user, "OWNER", "MAIN_OFFICE_MANAGER");
        validateId(stockCountId);
        StockCount stockCount = lockStockCount(stockCountId);
        try {
            stockCount.approve(user.getAccountId());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw new BusinessRuleException(exception.getMessage());
        }
        return new StockCountResponse(
                stockCountRepository.saveAndFlush(stockCount)
        );
    }

    @Override
    @Transactional
    public StockCountResponse rejectStockCount(
            Long stockCountId,
            StockCountRejectRequest request
    ) {
        ProductUserPrincipal user = currentUser();
        requireAnyRole(user, "OWNER", "MAIN_OFFICE_MANAGER");
        validateId(stockCountId);
        if (request == null || request.getRejectionReason() == null
                || request.getRejectionReason().isBlank()
                || request.getRejectionReason().trim().length() > 500) {
            throw new BusinessRuleException(
                    "A valid stock count rejection reason is required"
            );
        }
        StockCount stockCount = lockStockCount(stockCountId);
        try {
            stockCount.reject(
                    user.getAccountId(),
                    request.getRejectionReason()
            );
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw new BusinessRuleException(exception.getMessage());
        }
        return new StockCountResponse(
                stockCountRepository.saveAndFlush(stockCount)
        );
    }

    @Override
    @Transactional
    public StockCountResponse reconcileStockCount(Long stockCountId) {
        ProductUserPrincipal user = currentUser();
        requireAnyRole(user, "STOCK_KEEPER");
        validateId(stockCountId);

        StockCount stockCount = lockStockCount(stockCountId);
        if (stockCount.getStatus() != StockCountStatus.APPROVED) {
            throw new BusinessRuleException(
                    "Only APPROVED stock counts can be reconciled"
            );
        }
        if (stockCount.getItems().isEmpty()
                || stockCount.getItems().stream().anyMatch(item ->
                item.getPhysicalQuantity() == null)) {
            throw new BusinessRuleException(
                    "Every stock count item must have a physical quantity"
            );
        }

        StockLocation lockedLocation = stockLocationRepository
                .findByIdForUpdate(
                        stockCount.getStockLocation().getLocationCode()
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Stock location not found"
                ));
        if (!lockedLocation.isActive()) {
            throw new BusinessRuleException(
                    "Cannot reconcile a stock count for an inactive location"
            );
        }

        List<StockCountItem> items = stockCount.getItems().stream()
                .sorted(Comparator.comparing(
                        item -> item.getInventoryItem().getId()
                ))
                .toList();
        List<Optional<StockBalance>> balances =
                new ArrayList<>(items.size());
        for (StockCountItem item : items) {
            if (!item.getInventoryItem().isActive()) {
                throw new BusinessRuleException(
                        "Cannot reconcile an inactive inventory item: "
                                + item.getInventoryItem().getId()
                );
            }
            balances.add(stockBalanceRepository.findForUpdate(
                    lockedLocation.getLocationCode(),
                    item.getInventoryItem().getId()
            ));
        }
        for (int index = 0; index < items.size(); index++) {
            verifySnapshot(items.get(index), balances.get(index));
        }

        stockAdjustmentService.postStockCountVariances(stockCount);
        try {
            stockCount.reconcile(user.getAccountId());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw new BusinessRuleException(exception.getMessage());
        }
        return new StockCountResponse(
                stockCountRepository.saveAndFlush(stockCount)
        );
    }

    @Override
    public StockCountResponse getStockCountById(Long stockCountId) {
        ProductUserPrincipal user = currentUser();
        requireReadableRole(user);
        validateId(stockCountId);
        StockCount stockCount = stockCountRepository
                .findWithItemsById(stockCountId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Stock count not found"
                ));
        requireReadLocation(
                user,
                stockCount.getStockLocation().getLocationCode()
        );
        return new StockCountResponse(stockCount);
    }

    @Override
    public Page<StockCountResponse> getAllStockCounts(int page, int size) {
        ProductUserPrincipal user = currentUser();
        requireAnyRole(
                user,
                "OWNER",
                "MAIN_OFFICE_MANAGER",
                "ACCOUNTANT",
                "STOCK_KEEPER"
        );
        Pageable pageable = createPageable(page, size);
        if ("STOCK_KEEPER".equals(user.getRole())) {
            return stockCountRepository
                    .findByStockLocation_LocationCodeOrderByCountDateDescCreatedAtDescIdDesc(
                            MAIN_LOCATION,
                            pageable
                    )
                    .map(StockCountResponse::new);
        }
        return stockCountRepository
                .findAllByOrderByCountDateDescCreatedAtDescIdDesc(pageable)
                .map(StockCountResponse::new);
    }

    @Override
    public Page<StockCountResponse> getMyBranchStockCounts(
            int page,
            int size
    ) {
        ProductUserPrincipal user = currentUser();
        requireAnyRole(user, "BRANCH_MANAGER");
        return stockCountRepository
                .findByStockLocation_LocationCodeOrderByCountDateDescCreatedAtDescIdDesc(
                        assignedBranchCode(user),
                        createPageable(page, size)
                )
                .map(StockCountResponse::new);
    }

    @Override
    public Page<StockCountResponse> getStockCountsByLocation(
            String locationCode,
            int page,
            int size
    ) {
        ProductUserPrincipal user = currentUser();
        requireReadableRole(user);
        validateLocationCode(locationCode);
        requireReadLocation(user, locationCode);
        return stockCountRepository
                .findByStockLocation_LocationCodeOrderByCountDateDescCreatedAtDescIdDesc(
                        locationCode,
                        createPageable(page, size)
                )
                .map(StockCountResponse::new);
    }

    @Override
    public Page<StockCountResponse> getStockCountsByStatus(
            StockCountStatus status,
            int page,
            int size
    ) {
        ProductUserPrincipal user = currentUser();
        requireReadableRole(user);
        if (status == null) {
            throw new BusinessRuleException("Stock count status is required");
        }
        Pageable pageable = createPageable(page, size);
        if ("BRANCH_MANAGER".equals(user.getRole())) {
            return stockCountRepository
                    .findByStockLocation_LocationCodeAndStatusOrderByCountDateDescCreatedAtDescIdDesc(
                            assignedBranchCode(user),
                            status,
                            pageable
                    )
                    .map(StockCountResponse::new);
        }
        if ("STOCK_KEEPER".equals(user.getRole())) {
            return stockCountRepository
                    .findByStockLocation_LocationCodeAndStatusOrderByCountDateDescCreatedAtDescIdDesc(
                            MAIN_LOCATION,
                            status,
                            pageable
                    )
                    .map(StockCountResponse::new);
        }
        return stockCountRepository
                .findByStatusOrderByCountDateDescCreatedAtDescIdDesc(
                        status,
                        pageable
                )
                .map(StockCountResponse::new);
    }

    @Override
    public Page<StockCountResponse> getStockCountsByDateRange(
            LocalDate startDate,
            LocalDate endDate,
            String locationCode,
            StockCountStatus status,
            int page,
            int size
    ) {
        ProductUserPrincipal user = currentUser();
        requireReadableRole(user);
        if (startDate == null || endDate == null
                || startDate.isAfter(endDate)) {
            throw new BusinessRuleException(
                    "A valid start date and end date are required"
            );
        }
        if (locationCode != null) {
            validateLocationCode(locationCode);
        }
        String scopedLocation = locationCode;
        if ("BRANCH_MANAGER".equals(user.getRole())) {
            String assigned = assignedBranchCode(user);
            if (scopedLocation != null && !scopedLocation.equals(assigned)) {
                throw new AccessDeniedException(
                        "You can only view counts for your assigned branch"
                );
            }
            scopedLocation = assigned;
        } else if ("STOCK_KEEPER".equals(user.getRole())) {
            if (scopedLocation != null
                    && !MAIN_LOCATION.equals(scopedLocation)) {
                throw new AccessDeniedException(
                        "Stock Keeper can only view Main warehouse counts"
                );
            }
            scopedLocation = MAIN_LOCATION;
        }
        return stockCountRepository.findByFilters(
                        startDate,
                        endDate,
                        scopedLocation,
                        status,
                        createPageable(page, size)
                )
                .map(StockCountResponse::new);
    }

    private void verifySnapshot(
            StockCountItem item,
            Optional<StockBalance> currentBalance
    ) {
        if (item.getSystemBalanceVersion() == null) {
            if (currentBalance.isPresent()) {
                throw new ConflictException(
                        "Stock changed after the count snapshot for item "
                                + item.getInventoryItem().getItemCode()
                                + "; create a new stock count"
                );
            }
            return;
        }
        if (currentBalance.isEmpty()) {
            throw new ConflictException(
                    "Stock balance was removed after the count snapshot for item "
                            + item.getInventoryItem().getItemCode()
                            + "; create a new stock count"
            );
        }
        StockBalance balance = currentBalance.get();
        if (!Objects.equals(
                item.getSystemBalanceVersion(),
                balance.getVersion()
        ) || item.getSystemQuantity().compareTo(
                balance.getQuantityOnHand()) != 0) {
            throw new ConflictException(
                    "Stock changed after the count snapshot for item "
                            + item.getInventoryItem().getItemCode()
                            + "; create a new stock count"
            );
        }
    }

    private void ensureSameItemSet(
            StockCount stockCount,
            List<StockCountItemRequest> requests
    ) {
        Set<Long> requestIds = new HashSet<>();
        for (StockCountItemRequest request : requests) {
            if (request == null || request.getInventoryItemId() == null
                    || request.getInventoryItemId() <= 0
                    || !requestIds.add(request.getInventoryItemId())) {
                throw new BusinessRuleException(
                        "Stock count items must have valid unique inventory IDs"
                );
            }
        }
        Set<Long> existingIds = new HashSet<>();
        for (StockCountItem item : stockCount.getItems()) {
            existingIds.add(item.getInventoryItem().getId());
        }
        if (!existingIds.equals(requestIds)) {
            throw new BusinessRuleException(
                    "Draft updates must include exactly the selected stock count items"
            );
        }
    }

    private void validateItemRequests(List<StockCountItemRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new BusinessRuleException(
                    "Stock count must contain at least one item"
            );
        }
        Set<Long> seenIds = new HashSet<>();
        for (StockCountItemRequest request : requests) {
            if (request == null || request.getInventoryItemId() == null
                    || request.getInventoryItemId() <= 0) {
                throw new BusinessRuleException(
                        "Valid inventory item ID is required"
                );
            }
            if (!seenIds.add(request.getInventoryItemId())) {
                throw new BusinessRuleException(
                        "Duplicate inventory item in stock count"
                );
            }
        }
    }

    private StockCount lockStockCount(Long id) {
        return stockCountRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Stock count not found"
                ));
    }

    private void requireCreatorAndManageLocation(
            ProductUserPrincipal user,
            StockCount stockCount
    ) {
        requireAnyRole(user, "BRANCH_MANAGER", "STOCK_KEEPER");
        if (!user.getAccountId().equals(
                stockCount.getCreatedByAccountId())) {
            throw new AccessDeniedException(
                    "Only the stock count creator can modify or submit it"
            );
        }
        requireManageLocation(
                user,
                stockCount.getStockLocation().getLocationCode()
        );
    }

    private void requireManageLocation(
            ProductUserPrincipal user,
            String locationCode
    ) {
        if ("STOCK_KEEPER".equals(user.getRole())) {
            if (!MAIN_LOCATION.equals(locationCode)) {
                throw new AccessDeniedException(
                        "Stock Keeper can manage only Main warehouse counts"
                );
            }
            return;
        }
        requireAnyRole(user, "BRANCH_MANAGER");
        if (!BRANCH_CODES.contains(locationCode)
                || !assignedBranchCode(user).equals(locationCode)) {
            throw new AccessDeniedException(
                    "Branch Manager can manage only their assigned branch"
            );
        }
    }

    private void requireReadLocation(
            ProductUserPrincipal user,
            String locationCode
    ) {
        if ("BRANCH_MANAGER".equals(user.getRole())) {
            if (!assignedBranchCode(user).equals(locationCode)) {
                throw new AccessDeniedException(
                        "You can only access counts for your assigned branch"
                );
            }
        } else if ("STOCK_KEEPER".equals(user.getRole())
                && !MAIN_LOCATION.equals(locationCode)) {
            throw new AccessDeniedException(
                    "Stock Keeper can only access Main warehouse counts"
            );
        }
    }

    private void requireReadableRole(ProductUserPrincipal user) {
        requireAnyRole(
                user,
                "OWNER",
                "MAIN_OFFICE_MANAGER",
                "ACCOUNTANT",
                "STOCK_KEEPER",
                "BRANCH_MANAGER"
        );
    }

    private String assignedBranchCode(ProductUserPrincipal user) {
        if (user.getBranchId() == null || user.getBranchId() <= 0) {
            throw new AccessDeniedException(
                    "Valid branch assignment is required"
            );
        }
        String bearer = servletRequest.getHeader(HttpHeaders.AUTHORIZATION);
        if (bearer == null || !bearer.startsWith("Bearer ")) {
            throw new AccessDeniedException(
                    "Valid Bearer token is required"
            );
        }
        try {
            AuthBranchData branch = authClient.get()
                    .uri("/api/v1/branches/{id}", user.getBranchId())
                    .header(HttpHeaders.AUTHORIZATION, bearer)
                    .retrieve()
                    .body(AuthBranchData.class);
            if (branch == null
                    || branch.id() == null
                    || !user.getBranchId().equals(branch.id())
                    || !branch.active()
                    || !BRANCH_CODES.contains(branch.branchCode())) {
                throw new AccessDeniedException(
                        "A valid active branch assignment is required"
                );
            }
            return branch.branchCode();
        } catch (RestClientException exception) {
            throw new AccessDeniedException(
                    "Unable to verify branch assignment",
                    exception
            );
        }
    }

    private void validateLocationCode(String locationCode) {
        if (!MAIN_LOCATION.equals(locationCode)
                && !BRANCH_CODES.contains(locationCode)) {
            throw new BusinessRuleException(
                    "Location must be Main, BR001, BR002 or BR003"
            );
        }
    }

    private void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessRuleException(
                    "Valid stock count ID is required"
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
        Authentication authentication = SecurityContextHolder.getContext()
                .getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal()
                instanceof ProductUserPrincipal user)
                || user.getAccountId() == null
                || user.getAccountId() <= 0) {
            throw new AccessDeniedException(
                    "Authenticated staff account is required"
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
