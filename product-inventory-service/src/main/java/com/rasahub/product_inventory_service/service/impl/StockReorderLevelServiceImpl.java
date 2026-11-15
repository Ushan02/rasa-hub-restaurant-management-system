package com.rasahub.product_inventory_service.service.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.rasahub.product_inventory_service.dto.stockreorderlevel.StockReorderLevelResponse;
import com.rasahub.product_inventory_service.entity.InventoryItem;
import com.rasahub.product_inventory_service.entity.StockLocation;
import com.rasahub.product_inventory_service.entity.StockReorderLevel;
import com.rasahub.product_inventory_service.exception.BusinessRuleException;
import com.rasahub.product_inventory_service.exception.ResourceNotFoundException;
import com.rasahub.product_inventory_service.repository.InventoryItemRepository;
import com.rasahub.product_inventory_service.repository.StockBalanceRepository;
import com.rasahub.product_inventory_service.repository.StockLocationRepository;
import com.rasahub.product_inventory_service.repository.StockReorderLevelRepository;
import com.rasahub.product_inventory_service.security.ProductUserPrincipal;
import com.rasahub.product_inventory_service.service.LowStockAlertEvaluationService;
import com.rasahub.product_inventory_service.service.StockReorderLevelService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class StockReorderLevelServiceImpl implements StockReorderLevelService {

    private static final Set<String> BRANCH_CODES = Set.of("BR001", "BR002", "BR003");
    private static final String MAIN_LOCATION = "Main";

    private final StockReorderLevelRepository stockReorderLevelRepository;
    private final StockLocationRepository stockLocationRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final StockBalanceRepository stockBalanceRepository;
    private final LowStockAlertEvaluationService lowStockAlertEvaluationService;
    private final HttpServletRequest servletRequest;
    private final RestClient authClient;

    public StockReorderLevelServiceImpl(
            StockReorderLevelRepository stockReorderLevelRepository,
            StockLocationRepository stockLocationRepository,
            InventoryItemRepository inventoryItemRepository,
            LowStockAlertEvaluationService lowStockAlertEvaluationService,
            StockBalanceRepository stockBalanceRepository
    ) {
        this(
                stockReorderLevelRepository,
                stockLocationRepository,
                inventoryItemRepository,
                lowStockAlertEvaluationService,
                stockBalanceRepository,
                null,
                "http://localhost"
        );
    }

    @Autowired
    public StockReorderLevelServiceImpl(
            StockReorderLevelRepository stockReorderLevelRepository,
            StockLocationRepository stockLocationRepository,
            InventoryItemRepository inventoryItemRepository,
            LowStockAlertEvaluationService lowStockAlertEvaluationService,
            StockBalanceRepository stockBalanceRepository,
            HttpServletRequest servletRequest,
            @Value("${branch.sync.auth-base-url}") String authBaseUrl
    ) {
        this.stockReorderLevelRepository = stockReorderLevelRepository;
        this.stockLocationRepository = stockLocationRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.stockBalanceRepository = stockBalanceRepository;
        this.lowStockAlertEvaluationService = lowStockAlertEvaluationService;
        this.servletRequest = servletRequest;
        this.authClient = RestClient.builder().baseUrl(authBaseUrl).build();
    }

    @Override
    @Transactional
    public StockReorderLevelResponse createConfiguration(
            String locationCode,
            Long inventoryItemId,
            BigDecimal reorderLevel,
            BigDecimal recommendedReorderQuantity,
            Long createdByAccountId
    ) {
        ProductUserPrincipal user = currentUser();
        requireAnyRole(user, "OWNER", "MAIN_OFFICE_MANAGER", "STOCK_KEEPER");
        requireWriteLocation(user, locationCode);
        Long actorId = resolveActorAccountId(user, createdByAccountId);

        validateLocation(locationCode);
        validateInventoryItem(inventoryItemId);
        validateReorderLevel(reorderLevel);

        if (stockReorderLevelRepository.existsByStockLocation_LocationCodeAndInventoryItem_Id(locationCode, inventoryItemId)) {
            throw new BusinessRuleException("Reorder level already exists for this location and item");
        }

        StockLocation location = findLocation(locationCode);
        InventoryItem item = findInventoryItem(inventoryItemId);

        StockReorderLevel config = new StockReorderLevel(location, item, reorderLevel);
        config.setRecommendedReorderQuantity(recommendedReorderQuantity);
        config.setCreatedByAccountId(actorId);
        config.setUpdatedByAccountId(actorId);

        StockReorderLevel saved = stockReorderLevelRepository.saveAndFlush(config);
        if (lowStockAlertEvaluationService != null) {
            lowStockAlertEvaluationService.evaluateItem(locationCode, inventoryItemId);
        }
        return toResponse(saved);
    }

    @Override
    @Transactional
    public StockReorderLevelResponse updateConfiguration(
            String locationCode,
            Long inventoryItemId,
            BigDecimal reorderLevel,
            BigDecimal recommendedReorderQuantity,
            Long updatedByAccountId
    ) {
        ProductUserPrincipal user = currentUser();
        requireAnyRole(user, "OWNER", "MAIN_OFFICE_MANAGER", "STOCK_KEEPER");
        requireWriteLocation(user, locationCode);
        Long actorId = resolveActorAccountId(user, updatedByAccountId);

        validateLocation(locationCode);
        validateInventoryItem(inventoryItemId);
        validateReorderLevel(reorderLevel);

        StockReorderLevel config = stockReorderLevelRepository
                .findForUpdate(locationCode, inventoryItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Reorder level configuration not found"));

        config.setReorderLevel(reorderLevel);
        config.setRecommendedReorderQuantity(recommendedReorderQuantity);
        config.setUpdatedByAccountId(actorId);

        StockReorderLevel saved = stockReorderLevelRepository.saveAndFlush(config);
        if (lowStockAlertEvaluationService != null) {
            lowStockAlertEvaluationService.evaluateItem(locationCode, inventoryItemId);
        }
        return toResponse(saved);
    }

    @Override
    @Transactional
    public StockReorderLevelResponse deactivateConfiguration(
            String locationCode,
            Long inventoryItemId,
            Long updatedByAccountId
    ) {
        ProductUserPrincipal user = currentUser();
        requireAnyRole(user, "OWNER", "MAIN_OFFICE_MANAGER", "STOCK_KEEPER");
        requireWriteLocation(user, locationCode);
        Long actorId = resolveActorAccountId(user, updatedByAccountId);

        validateLocation(locationCode);
        validateInventoryItem(inventoryItemId);

        StockReorderLevel config = stockReorderLevelRepository
                .findForUpdate(locationCode, inventoryItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Reorder level configuration not found"));

        boolean wasActive = config.isActive();
        config.setActive(false);
        config.setUpdatedByAccountId(actorId);
        StockReorderLevel saved = stockReorderLevelRepository.saveAndFlush(config);
        if (wasActive && lowStockAlertEvaluationService != null) {
            lowStockAlertEvaluationService.closeForMonitoringDisabled(locationCode, inventoryItemId);
        }
        return toResponse(saved);
    }

    @Override
    @Transactional
    public StockReorderLevelResponse reactivateConfiguration(
            String locationCode,
            Long inventoryItemId,
            Long updatedByAccountId
    ) {
        ProductUserPrincipal user = currentUser();
        requireAnyRole(user, "OWNER", "MAIN_OFFICE_MANAGER", "STOCK_KEEPER");
        requireWriteLocation(user, locationCode);
        Long actorId = resolveActorAccountId(user, updatedByAccountId);
        validateLocation(locationCode);
        validateInventoryItem(inventoryItemId);

        StockReorderLevel config = stockReorderLevelRepository
                .findForUpdate(locationCode, inventoryItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Reorder level configuration not found"));
        config.setActive(true);
        config.setUpdatedByAccountId(actorId);
        StockReorderLevel saved = stockReorderLevelRepository.saveAndFlush(config);
        if (lowStockAlertEvaluationService != null) {
            lowStockAlertEvaluationService.evaluateItem(locationCode, inventoryItemId);
        }
        return toResponse(saved);
    }

    @Override
    public List<StockReorderLevelResponse> getConfigurationsByLocation(String locationCode) {
        validateLocation(locationCode);
        ProductUserPrincipal user = currentUser();
        requireReadableRole(user);
        requireReadLocation(user, locationCode);
        List<StockReorderLevel> configurations =
                stockReorderLevelRepository.findByStockLocation_LocationCodeAndActiveTrue(locationCode);
        Map<Long, BigDecimal> quantities = stockBalanceRepository
                .findByStockLocation_LocationCode(locationCode)
                .stream()
                .collect(Collectors.toMap(
                        balance -> balance.getInventoryItem().getId(),
                        balance -> balance.getQuantityOnHand(),
                        (first, ignored) -> first
                ));
        return configurations.stream()
                .map(config -> StockReorderLevelResponse.from(
                        config,
                        quantities.getOrDefault(config.getInventoryItem().getId(), BigDecimal.ZERO)
                ))
                .toList();
    }

    @Override
    public Optional<StockReorderLevelResponse> getConfiguration(String locationCode, Long inventoryItemId) {
        validateLocation(locationCode);
        validateInventoryItem(inventoryItemId);
        ProductUserPrincipal user = currentUser();
        requireReadableRole(user);
        requireReadLocation(user, locationCode);
        return stockReorderLevelRepository.findByStockLocation_LocationCodeAndInventoryItem_Id(locationCode, inventoryItemId)
                .map(this::toResponse);
    }

    private ProductUserPrincipal currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof ProductUserPrincipal user) || user.getAccountId() == null || user.getAccountId() <= 0) {
            throw new AccessDeniedException("Authenticated staff account is required");
        }
        return user;
    }

    private Long resolveActorAccountId(ProductUserPrincipal user, Long suppliedAccountId) {
        Long effective = user != null ? user.getAccountId() : suppliedAccountId;
        if (effective == null || effective <= 0) {
            throw new BusinessRuleException("Authenticated staff account is required");
        }
        if (user != null && suppliedAccountId != null && !user.getAccountId().equals(suppliedAccountId)) {
            throw new AccessDeniedException("You cannot impersonate another staff account");
        }
        return effective;
    }

    private void requireAnyRole(ProductUserPrincipal user, String... allowedRoles) {
        for (String role : allowedRoles) {
            if (role.equals(user.getRole())) {
                return;
            }
        }
        throw new AccessDeniedException("You do not have permission for this operation");
    }

    private void requireReadableRole(ProductUserPrincipal user) {
        requireAnyRole(user, "OWNER", "MAIN_OFFICE_MANAGER", "ACCOUNTANT", "STOCK_KEEPER", "BRANCH_MANAGER");
    }

    private void requireWriteLocation(ProductUserPrincipal user, String locationCode) {
        if ("STOCK_KEEPER".equals(user.getRole()) && !MAIN_LOCATION.equals(locationCode)) {
            throw new AccessDeniedException("Stock Keeper can only manage Main warehouse reorder levels");
        }
    }

    private void requireReadLocation(ProductUserPrincipal user, String locationCode) {
        if ("BRANCH_MANAGER".equals(user.getRole())) {
            if (!BRANCH_CODES.contains(locationCode) || !assignedBranchCode(user).equals(locationCode)) {
                throw new AccessDeniedException("You can only access reorder levels for your assigned branch");
            }
            return;
        }
        if ("STOCK_KEEPER".equals(user.getRole()) && !MAIN_LOCATION.equals(locationCode)) {
            throw new AccessDeniedException("Stock Keeper can only access Main warehouse reorder levels");
        }
    }

    private String assignedBranchCode(ProductUserPrincipal user) {
        if (user.getBranchId() == null || user.getBranchId() <= 0) {
            throw new AccessDeniedException("Valid branch assignment is required");
        }
        if (servletRequest == null) {
            throw new AccessDeniedException("Branch assignment cannot be verified in this context");
        }
        String bearer = servletRequest.getHeader(HttpHeaders.AUTHORIZATION);
        if (bearer == null || !bearer.startsWith("Bearer ")) {
            throw new AccessDeniedException("Valid Bearer token is required");
        }
        try {
            AuthBranchData branch = authClient.get()
                    .uri("/api/v1/branches/{id}", user.getBranchId())
                    .header(HttpHeaders.AUTHORIZATION, bearer)
                    .retrieve()
                    .body(AuthBranchData.class);
            if (branch == null || branch.id() == null || !user.getBranchId().equals(branch.id()) || !branch.active() || !BRANCH_CODES.contains(branch.branchCode())) {
                throw new AccessDeniedException("A valid active branch assignment is required");
            }
            return branch.branchCode();
        } catch (RestClientException exception) {
            throw new AccessDeniedException("Unable to verify branch assignment", exception);
        }
    }

    private void validateLocation(String locationCode) {
        if (locationCode == null || locationCode.isBlank()) {
            throw new BusinessRuleException("Location code is required");
        }
        if (!stockLocationRepository.existsById(locationCode.trim())) {
            throw new ResourceNotFoundException("Stock location not found");
        }
    }

    private void validateInventoryItem(Long inventoryItemId) {
        if (inventoryItemId == null || inventoryItemId < 1) {
            throw new BusinessRuleException("Valid inventory item ID is required");
        }
        if (!inventoryItemRepository.existsById(inventoryItemId)) {
            throw new ResourceNotFoundException("Inventory item not found");
        }
    }

    private void validateReorderLevel(BigDecimal reorderLevel) {
        if (reorderLevel == null) {
            throw new BusinessRuleException("Reorder level is required");
        }
        if (reorderLevel.signum() < 0) {
            throw new BusinessRuleException("Reorder level cannot be negative");
        }
        if (reorderLevel.scale() > 3) {
            throw new BusinessRuleException("Invalid reorder level precision");
        }
    }

    private StockLocation findLocation(String locationCode) {
        return stockLocationRepository.findById(locationCode)
                .orElseThrow(() -> new ResourceNotFoundException("Stock location not found"));
    }

    private InventoryItem findInventoryItem(Long inventoryItemId) {
        return inventoryItemRepository.findById(inventoryItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item not found"));
    }

    private StockReorderLevelResponse toResponse(StockReorderLevel config) {
        BigDecimal currentQuantity = stockBalanceRepository
                .findByStockLocation_LocationCodeAndInventoryItem_Id(
                        config.getStockLocation().getLocationCode(),
                        config.getInventoryItem().getId()
                )
                .map(balance -> balance.getQuantityOnHand())
                .orElse(BigDecimal.ZERO);
        return StockReorderLevelResponse.from(config, currentQuantity);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AuthBranchData(Long id, String branchCode, boolean active) {
    }
}
