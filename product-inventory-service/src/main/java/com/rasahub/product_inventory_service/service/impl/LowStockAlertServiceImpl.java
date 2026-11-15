package com.rasahub.product_inventory_service.service.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.rasahub.product_inventory_service.dto.lowstock.LowStockAlertResponse;
import com.rasahub.product_inventory_service.dto.lowstock.LowStockSummaryResponse;
import com.rasahub.product_inventory_service.entity.InventoryItem;
import com.rasahub.product_inventory_service.entity.LowStockAlert;
import com.rasahub.product_inventory_service.entity.StockBalance;
import com.rasahub.product_inventory_service.entity.StockLocation;
import com.rasahub.product_inventory_service.entity.StockReorderLevel;
import com.rasahub.product_inventory_service.enums.AlertSeverity;
import com.rasahub.product_inventory_service.enums.AlertStatus;
import com.rasahub.product_inventory_service.enums.StockStatus;
import com.rasahub.product_inventory_service.exception.BusinessRuleException;
import com.rasahub.product_inventory_service.exception.ConflictException;
import com.rasahub.product_inventory_service.exception.ResourceNotFoundException;
import com.rasahub.product_inventory_service.repository.InventoryItemRepository;
import com.rasahub.product_inventory_service.repository.LowStockAlertRepository;
import com.rasahub.product_inventory_service.repository.StockBalanceRepository;
import com.rasahub.product_inventory_service.repository.StockLocationRepository;
import com.rasahub.product_inventory_service.repository.StockReorderLevelRepository;
import com.rasahub.product_inventory_service.security.ProductUserPrincipal;
import com.rasahub.product_inventory_service.service.AuditLogService;
import com.rasahub.product_inventory_service.service.LowStockAlertEvaluationService;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class LowStockAlertServiceImpl implements LowStockAlertEvaluationService {

    private static final Set<String> BRANCH_CODES = Set.of("BR001", "BR002", "BR003");
    private static final String MAIN_LOCATION = "Main";

    private final StockReorderLevelRepository stockReorderLevelRepository;
    private final LowStockAlertRepository lowStockAlertRepository;
    private final StockBalanceRepository stockBalanceRepository;
    private final StockLocationRepository stockLocationRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final HttpServletRequest servletRequest;
    private final RestClient authClient;

    @Autowired
    private AuditLogService auditLogService;

    public LowStockAlertServiceImpl(
            StockReorderLevelRepository stockReorderLevelRepository,
            LowStockAlertRepository lowStockAlertRepository,
            StockBalanceRepository stockBalanceRepository,
            StockLocationRepository stockLocationRepository,
            InventoryItemRepository inventoryItemRepository
    ) {
        this(
                stockReorderLevelRepository,
                lowStockAlertRepository,
                stockBalanceRepository,
                stockLocationRepository,
                inventoryItemRepository,
                null,
                "http://localhost"
        );
    }

    @Autowired
    public LowStockAlertServiceImpl(
            StockReorderLevelRepository stockReorderLevelRepository,
            LowStockAlertRepository lowStockAlertRepository,
            StockBalanceRepository stockBalanceRepository,
            StockLocationRepository stockLocationRepository,
            InventoryItemRepository inventoryItemRepository,
            HttpServletRequest servletRequest,
            @Value("${branch.sync.auth-base-url}") String authBaseUrl
    ) {
        this.stockReorderLevelRepository = stockReorderLevelRepository;
        this.lowStockAlertRepository = lowStockAlertRepository;
        this.stockBalanceRepository = stockBalanceRepository;
        this.stockLocationRepository = stockLocationRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.servletRequest = servletRequest;
        this.authClient = RestClient.builder().baseUrl(authBaseUrl).build();
    }

    @Override
    public List<LowStockAlertResponse> getAlertsByLocation(String locationCode) {
        validateLocation(locationCode);
        ProductUserPrincipal user = currentUser();
        requireReadableRole(user);
        requireReadLocation(user, locationCode);
        return lowStockAlertRepository.findByStockLocation_LocationCodeOrderByDetectedAtDesc(locationCode)
                .stream()
                .map(LowStockAlertResponse::from)
                .toList();
    }

    @Override
    public List<LowStockAlertResponse> getAlertsByItem(Long inventoryItemId) {
        validateInventoryItem(inventoryItemId);
        ProductUserPrincipal user = currentUser();
        requireReadableRole(user);
        List<LowStockAlert> alerts = lowStockAlertRepository.findByInventoryItem_IdOrderByDetectedAtDesc(inventoryItemId);
        String assignedBranch = "BRANCH_MANAGER".equals(user.getRole())
                ? assignedBranchCode(user)
                : null;
        alerts = alerts.stream()
                .filter(alert -> canReadLocation(user, assignedBranch, alert.getStockLocation().getLocationCode()))
                .toList();
        return alerts.stream().map(LowStockAlertResponse::from).toList();
    }

    @Override
    @Transactional
    public LowStockAlertResponse acknowledgeAlert(Long alertId, Long accountId, String remarks) {
        if (alertId == null || alertId < 1) {
            throw new BusinessRuleException("Valid alert ID is required");
        }

        ProductUserPrincipal user = currentUser();
        requireAnyRole(user, "OWNER", "MAIN_OFFICE_MANAGER", "STOCK_KEEPER", "BRANCH_MANAGER");
        Long resolvedAccountId = resolveActorAccountId(user, accountId);

        LowStockAlert alert = lowStockAlertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("Low stock alert not found"));

        requireReadLocation(user, alert.getStockLocation().getLocationCode());

        AlertStatus previousStatus = alert.getStatus();
        try {
            alert.acknowledge(resolvedAccountId, remarks);
        } catch (IllegalStateException | IllegalArgumentException exception) {
            throw new ConflictException(exception.getMessage());
        }
        LowStockAlert saved = lowStockAlertRepository.saveAndFlush(alert);
        auditLogService.record(
                "LOW_STOCK_ALERT",
                saved.getId(),
                "ACKNOWLEDGED",
                resolvedAccountId,
                previousStatus.name(),
                saved.getStatus().name(),
                remarks,
                null,
                saved.getStockLocation().getLocationCode()
        );
        return LowStockAlertResponse.from(saved);
    }

    @Override
    @Transactional
    public LowStockAlertResponse resolveAlert(Long alertId, String remarks) {
        if (alertId == null || alertId < 1) {
            throw new BusinessRuleException("Valid alert ID is required");
        }

        ProductUserPrincipal user = currentUser();
        requireAnyRole(user, "OWNER", "MAIN_OFFICE_MANAGER", "STOCK_KEEPER", "BRANCH_MANAGER");
        LowStockAlert alert = lowStockAlertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("Low stock alert not found"));

        requireReadLocation(user, alert.getStockLocation().getLocationCode());

        StockReorderLevel config = stockReorderLevelRepository
                .findForUpdate(
                        alert.getStockLocation().getLocationCode(),
                        alert.getInventoryItem().getId()
                )
                .filter(StockReorderLevel::isActive)
                .orElseThrow(() -> new ConflictException(
                        "Alert cannot be resolved manually while monitoring is disabled"
                ));
        BigDecimal quantity = stockBalanceRepository
                .findByStockLocation_LocationCodeAndInventoryItem_Id(
                        config.getStockLocation().getLocationCode(),
                        config.getInventoryItem().getId()
                )
                .map(StockBalance::getQuantityOnHand)
                .orElse(BigDecimal.ZERO);
        if (StockStatus.fromQuantity(quantity, config.getReorderLevel()) != StockStatus.NORMAL) {
            throw new ConflictException("An active stock shortage cannot be manually resolved");
        }
        AlertStatus previousStatus = alert.getStatus();
        try {
            alert.resolveAsRecovered(remarks);
        } catch (IllegalStateException exception) {
            throw new ConflictException(exception.getMessage());
        }
        LowStockAlert saved = lowStockAlertRepository.saveAndFlush(alert);
        auditLogService.record(
                "LOW_STOCK_ALERT",
                saved.getId(),
                "RECOVERED",
                user.getAccountId(),
                previousStatus.name(),
                saved.getStatus().name(),
                remarks,
                null,
                saved.getStockLocation().getLocationCode()
        );
        return LowStockAlertResponse.from(saved);
    }

    @Override
    @Transactional
    public LowStockAlert evaluateItem(String locationCode, Long inventoryItemId) {
        validateLocation(locationCode);
        validateInventoryItem(inventoryItemId);

        StockReorderLevel config = stockReorderLevelRepository
                .findForUpdate(locationCode, inventoryItemId)
                .orElse(null);

        if (config == null || !config.isActive()) {
            return null;
        }

        StockBalance balance = stockBalanceRepository
                .findByStockLocation_LocationCodeAndInventoryItem_Id(locationCode, inventoryItemId)
                .orElseGet(() -> new StockBalance(findLocation(locationCode), findInventoryItem(inventoryItemId)));

        BigDecimal currentQuantity = balance.getQuantityOnHand();
        StockStatus status = StockStatus.fromQuantity(currentQuantity, config.getReorderLevel());

        List<LowStockAlert> activeAlerts = lowStockAlertRepository.findActiveForUpdate(
                locationCode,
                inventoryItemId,
                List.of(AlertStatus.OPEN, AlertStatus.ACKNOWLEDGED)
        );

        if (status == StockStatus.NORMAL) {
            if (!activeAlerts.isEmpty()) {
                LowStockAlert resolvedAlert = activeAlerts.getFirst();
                for (LowStockAlert activeAlert : activeAlerts) {
                    AlertStatus previousStatus = activeAlert.getStatus();
                    activeAlert.resolveAsRecovered("Stock returned to normal");
                    auditLogService.record(
                            "LOW_STOCK_ALERT",
                            activeAlert.getId(),
                            "RECOVERED",
                            authenticatedAccountIdOrNull(),
                            previousStatus.name(),
                            activeAlert.getStatus().name(),
                            "Stock returned to normal",
                            null,
                            locationCode
                    );
                }
                lowStockAlertRepository.saveAllAndFlush(activeAlerts);
                return resolvedAlert;
            }
            return null;
        }

        AlertSeverity severity = status == StockStatus.OUT_OF_STOCK
                ? AlertSeverity.OUT_OF_STOCK
                : AlertSeverity.LOW_STOCK;

        if (activeAlerts.isEmpty()) {
            LowStockAlert alert = new LowStockAlert(
                    config.getStockLocation(),
                    config.getInventoryItem(),
                    config.getReorderLevel(),
                    currentQuantity,
                    severity
            );
            alert.updateEvaluation(
                    config.getReorderLevel(),
                    currentQuantity,
                    severity,
                    LocalDateTime.now(),
                    "System-evaluated alert"
            );
            LowStockAlert saved = lowStockAlertRepository.saveAndFlush(alert);
            auditLogService.record(
                    "LOW_STOCK_ALERT",
                    saved.getId(),
                    "CREATED",
                    authenticatedAccountIdOrNull(),
                    null,
                    saved.getStatus().name(),
                    "System-evaluated alert",
                    null,
                    locationCode
            );
            return saved;
        }

        LowStockAlert activeAlert = activeAlerts.getFirst();
        if (activeAlert.getSeverity() != severity) {
            AlertSeverity previousSeverity = activeAlert.getSeverity();
            activeAlert.updateEvaluation(
                    config.getReorderLevel(),
                    currentQuantity,
                    severity,
                    LocalDateTime.now(),
                    "Severity updated due to stock movement"
            );
            LowStockAlert saved = lowStockAlertRepository.saveAndFlush(activeAlert);
            auditLogService.record(
                    "LOW_STOCK_ALERT",
                    saved.getId(),
                    "SEVERITY_CHANGED",
                    authenticatedAccountIdOrNull(),
                    previousSeverity.name(),
                    severity.name(),
                    "Severity updated due to stock movement",
                    null,
                    locationCode
            );
            return saved;
        }

        activeAlert.updateEvaluation(
                config.getReorderLevel(),
                currentQuantity,
                severity,
                LocalDateTime.now(),
                activeAlert.getRemarks()
        );
        return lowStockAlertRepository.saveAndFlush(activeAlert);
    }

    @Override
    @Transactional
    public List<LowStockAlert> evaluateLocation(String locationCode) {
        validateLocation(locationCode);

        List<StockReorderLevel> configs = stockReorderLevelRepository
                .findByStockLocation_LocationCodeAndActiveTrue(locationCode);

        return configs.stream()
                .map(config -> evaluateItem(config.getStockLocation().getLocationCode(), config.getInventoryItem().getId()))
                .filter(alert -> alert != null)
                .toList();
    }

    @Override
    @Transactional
    public LowStockAlert evaluateItemAuthorized(String locationCode, Long inventoryItemId) {
        ProductUserPrincipal user = currentUser();
        requireAnyRole(user, "OWNER", "MAIN_OFFICE_MANAGER", "STOCK_KEEPER");
        requireReadLocation(user, locationCode);
        return evaluateItem(locationCode, inventoryItemId);
    }

    @Override
    @Transactional
    public List<LowStockAlert> evaluateLocationAuthorized(String locationCode) {
        ProductUserPrincipal user = currentUser();
        requireAnyRole(user, "OWNER", "MAIN_OFFICE_MANAGER", "STOCK_KEEPER");
        requireReadLocation(user, locationCode);
        return evaluateLocation(locationCode);
    }

    @Override
    @Transactional
    public void closeForMonitoringDisabled(
            String locationCode,
            Long inventoryItemId,
            Long actorAccountId
    ) {
        List<LowStockAlert> activeAlerts = lowStockAlertRepository.findActiveForUpdate(
                locationCode,
                inventoryItemId,
                List.of(AlertStatus.OPEN, AlertStatus.ACKNOWLEDGED)
        );
        if (activeAlerts.isEmpty()) {
            return;
        }
        for (LowStockAlert alert : activeAlerts) {
            AlertStatus previousStatus = alert.getStatus();
            alert.closeMonitoringDisabled();
            auditLogService.record(
                    "LOW_STOCK_ALERT",
                    alert.getId(),
                    "MONITORING_DISABLED",
                    actorAccountId,
                    previousStatus.name(),
                    alert.getStatus().name(),
                    "Monitoring disabled for this item and location",
                    null,
                    locationCode
            );
        }
        lowStockAlertRepository.saveAllAndFlush(activeAlerts);
    }

    private Long authenticatedAccountIdOrNull() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal()
                instanceof ProductUserPrincipal principal)) {
            return null;
        }
        return principal.getAccountId();
    }

    @Override
    public LowStockSummaryResponse getSummaryByLocation(String locationCode) {
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
                        StockBalance::getQuantityOnHand,
                        (first, ignored) -> first
                ));
        long lowStock = 0;
        long outOfStock = 0;
        long normal = 0;
        for (StockReorderLevel config : configurations) {
            StockStatus status = StockStatus.fromQuantity(
                    quantities.getOrDefault(config.getInventoryItem().getId(), BigDecimal.ZERO),
                    config.getReorderLevel()
            );
            if (status == StockStatus.OUT_OF_STOCK) {
                outOfStock++;
            } else if (status == StockStatus.LOW_STOCK) {
                lowStock++;
            } else {
                normal++;
            }
        }
        long activeAlertCount = lowStockAlertRepository.countByStockLocation_LocationCodeAndStatusIn(
                locationCode,
                List.of(AlertStatus.OPEN, AlertStatus.ACKNOWLEDGED)
        );
        return new LowStockSummaryResponse(
                locationCode,
                configurations.size(),
                normal,
                lowStock,
                outOfStock,
                activeAlertCount
        );
    }

    private ProductUserPrincipal currentUserOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof ProductUserPrincipal user)
                || user.getAccountId() == null
                || user.getAccountId() <= 0) {
            return null;
        }
        return user;
    }

    private ProductUserPrincipal currentUser() {
        ProductUserPrincipal user = currentUserOrNull();
        if (user == null) {
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

    private void requireReadableRole(ProductUserPrincipal user) {
        requireAnyRole(user, "OWNER", "MAIN_OFFICE_MANAGER", "ACCOUNTANT", "STOCK_KEEPER", "BRANCH_MANAGER");
    }

    private void requireAnyRole(ProductUserPrincipal user, String... allowedRoles) {
        for (String role : allowedRoles) {
            if (role.equals(user.getRole())) {
                return;
            }
        }
        throw new AccessDeniedException("You do not have permission for this operation");
    }

    private void requireReadLocation(ProductUserPrincipal user, String locationCode) {
        if ("BRANCH_MANAGER".equals(user.getRole())) {
            if (!BRANCH_CODES.contains(locationCode) || !assignedBranchCode(user).equals(locationCode)) {
                throw new AccessDeniedException("You can only access alerts for your assigned branch");
            }
            return;
        }
        if ("STOCK_KEEPER".equals(user.getRole()) && !MAIN_LOCATION.equals(locationCode)) {
            throw new AccessDeniedException("Stock Keeper can only access Main warehouse alerts");
        }
    }

    private boolean canReadLocation(
            ProductUserPrincipal user,
            String assignedBranch,
            String locationCode
    ) {
        if ("BRANCH_MANAGER".equals(user.getRole())) {
            return BRANCH_CODES.contains(locationCode) && assignedBranch.equals(locationCode);
        }
        return !"STOCK_KEEPER".equals(user.getRole()) || MAIN_LOCATION.equals(locationCode);
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

    private StockLocation findLocation(String locationCode) {
        return stockLocationRepository.findById(locationCode)
                .orElseThrow(() -> new ResourceNotFoundException("Stock location not found"));
    }

    private InventoryItem findInventoryItem(Long inventoryItemId) {
        return inventoryItemRepository.findById(inventoryItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item not found"));
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AuthBranchData(Long id, String branchCode, boolean active) {
    }
}
