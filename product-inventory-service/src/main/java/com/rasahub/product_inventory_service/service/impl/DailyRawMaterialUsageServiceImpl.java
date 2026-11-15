package com.rasahub.product_inventory_service.service.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import com.rasahub.product_inventory_service.dto.dailyrawmaterialusage.DailyRawMaterialUsageCreateRequest;
import com.rasahub.product_inventory_service.dto.dailyrawmaterialusage.DailyRawMaterialUsageItemRequest;
import com.rasahub.product_inventory_service.dto.dailyrawmaterialusage.DailyRawMaterialUsageResponse;
import com.rasahub.product_inventory_service.dto.dailyrawmaterialusage.DailyRawMaterialUsageUpdateRequest;
import com.rasahub.product_inventory_service.entity.DailyRawMaterialUsage;
import com.rasahub.product_inventory_service.entity.DailyRawMaterialUsageItem;
import com.rasahub.product_inventory_service.entity.InventoryItem;
import com.rasahub.product_inventory_service.entity.StockBalance;
import com.rasahub.product_inventory_service.entity.StockLocation;
import com.rasahub.product_inventory_service.entity.StockMovement;
import com.rasahub.product_inventory_service.enums.DailyRawMaterialUsageStatus;
import com.rasahub.product_inventory_service.enums.MeasurementUnit;
import com.rasahub.product_inventory_service.enums.StockMovementType;
import com.rasahub.product_inventory_service.exception.BusinessRuleException;
import com.rasahub.product_inventory_service.exception.ResourceNotFoundException;
import com.rasahub.product_inventory_service.repository.DailyRawMaterialUsageRepository;
import com.rasahub.product_inventory_service.repository.InventoryItemRepository;
import com.rasahub.product_inventory_service.repository.StockBalanceRepository;
import com.rasahub.product_inventory_service.repository.StockLocationRepository;
import com.rasahub.product_inventory_service.repository.StockMovementRepository;
import com.rasahub.product_inventory_service.security.ProductUserPrincipal;
import com.rasahub.product_inventory_service.service.DailyRawMaterialUsageService;
import com.rasahub.product_inventory_service.service.LowStockAlertEvaluationService;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class DailyRawMaterialUsageServiceImpl
        implements DailyRawMaterialUsageService {

    private static final Set<String> BRANCH_CODES =
            Set.of("BR001", "BR002", "BR003");

    private final DailyRawMaterialUsageRepository usageRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final StockLocationRepository stockLocationRepository;
    private final StockBalanceRepository stockBalanceRepository;
    private final StockMovementRepository stockMovementRepository;
    private final HttpServletRequest servletRequest;
    private final RestClient authClient;
    private final LowStockAlertEvaluationService lowStockAlertEvaluationService;

    public DailyRawMaterialUsageServiceImpl(
            DailyRawMaterialUsageRepository usageRepository,
            InventoryItemRepository inventoryItemRepository,
            StockLocationRepository stockLocationRepository,
            StockBalanceRepository stockBalanceRepository,
            StockMovementRepository stockMovementRepository,
            HttpServletRequest servletRequest,
            @Value("${branch.sync.auth-base-url}") String authBaseUrl
    ) {
        this(
                usageRepository,
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
    public DailyRawMaterialUsageServiceImpl(
            DailyRawMaterialUsageRepository usageRepository,
            InventoryItemRepository inventoryItemRepository,
            StockLocationRepository stockLocationRepository,
            StockBalanceRepository stockBalanceRepository,
            StockMovementRepository stockMovementRepository,
            HttpServletRequest servletRequest,
            @Value("${branch.sync.auth-base-url}") String authBaseUrl,
            LowStockAlertEvaluationService lowStockAlertEvaluationService
    ) {
        this.usageRepository = usageRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.stockLocationRepository = stockLocationRepository;
        this.stockBalanceRepository = stockBalanceRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.servletRequest = servletRequest;
        this.lowStockAlertEvaluationService = lowStockAlertEvaluationService;
        this.authClient = RestClient.builder()
                .baseUrl(authBaseUrl)
                .build();
    }

    @Override
    @Transactional
    public DailyRawMaterialUsageResponse createDailyUsage(
            DailyRawMaterialUsageCreateRequest request
    ) {
        ProductUserPrincipal user = currentUser();
        requireAnyRole(user, "BRANCH_MANAGER");

        validateRequest(request);

        String branchCode = getAssignedBranchCode(user);
        StockLocation branchLocation = stockLocationRepository
                .findById(branchCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Branch stock location not found"
                ));

        if (!branchLocation.isActive()) {
            throw new BusinessRuleException(
                    "Cannot create usage for an inactive branch"
            );
        }

        DailyRawMaterialUsage usage = new DailyRawMaterialUsage(
                branchLocation,
                request.getUsageDate(),
                user.getAccountId(),
                request.getRemarks()
        );

        Set<Long> seenIds = new HashSet<>();

        for (DailyRawMaterialUsageItemRequest itemRequest
                : request.getItems()) {
            Long itemId = itemRequest.getInventoryItemId();

            if (!seenIds.add(itemId)) {
                throw new BusinessRuleException(
                        "Duplicate inventory item in daily usage"
                );
            }

            InventoryItem item = inventoryItemRepository
                    .findById(itemId)
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Inventory item not found: " + itemId
                            )
                    );

            if (!item.isActive()) {
                throw new BusinessRuleException(
                        "Cannot use an inactive inventory item"
                );
            }

            validateQuantity(item, itemRequest.getUsedQuantity());
            usage.addItem(item, itemRequest.getUsedQuantity());
        }

        DailyRawMaterialUsage saved = usageRepository.saveAndFlush(usage);
        return new DailyRawMaterialUsageResponse(saved);
    }

    @Override
    @Transactional
    public DailyRawMaterialUsageResponse updateDraftUsage(
            Long usageId,
            DailyRawMaterialUsageUpdateRequest request
    ) {
        ProductUserPrincipal user = currentUser();
        requireAnyRole(user, "BRANCH_MANAGER");
        validateId(usageId);

        if (request == null || request.getItems() == null
                || request.getItems().isEmpty()) {
            throw new BusinessRuleException(
                    "Daily usage must contain at least one item"
            );
        }

        DailyRawMaterialUsage usage = usageRepository
                .findWithItemsById(usageId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Daily usage not found"
                        )
                );

        requireAssignedBranch(user, usage.getBranchLocation().getLocationCode());

        if (usage.getStatus() != DailyRawMaterialUsageStatus.DRAFT) {
            throw new BusinessRuleException(
                    "Only DRAFT usage records can be updated"
            );
        }

        List<DailyRawMaterialUsageItem> newItems = new ArrayList<>();
        Set<Long> seenIds = new HashSet<>();

        for (DailyRawMaterialUsageItemRequest itemRequest
                : request.getItems()) {
            Long itemId = itemRequest.getInventoryItemId();

            if (!seenIds.add(itemId)) {
                throw new BusinessRuleException(
                        "Duplicate inventory item in daily usage"
                );
            }

            InventoryItem item = inventoryItemRepository
                    .findById(itemId)
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Inventory item not found: " + itemId
                            )
                    );

            if (!item.isActive()) {
                throw new BusinessRuleException(
                        "Cannot use an inactive inventory item"
                );
            }

            validateQuantity(item, itemRequest.getUsedQuantity());

            DailyRawMaterialUsageItem usageItem =
                    new DailyRawMaterialUsageItem(
                            usage,
                            item,
                            itemRequest.getUsedQuantity()
                    );
            newItems.add(usageItem);
        }

        usage.updateDraftDetails(request.getRemarks());
        usage.replaceItems(newItems);

        DailyRawMaterialUsage saved = usageRepository.saveAndFlush(usage);
        return new DailyRawMaterialUsageResponse(saved);
    }

    @Override
    @Transactional
    public DailyRawMaterialUsageResponse postDailyUsage(Long usageId) {
        ProductUserPrincipal user = currentUser();
        requireAnyRole(user, "BRANCH_MANAGER");
        validateId(usageId);

        DailyRawMaterialUsage usage = usageRepository
                .findByIdForUpdate(usageId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Daily usage not found"
                        )
                );

        requireAssignedBranch(user, usage.getBranchLocation().getLocationCode());

        if (usage.getStatus() != DailyRawMaterialUsageStatus.DRAFT) {
            throw new BusinessRuleException(
                    "Only DRAFT usage records can be posted"
            );
        }

        if (usage.getItems() == null || usage.getItems().isEmpty()) {
            throw new BusinessRuleException(
                    "Daily usage must contain at least one item"
            );
        }

        String branchCode = usage.getBranchLocation().getLocationCode();

        List<DailyRawMaterialUsageItem> itemsInLockOrder =
                usage.getItems().stream()
                        .sorted(Comparator.comparing(
                                usageItem -> usageItem.getInventoryItem().getId()
                        ))
                        .toList();

        for (DailyRawMaterialUsageItem usageItem : itemsInLockOrder) {
            InventoryItem item = usageItem.getInventoryItem();
            BigDecimal quantity = usageItem.getUsedQuantity();

            StockBalance balance = stockBalanceRepository
                    .findForUpdate(branchCode, item.getId())
                    .orElseThrow(() ->
                            new BusinessRuleException(
                                    "No stock balance found for item: "
                                            + item.getId()
                            )
                    );

            BigDecimal before = balance.getQuantityOnHand();

            if (before.compareTo(quantity) < 0) {
                throw new BusinessRuleException(
                        "Insufficient stock for item: "
                                + item.getItemCode()
                );
            }

            balance.decreaseQuantity(quantity);
            stockBalanceRepository.saveAndFlush(balance);

            stockMovementRepository.save(
                    new StockMovement(
                            balance.getStockLocation(),
                            item,
                            StockMovementType.DAILY_USAGE,
                            quantity,
                            before,
                            balance.getQuantityOnHand(),
                            "DAILY_USAGE",
                            usage.getId().toString(),
                            user.getAccountId(),
                            usage.getRemarks()
                    )
            );
        }

        usage.post(user.getAccountId());
        DailyRawMaterialUsage saved = usageRepository.saveAndFlush(usage);
        if (lowStockAlertEvaluationService != null) {
            for (DailyRawMaterialUsageItem usageItem : itemsInLockOrder) {
                lowStockAlertEvaluationService.evaluateItem(branchCode, usageItem.getInventoryItem().getId());
            }
        }
        return new DailyRawMaterialUsageResponse(saved);
    }

    @Override
    public DailyRawMaterialUsageResponse getDailyUsageById(Long usageId) {
        ProductUserPrincipal user = currentUser();
        requireAnyRole(user, "OWNER", "MAIN_OFFICE_MANAGER", "BRANCH_MANAGER");
        validateId(usageId);

        DailyRawMaterialUsage usage = usageRepository
                .findWithItemsById(usageId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Daily usage not found"
                        )
                );

        if ("BRANCH_MANAGER".equals(user.getRole())) {
            requireAssignedBranch(user, usage.getBranchLocation().getLocationCode());
        }

        return new DailyRawMaterialUsageResponse(usage);
    }

    @Override
    public Page<DailyRawMaterialUsageResponse> getAllDailyUsages(
            int page,
            int size
    ) {
        ProductUserPrincipal user = currentUser();
        requireAnyRole(user, "OWNER", "MAIN_OFFICE_MANAGER");

        return usageRepository
                .findAllByOrderByUsageDateDescIdDesc(createPageable(page, size))
                .map(DailyRawMaterialUsageResponse::new);
    }

    @Override
    public Page<DailyRawMaterialUsageResponse> getMyBranchDailyUsages(
            int page,
            int size
    ) {
        ProductUserPrincipal user = currentUser();
        requireAnyRole(user, "BRANCH_MANAGER");

        String branchCode = getAssignedBranchCode(user);

        return usageRepository
                .findByBranchLocation_LocationCodeOrderByUsageDateDescIdDesc(
                        branchCode,
                        createPageable(page, size)
                )
                .map(DailyRawMaterialUsageResponse::new);
    }

    @Override
    public Page<DailyRawMaterialUsageResponse> getDailyUsagesByBranch(
            String branchLocationCode,
            int page,
            int size
    ) {
        ProductUserPrincipal user = currentUser();
        requireAnyRole(user, "OWNER", "MAIN_OFFICE_MANAGER", "BRANCH_MANAGER");
        validateBranchCode(branchLocationCode);

        if ("BRANCH_MANAGER".equals(user.getRole())) {
            requireAssignedBranch(user, branchLocationCode);
        }

        return usageRepository
                .findByBranchLocation_LocationCodeOrderByUsageDateDescIdDesc(
                        branchLocationCode,
                        createPageable(page, size)
                )
                .map(DailyRawMaterialUsageResponse::new);
    }

    @Override
    public Page<DailyRawMaterialUsageResponse> getDailyUsagesByStatus(
            DailyRawMaterialUsageStatus status,
            int page,
            int size
    ) {
        ProductUserPrincipal user = currentUser();
        requireAnyRole(user, "OWNER", "MAIN_OFFICE_MANAGER", "BRANCH_MANAGER");
        validateStatus(status);

        Pageable pageable = createPageable(page, size);

        if ("BRANCH_MANAGER".equals(user.getRole())) {
            String branchCode = getAssignedBranchCode(user);
            return usageRepository
                    .findByBranchLocation_LocationCodeAndStatusOrderByUsageDateDescIdDesc(
                            branchCode,
                            status,
                            pageable
                    )
                    .map(DailyRawMaterialUsageResponse::new);
        }

        return usageRepository
                .findByStatusOrderByUsageDateDescIdDesc(
                        status,
                        pageable
                )
                .map(DailyRawMaterialUsageResponse::new);
    }

    @Override
    public Page<DailyRawMaterialUsageResponse> getDailyUsagesByDateRange(
            String branchLocationCode,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size
    ) {
        ProductUserPrincipal user = currentUser();
        requireAnyRole(user, "OWNER", "MAIN_OFFICE_MANAGER", "BRANCH_MANAGER");
        validateBranchCode(branchLocationCode);

        if (startDate == null || endDate == null) {
            throw new BusinessRuleException(
                    "Start date and end date are required"
            );
        }

        if (startDate.isAfter(endDate)) {
            throw new BusinessRuleException(
                    "Start date cannot be after end date"
            );
        }

        if ("BRANCH_MANAGER".equals(user.getRole())) {
            requireAssignedBranch(user, branchLocationCode);
        }

        Pageable pageable = createPageable(page, size);
        return usageRepository
                .findByBranchLocation_LocationCodeAndUsageDateBetweenOrderByUsageDateDescIdDesc(
                        branchLocationCode,
                        startDate,
                        endDate,
                        pageable
                )
                .map(DailyRawMaterialUsageResponse::new);
    }

    private String getAssignedBranchCode(
            ProductUserPrincipal user
    ) {
        if (user.getBranchId() == null || user.getBranchId() <= 0) {
            throw new AccessDeniedException(
                    "Valid branch assignment is required"
            );
        }

        String authorizationHeader = servletRequest.getHeader(
                HttpHeaders.AUTHORIZATION
        );

        if (authorizationHeader == null
                || !authorizationHeader.startsWith("Bearer ")) {
            throw new AccessDeniedException(
                    "Valid authentication token is required"
            );
        }

        try {
            AuthBranchData branch = authClient
                    .get()
                    .uri("/api/v1/branches/{id}", user.getBranchId())
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                    .retrieve()
                    .body(AuthBranchData.class);

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
        } catch (RestClientException ex) {
            throw new AccessDeniedException(
                    "Unable to verify branch assignment",
                    ex
            );
        }
    }

    private void requireAssignedBranch(
            ProductUserPrincipal user,
            String branchCode
    ) {
        if (!getAssignedBranchCode(user).equals(branchCode)) {
            throw new AccessDeniedException(
                    "You can only access your assigned branch"
            );
        }
    }

    private void validateRequest(
            DailyRawMaterialUsageCreateRequest request
    ) {
        if (request == null) {
            throw new BusinessRuleException(
                    "Daily usage request is required"
            );
        }

        if (request.getUsageDate() == null) {
            throw new BusinessRuleException(
                    "Usage date is required"
            );
        }

        if (request.getUsageDate().isAfter(LocalDate.now())) {
            throw new BusinessRuleException(
                    "Usage date cannot be in the future"
            );
        }

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessRuleException(
                    "Daily usage must contain at least one item"
            );
        }
    }

    private void validateQuantity(
            InventoryItem item,
            BigDecimal quantity
    ) {
        if (quantity == null) {
            throw new BusinessRuleException(
                    "Used quantity is required"
            );
        }

        if (quantity.signum() <= 0) {
            throw new BusinessRuleException(
                    "Used quantity must be greater than zero"
            );
        }

        if (quantity.scale() > 3
                || quantity.precision() - quantity.scale() > 15) {
            throw new BusinessRuleException(
                    "Invalid used quantity precision"
            );
        }

        if (item.getMeasurementUnit() == MeasurementUnit.PCS
                && quantity.stripTrailingZeros().scale() > 0) {
            throw new BusinessRuleException(
                    "PCS usage quantity must be a whole number"
            );
        }
    }

    private void validateBranchCode(String branchCode) {
        if (branchCode == null || !BRANCH_CODES.contains(branchCode)) {
            throw new BusinessRuleException(
                    "Branch must be BR001, BR002 or BR003"
            );
        }
    }

    private void validateStatus(
            DailyRawMaterialUsageStatus status
    ) {
        if (status == null) {
            throw new BusinessRuleException(
                    "Daily usage status is required"
            );
        }
    }

    private void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessRuleException(
                    "Valid daily usage ID is required"
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
        Authentication authentication = SecurityContextHolder
                .getContext()
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
