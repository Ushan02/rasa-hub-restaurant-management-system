package com.rasahub.product_inventory_service.service.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.rasahub.product_inventory_service.exception.ResourceNotFoundException;
import com.rasahub.product_inventory_service.entity.StockLocation;
import com.rasahub.product_inventory_service.repository.StockLocationRepository;
import com.rasahub.product_inventory_service.security.ProductUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.Set;
import java.util.List;

@Service
public class InventoryReportAccessService {

    private static final String MAIN_LOCATION = "Main";
    private static final Set<String> READ_ROLES = Set.of(
            "OWNER",
            "MAIN_OFFICE_MANAGER",
            "STOCK_KEEPER",
            "ACCOUNTANT",
            "BRANCH_MANAGER"
    );
    private static final Set<String> BRANCH_CODES =
            Set.of("BR001", "BR002", "BR003");

    private final StockLocationRepository locationRepository;
    private final HttpServletRequest servletRequest;
    private final RestClient authClient;

    public InventoryReportAccessService(
            StockLocationRepository locationRepository,
            HttpServletRequest servletRequest,
            @Value("${branch.sync.auth-base-url}") String authBaseUrl
    ) {
        this.locationRepository = locationRepository;
        this.servletRequest = servletRequest;
        SimpleClientHttpRequestFactory requestFactory =
                new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        this.authClient = RestClient.builder()
                .baseUrl(authBaseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public String resolveLocation(String requestedLocationCode) {
        ProductUserPrincipal user = currentUser();
        if (!READ_ROLES.contains(user.getRole())) {
            throw new AccessDeniedException(
                    "You do not have permission to view inventory reports"
            );
        }

        String requested = requestedLocationCode == null
                || requestedLocationCode.isBlank()
                ? null
                : requestedLocationCode.trim();
        if (requested != null && !locationRepository.existsById(requested)) {
            throw new ResourceNotFoundException("Stock location not found");
        }

        if ("BRANCH_MANAGER".equals(user.getRole())) {
            String assigned = assignedBranchCode(user);
            if (requested != null && !requested.equals(assigned)) {
                throw new AccessDeniedException(
                        "Branch managers can only view their assigned branch"
                );
            }
            return assigned;
        }

        if ("STOCK_KEEPER".equals(user.getRole())) {
            if (requested != null && !MAIN_LOCATION.equals(requested)) {
                throw new AccessDeniedException(
                        "Stock Keepers can only view the Main warehouse"
                );
            }
            return MAIN_LOCATION;
        }

        return requested;
    }

    public List<StockLocation> resolveLocations(String requestedLocationCode) {
        String resolved = resolveLocation(requestedLocationCode);
        if (resolved != null) {
            return locationRepository.findById(resolved)
                    .filter(StockLocation::isActive)
                    .map(List::of)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Active stock location not found"
                    ));
        }
        return locationRepository.findByActiveOrderByLocationCodeAsc(true)
                .stream()
                .filter(location -> Set.of(
                        "Main", "BR001", "BR002", "BR003"
                ).contains(location.getLocationCode()))
                .toList();
    }

    public ProductUserPrincipal currentUser() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
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

    private String assignedBranchCode(ProductUserPrincipal user) {
        if (user.getBranchId() == null || user.getBranchId() <= 0
                || servletRequest == null) {
            throw new AccessDeniedException(
                    "A valid branch assignment is required"
            );
        }
        String bearer = servletRequest.getHeader(HttpHeaders.AUTHORIZATION);
        if (bearer == null || !bearer.startsWith("Bearer ")) {
            throw new AccessDeniedException("A valid Bearer token is required");
        }
        try {
            AuthBranchData branch = authClient.get()
                    .uri("/api/v1/branches/{id}", user.getBranchId())
                    .header(HttpHeaders.AUTHORIZATION, bearer)
                    .retrieve()
                    .body(AuthBranchData.class);
            if (branch == null
                    || !user.getBranchId().equals(branch.id())
                    || !branch.active()
                    || !BRANCH_CODES.contains(branch.branchCode())
                    || !locationRepository.existsById(branch.branchCode())) {
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AuthBranchData(
            Long id,
            String branchCode,
            boolean active
    ) {
    }
}
