package com.rasahub.product_inventory_service.service.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.rasahub.product_inventory_service.dto.stockadjustment.StockAdjustmentCreateRequest;
import com.rasahub.product_inventory_service.dto.stockadjustment.StockAdjustmentItemRequest;
import com.rasahub.product_inventory_service.dto.stockadjustment.StockAdjustmentRejectRequest;
import com.rasahub.product_inventory_service.dto.stockadjustment.StockAdjustmentResponse;
import com.rasahub.product_inventory_service.dto.stockadjustment.StockAdjustmentUpdateRequest;
import com.rasahub.product_inventory_service.entity.InventoryItem;
import com.rasahub.product_inventory_service.entity.StockAdjustment;
import com.rasahub.product_inventory_service.entity.StockAdjustmentItem;
import com.rasahub.product_inventory_service.entity.StockBalance;
import com.rasahub.product_inventory_service.entity.StockCount;
import com.rasahub.product_inventory_service.entity.StockCountItem;
import com.rasahub.product_inventory_service.entity.StockLocation;
import com.rasahub.product_inventory_service.entity.StockMovement;
import com.rasahub.product_inventory_service.enums.StockAdjustmentDirection;
import com.rasahub.product_inventory_service.enums.StockAdjustmentReason;
import com.rasahub.product_inventory_service.enums.StockAdjustmentStatus;
import com.rasahub.product_inventory_service.enums.StockCountStatus;
import com.rasahub.product_inventory_service.enums.StockMovementType;
import com.rasahub.product_inventory_service.exception.BusinessRuleException;
import com.rasahub.product_inventory_service.exception.ResourceNotFoundException;
import com.rasahub.product_inventory_service.repository.InventoryItemRepository;
import com.rasahub.product_inventory_service.repository.StockAdjustmentRepository;
import com.rasahub.product_inventory_service.repository.StockBalanceRepository;
import com.rasahub.product_inventory_service.repository.StockLocationRepository;
import com.rasahub.product_inventory_service.repository.StockMovementRepository;
import com.rasahub.product_inventory_service.security.ProductUserPrincipal;
import com.rasahub.product_inventory_service.service.AuditLogService;
import com.rasahub.product_inventory_service.service.LowStockAlertEvaluationService;
import com.rasahub.product_inventory_service.service.StockAdjustmentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class StockAdjustmentServiceImpl implements StockAdjustmentService {

    private static final Set<String> BRANCH_CODES =
            Set.of("BR001", "BR002", "BR003");
    private static final String MAIN_LOCATION = "Main";
    private static final String MOVEMENT_REFERENCE = "STOCK_ADJUSTMENT";

    private final StockAdjustmentRepository adjustmentRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final StockLocationRepository stockLocationRepository;
    private final StockBalanceRepository stockBalanceRepository;
    private final StockMovementRepository stockMovementRepository;
    private final HttpServletRequest servletRequest;
    private final RestClient authClient;
    private final LowStockAlertEvaluationService lowStockAlertEvaluationService;

    @Autowired
    private AuditLogService auditLogService;

    public StockAdjustmentServiceImpl(
            StockAdjustmentRepository adjustmentRepository,
            InventoryItemRepository inventoryItemRepository,
            StockLocationRepository stockLocationRepository,
            StockBalanceRepository stockBalanceRepository,
            StockMovementRepository stockMovementRepository,
            HttpServletRequest servletRequest,
            @Value("${branch.sync.auth-base-url}") String authBaseUrl
    ) {
        this(
                adjustmentRepository,
                inventoryItemRepository,
                stockLocationRepository,
                stockBalanceRepository,
                stockMovementRepository,
                servletRequest,
                authBaseUrl,
                null
        );
    }

    @Autowired
    public StockAdjustmentServiceImpl(
            StockAdjustmentRepository adjustmentRepository,
            InventoryItemRepository inventoryItemRepository,
            StockLocationRepository stockLocationRepository,
            StockBalanceRepository stockBalanceRepository,
            StockMovementRepository stockMovementRepository,
            HttpServletRequest servletRequest,
            @Value("${branch.sync.auth-base-url}") String authBaseUrl,
            LowStockAlertEvaluationService lowStockAlertEvaluationService
    ) {
        this.adjustmentRepository = adjustmentRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.stockLocationRepository = stockLocationRepository;
        this.stockBalanceRepository = stockBalanceRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.servletRequest = servletRequest;
        this.lowStockAlertEvaluationService = lowStockAlertEvaluationService;

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
    public StockAdjustmentResponse createStockAdjustment(
            StockAdjustmentCreateRequest request
    ) {
        ProductUserPrincipal user = currentUser();
        if (request == null) {
            throw new BusinessRuleException(
                    "Stock adjustment request is required"
            );
        }

        validateLocationCode(request.getLocationCode());
        requireManageLocation(user, request.getLocationCode());

        StockLocation location = getActiveLocation(request.getLocationCode());
        StockAdjustment adjustment;
        try {
            adjustment = new StockAdjustment(
                    location,
                    request.getAdjustmentReason(),
                    request.getDirection(),
                    user.getAccountId(),
                    request.getRemarks()
            );
        } catch (IllegalArgumentException exception) {
            throw new BusinessRuleException(exception.getMessage());
        }

        addItems(adjustment, request.getItems());
        return new StockAdjustmentResponse(
                adjustmentRepository.saveAndFlush(adjustment)
        );
    }

    @Override
    @Transactional
    public StockAdjustmentResponse updateDraftStockAdjustment(
            Long adjustmentId,
            StockAdjustmentUpdateRequest request
    ) {
        ProductUserPrincipal user = currentUser();
        validateId(adjustmentId);
        if (request == null || request.getItems() == null
                || request.getItems().isEmpty()) {
            throw new BusinessRuleException(
                    "Stock adjustment must contain at least one item"
            );
        }
        validateLocationCode(request.getLocationCode());

        StockAdjustment adjustment = adjustmentRepository
                .findWithItemsById(adjustmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Stock adjustment not found"
                ));
        requireCreatorAndManageLocation(user, adjustment);
        requireManageLocation(user, request.getLocationCode());
        if (adjustment.getStatus() != StockAdjustmentStatus.DRAFT) {
            throw new BusinessRuleException(
                    "Only DRAFT adjustments can be updated"
            );
        }

        StockLocation location = getActiveLocation(request.getLocationCode());
        List<StockAdjustmentItem> newItems =
                buildItems(adjustment, request.getItems());
        try {
            adjustment.updateDraft(
                    location,
                    request.getAdjustmentReason(),
                    request.getDirection(),
                    request.getRemarks()
            );
            adjustment.replaceItems(newItems);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw new BusinessRuleException(exception.getMessage());
        }

        return new StockAdjustmentResponse(
                adjustmentRepository.saveAndFlush(adjustment)
        );
    }

    @Override
    @Transactional
    public StockAdjustmentResponse submitStockAdjustment(Long adjustmentId) {
        ProductUserPrincipal user = currentUser();
        requireAnyRole(user, "BRANCH_MANAGER", "STOCK_KEEPER");
        validateId(adjustmentId);

        StockAdjustment adjustment = lockAdjustment(adjustmentId);
        requireCreatorAndManageLocation(user, adjustment);
        StockAdjustmentStatus previousStatus = adjustment.getStatus();
        try {
            adjustment.submit(user.getAccountId());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw new BusinessRuleException(exception.getMessage());
        }

        StockAdjustment saved = adjustmentRepository.saveAndFlush(adjustment);
        auditLogService.record(
                "STOCK_ADJUSTMENT",
                saved.getId(),
                "SUBMITTED",
                user.getAccountId(),
                previousStatus.name(),
                saved.getStatus().name(),
                null,
                null,
                saved.getStockLocation().getLocationCode()
        );
        return new StockAdjustmentResponse(saved);
    }

    @Override
    @Transactional
    public StockAdjustmentResponse approveStockAdjustment(Long adjustmentId) {
        ProductUserPrincipal user = currentUser();
        requireAnyRole(user, "OWNER", "MAIN_OFFICE_MANAGER");
        validateId(adjustmentId);

        StockAdjustment adjustment = lockAdjustment(adjustmentId);
        if (!adjustment.getStockLocation().isActive()) {
            throw new BusinessRuleException(
                    "Cannot approve an adjustment for an inactive location"
            );
        }
        StockAdjustmentStatus previousStatus = adjustment.getStatus();
        try {
            adjustment.approve(user.getAccountId());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw new BusinessRuleException(exception.getMessage());
        }

        StockAdjustment saved = adjustmentRepository.saveAndFlush(adjustment);
        auditLogService.record(
                "STOCK_ADJUSTMENT",
                saved.getId(),
                "APPROVED",
                user.getAccountId(),
                previousStatus.name(),
                saved.getStatus().name(),
                null,
                null,
                saved.getStockLocation().getLocationCode()
        );
        return new StockAdjustmentResponse(saved);
    }

    @Override
    @Transactional
    public StockAdjustmentResponse rejectStockAdjustment(
            Long adjustmentId,
            StockAdjustmentRejectRequest request
    ) {
        ProductUserPrincipal user = currentUser();
        requireAnyRole(user, "OWNER", "MAIN_OFFICE_MANAGER");
        validateId(adjustmentId);
        if (request == null || request.getRejectionReason() == null
                || request.getRejectionReason().isBlank()
                || request.getRejectionReason().trim().length() > 500) {
            throw new BusinessRuleException(
                    "Valid rejection reason is required"
            );
        }

        StockAdjustment adjustment = lockAdjustment(adjustmentId);
        StockAdjustmentStatus previousStatus = adjustment.getStatus();
        try {
            adjustment.reject(
                    user.getAccountId(),
                    request.getRejectionReason()
            );
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw new BusinessRuleException(exception.getMessage());
        }

        StockAdjustment saved = adjustmentRepository.saveAndFlush(adjustment);
        auditLogService.record(
                "STOCK_ADJUSTMENT",
                saved.getId(),
                "REJECTED",
                user.getAccountId(),
                previousStatus.name(),
                saved.getStatus().name(),
                request.getRejectionReason(),
                null,
                saved.getStockLocation().getLocationCode()
        );
        return new StockAdjustmentResponse(saved);
    }

    @Override
    @Transactional
    public StockAdjustmentResponse postStockAdjustment(Long adjustmentId) {
        ProductUserPrincipal user = currentUser();
        requireAnyRole(user, "STOCK_KEEPER");
        validateId(adjustmentId);

        return new StockAdjustmentResponse(
                postAdjustmentUnderLock(adjustmentId, user)
        );
    }

    @Override
    @Transactional
    public List<StockAdjustmentResponse> postStockCountVariances(
            StockCount stockCount
    ) {
        ProductUserPrincipal user = currentUser();
        requireAnyRole(user, "STOCK_KEEPER");
        if (stockCount == null
                || stockCount.getId() == null
                || stockCount.getStatus() != StockCountStatus.APPROVED) {
            throw new BusinessRuleException(
                    "An approved, persisted stock count is required"
            );
        }

        List<StockAdjustmentResponse> responses = new ArrayList<>();
        for (StockAdjustmentDirection direction
                : StockAdjustmentDirection.values()) {
            List<StockCountItem> varianceItems = stockCount.getItems().stream()
                    .filter(item -> item.getPhysicalQuantity() != null)
                    .filter(item -> direction == StockAdjustmentDirection.IN
                            ? item.getVarianceQuantity().signum() > 0
                            : item.getVarianceQuantity().signum() < 0)
                    .sorted(Comparator.comparing(
                            item -> item.getInventoryItem().getId()
                    ))
                    .toList();
            if (varianceItems.isEmpty()) {
                continue;
            }

            StockAdjustment adjustment;
            try {
                adjustment = new StockAdjustment(
                        stockCount.getStockLocation(),
                        StockAdjustmentReason.MANUAL_CORRECTION,
                        direction,
                        stockCount.getCreatedByAccountId(),
                        "Stock count variance #" + stockCount.getId()
                );
                adjustment.linkToStockCount(stockCount);
                for (StockCountItem item : varianceItems) {
                    adjustment.addCountVarianceItem(
                            item.getInventoryItem(),
                            item.getVarianceQuantity().abs(),
                            item
                    );
                }
            } catch (IllegalArgumentException | IllegalStateException exception) {
                throw new BusinessRuleException(exception.getMessage());
            }

            adjustment = adjustmentRepository.saveAndFlush(adjustment);
            try {
                adjustment.submit(stockCount.getCreatedByAccountId());
                adjustment.approve(stockCount.getReviewedByAccountId());
            } catch (IllegalArgumentException | IllegalStateException exception) {
                throw new BusinessRuleException(exception.getMessage());
            }
            adjustmentRepository.saveAndFlush(adjustment);

            responses.add(new StockAdjustmentResponse(
                    postAdjustmentUnderLock(adjustment.getId(), user)
            ));
        }
        return List.copyOf(responses);
    }

    private StockAdjustment postAdjustmentUnderLock(
            Long adjustmentId,
            ProductUserPrincipal user
    ) {
        StockAdjustment adjustment = lockAdjustment(adjustmentId);
        String locationCode =
                adjustment.getStockLocation().getLocationCode();
        StockLocation lockedLocation = stockLocationRepository
                .findByIdForUpdate(locationCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Stock location not found"
                ));
        if (!lockedLocation.isActive()) {
            throw new BusinessRuleException(
                    "Cannot post an adjustment for an inactive location"
            );
        }
        if (adjustment.getStatus() != StockAdjustmentStatus.APPROVED) {
            throw new BusinessRuleException(
                    "Only APPROVED adjustments can be posted"
            );
        }

        List<StockAdjustmentItem> items = adjustment.getItems().stream()
                .sorted(Comparator.comparing(
                        item -> item.getInventoryItem().getId()
                ))
                .toList();
        if (items.isEmpty()) {
            throw new BusinessRuleException(
                    "Stock adjustment must contain at least one item"
            );
        }

        List<StockBalance> balances = new ArrayList<>(items.size());
        for (StockAdjustmentItem item : items) {
            InventoryItem inventoryItem = item.getInventoryItem();
            if (!inventoryItem.isActive()) {
                throw new BusinessRuleException(
                        "Cannot post an adjustment for an inactive item: "
                                + inventoryItem.getId()
                );
            }

            var balance = stockBalanceRepository.findForUpdate(
                    locationCode,
                    inventoryItem.getId()
            );
            if (balance.isEmpty()
                    && adjustment.getDirection()
                    == StockAdjustmentDirection.OUT) {
                throw new BusinessRuleException(
                        "Stock balance is missing for item: "
                                + inventoryItem.getId()
                );
            }
            balances.add(balance.orElseGet(() ->
                    new StockBalance(lockedLocation, inventoryItem)
            ));
        }

        for (int index = 0; index < items.size(); index++) {
            StockAdjustmentItem item = items.get(index);
            InventoryItem inventoryItem = item.getInventoryItem();
            StockBalance balance = balances.get(index);
            BigDecimal quantity = item.getAdjustmentQuantity();
            BigDecimal before = balance.getQuantityOnHand();
            StockMovementType movementType;

            if (adjustment.getDirection() == StockAdjustmentDirection.IN) {
                balance.increaseQuantity(quantity);
                movementType = StockMovementType.ADJUSTMENT_IN;
            } else {
                if (before.compareTo(quantity) < 0) {
                    throw new BusinessRuleException(
                            "Insufficient stock for item: "
                                    + inventoryItem.getItemCode()
                    );
                }
                balance.decreaseQuantity(quantity);
                movementType = StockMovementType.ADJUSTMENT_OUT;
            }

            stockBalanceRepository.saveAndFlush(balance);
            stockMovementRepository.save(new StockMovement(
                    lockedLocation,
                    inventoryItem,
                    movementType,
                    quantity,
                    before,
                    balance.getQuantityOnHand(),
                    MOVEMENT_REFERENCE,
                    adjustment.getId().toString(),
                    user.getAccountId(),
                    movementRemarks(adjustment)
            ));
        }

        try {
            adjustment.post(user.getAccountId());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw new BusinessRuleException(exception.getMessage());
        }

        StockAdjustment posted = adjustmentRepository.saveAndFlush(adjustment);
        auditLogService.record(
                "STOCK_ADJUSTMENT",
                posted.getId(),
                "POSTED",
                user.getAccountId(),
                StockAdjustmentStatus.APPROVED.name(),
                posted.getStatus().name(),
                null,
                null,
                locationCode
        );
        if (lowStockAlertEvaluationService != null) {
            for (StockAdjustmentItem item : items) {
                lowStockAlertEvaluationService.evaluateItem(locationCode, item.getInventoryItem().getId());
            }
        }
        return posted;
    }

    @Override
    public StockAdjustmentResponse getStockAdjustmentById(Long adjustmentId) {
        ProductUserPrincipal user = currentUser();
        requireReadableRole(user);
        validateId(adjustmentId);

        StockAdjustment adjustment = adjustmentRepository
                .findWithItemsById(adjustmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Stock adjustment not found"
                ));
        requireReadLocation(user, adjustment.getStockLocation()
                .getLocationCode());
        return new StockAdjustmentResponse(adjustment);
    }

    @Override
    public Page<StockAdjustmentResponse> getAllStockAdjustments(
            int page,
            int size
    ) {
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
            return adjustmentRepository
                    .findByStockLocation_LocationCodeOrderByCreatedAtDescIdDesc(
                            MAIN_LOCATION,
                            pageable
                    )
                    .map(StockAdjustmentResponse::new);
        }
        return adjustmentRepository
                .findAllByOrderByCreatedAtDescIdDesc(pageable)
                .map(StockAdjustmentResponse::new);
    }

    @Override
    public Page<StockAdjustmentResponse> getMyStockAdjustments(
            int page,
            int size
    ) {
        ProductUserPrincipal user = currentUser();
        requireAnyRole(user, "BRANCH_MANAGER", "STOCK_KEEPER");
        Pageable pageable = createPageable(page, size);

        String locationCode = "STOCK_KEEPER".equals(user.getRole())
                ? MAIN_LOCATION
                : assignedBranchCode(user);
        return adjustmentRepository
                .findByStockLocation_LocationCodeAndCreatedByAccountIdOrderByCreatedAtDescIdDesc(
                        locationCode,
                        user.getAccountId(),
                        pageable
                )
                .map(StockAdjustmentResponse::new);
    }

    @Override
    public Page<StockAdjustmentResponse> getStockAdjustmentsByLocation(
            String locationCode,
            int page,
            int size
    ) {
        ProductUserPrincipal user = currentUser();
        requireReadableRole(user);
        validateLocationCode(locationCode);
        requireReadLocation(user, locationCode);

        return adjustmentRepository
                .findByStockLocation_LocationCodeOrderByCreatedAtDescIdDesc(
                        locationCode,
                        createPageable(page, size)
                )
                .map(StockAdjustmentResponse::new);
    }

    @Override
    public Page<StockAdjustmentResponse> getStockAdjustmentsByStatus(
            StockAdjustmentStatus status,
            int page,
            int size
    ) {
        ProductUserPrincipal user = currentUser();
        requireReadableRole(user);
        if (status == null) {
            throw new BusinessRuleException(
                    "Stock adjustment status is required"
            );
        }

        Pageable pageable = createPageable(page, size);
        if ("BRANCH_MANAGER".equals(user.getRole())) {
            return adjustmentRepository
                    .findByStockLocation_LocationCodeAndStatusOrderByCreatedAtDescIdDesc(
                            assignedBranchCode(user),
                            status,
                            pageable
                    )
                    .map(StockAdjustmentResponse::new);
        }
        if ("STOCK_KEEPER".equals(user.getRole())) {
            return adjustmentRepository
                    .findByStockLocation_LocationCodeAndStatusOrderByCreatedAtDescIdDesc(
                            MAIN_LOCATION,
                            status,
                            pageable
                    )
                    .map(StockAdjustmentResponse::new);
        }
        return adjustmentRepository
                .findByStatusOrderByCreatedAtDescIdDesc(status, pageable)
                .map(StockAdjustmentResponse::new);
    }

    @Override
    public Page<StockAdjustmentResponse> getStockAdjustmentsByDateRange(
            LocalDate startDate,
            LocalDate endDate,
            String locationCode,
            StockAdjustmentStatus status,
            int page,
            int size
    ) {
        ProductUserPrincipal user = currentUser();
        requireReadableRole(user);
        if (startDate == null || endDate == null
                || startDate.isAfter(endDate)
                || endDate.equals(LocalDate.MAX)) {
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
                        "You can only view adjustments for your assigned branch"
                );
            }
            scopedLocation = assigned;
        } else if ("STOCK_KEEPER".equals(user.getRole())) {
            if (scopedLocation != null
                    && !MAIN_LOCATION.equals(scopedLocation)) {
                throw new AccessDeniedException(
                        "Stock Keeper can only view Main warehouse adjustments"
                );
            }
            scopedLocation = MAIN_LOCATION;
        }

        Pageable pageable = createPageable(page, size);
        return adjustmentRepository.findByFilters(
                        startDate.atStartOfDay(),
                        endDate.plusDays(1).atStartOfDay(),
                        scopedLocation,
                        status,
                        pageable
                )
                .map(StockAdjustmentResponse::new);
    }

    private void addItems(
            StockAdjustment adjustment,
            List<StockAdjustmentItemRequest> requests
    ) {
        if (requests == null || requests.isEmpty()) {
            throw new BusinessRuleException(
                    "Stock adjustment must contain at least one item"
            );
        }

        Set<Long> itemIds = new HashSet<>();
        for (StockAdjustmentItemRequest request : requests) {
            if (request == null
                    || request.getInventoryItemId() == null
                    || request.getInventoryItemId() <= 0) {
                throw new BusinessRuleException(
                        "Valid inventory item ID is required"
                );
            }
            if (!itemIds.add(request.getInventoryItemId())) {
                throw new BusinessRuleException(
                        "Duplicate inventory item in stock adjustment"
                );
            }

            InventoryItem inventoryItem = inventoryItemRepository
                    .findById(request.getInventoryItemId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Inventory item not found: "
                                    + request.getInventoryItemId()
                    ));
            try {
                adjustment.addItem(
                        inventoryItem,
                        request.getAdjustmentQuantity()
                );
            } catch (IllegalArgumentException | IllegalStateException exception) {
                throw new BusinessRuleException(exception.getMessage());
            }
        }
    }

    private List<StockAdjustmentItem> buildItems(
            StockAdjustment adjustment,
            List<StockAdjustmentItemRequest> requests
    ) {
        if (requests == null || requests.isEmpty()) {
            throw new BusinessRuleException(
                    "Stock adjustment must contain at least one item"
            );
        }

        Set<Long> itemIds = new HashSet<>();
        List<StockAdjustmentItem> result =
                new ArrayList<>(requests.size());
        for (StockAdjustmentItemRequest request : requests) {
            if (request == null
                    || request.getInventoryItemId() == null
                    || request.getInventoryItemId() <= 0) {
                throw new BusinessRuleException(
                        "Valid inventory item ID is required"
                );
            }
            Long itemId = request.getInventoryItemId();
            if (!itemIds.add(itemId)) {
                throw new BusinessRuleException(
                        "Duplicate inventory item in stock adjustment"
                );
            }
            InventoryItem inventoryItem = inventoryItemRepository.findById(itemId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Inventory item not found: " + itemId
                    ));
            try {
                result.add(new StockAdjustmentItem(
                        adjustment,
                        inventoryItem,
                        request.getAdjustmentQuantity()
                ));
            } catch (IllegalArgumentException | IllegalStateException exception) {
                throw new BusinessRuleException(exception.getMessage());
            }
        }
        return result;
    }

    private StockAdjustment lockAdjustment(Long adjustmentId) {
        return adjustmentRepository.findByIdForUpdate(adjustmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Stock adjustment not found"
                ));
    }

    private StockLocation getActiveLocation(String locationCode) {
        StockLocation location = stockLocationRepository.findById(locationCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Stock location not found: " + locationCode
                ));
        if (!location.isActive()) {
            throw new BusinessRuleException(
                    "Stock location is inactive"
            );
        }
        return location;
    }

    private void requireCreatorAndManageLocation(
            ProductUserPrincipal user,
            StockAdjustment adjustment
    ) {
        requireAnyRole(user, "BRANCH_MANAGER", "STOCK_KEEPER");
        if (!user.getAccountId().equals(adjustment.getCreatedByAccountId())) {
            throw new AccessDeniedException(
                    "Only the adjustment creator can manage this draft"
            );
        }
        requireManageLocation(
                user,
                adjustment.getStockLocation().getLocationCode()
        );
    }

    private void requireManageLocation(
            ProductUserPrincipal user,
            String locationCode
    ) {
        if ("STOCK_KEEPER".equals(user.getRole())) {
            if (!MAIN_LOCATION.equals(locationCode)) {
                throw new AccessDeniedException(
                        "Stock Keeper can manage only Main warehouse adjustments"
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
                        "You can only access your assigned branch"
                );
            }
        } else if ("STOCK_KEEPER".equals(user.getRole())
                && !MAIN_LOCATION.equals(locationCode)) {
            throw new AccessDeniedException(
                    "Stock Keeper can only access Main warehouse adjustments"
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
                    "Valid stock adjustment ID is required"
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

    private String movementRemarks(StockAdjustment adjustment) {
        if (adjustment.getRemarks() == null) {
            return adjustment.getAdjustmentReason().name();
        }
        return adjustment.getAdjustmentReason().name()
                + ": " + adjustment.getRemarks();
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
