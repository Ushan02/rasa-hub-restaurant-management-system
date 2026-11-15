package com.rasahub.product_inventory_service.report;

import com.rasahub.product_inventory_service.repository.StockLocationRepository;
import com.rasahub.product_inventory_service.security.ProductUserPrincipal;
import com.rasahub.product_inventory_service.service.impl.InventoryReportAccessService;
import com.sun.net.httpserver.HttpServer;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryReportAccessServiceTest {

    @Mock
    private StockLocationRepository locationRepository;

    @Mock
    private HttpServletRequest servletRequest;

    private HttpServer branchServer;
    private InventoryReportAccessService accessService;

    @BeforeEach
    void setUp() throws IOException {
        branchServer = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0),
                0
        );
        branchServer.createContext("/api/v1/branches/7", exchange -> {
            byte[] body = """
                    {"id":7,"branchCode":"BR001","active":true}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set(
                    "Content-Type",
                    "application/json"
            );
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        branchServer.start();
        accessService = new InventoryReportAccessService(
                locationRepository,
                servletRequest,
                "http://127.0.0.1:" + branchServer.getAddress().getPort()
        );
        when(servletRequest.getHeader(HttpHeaders.AUTHORIZATION))
                .thenReturn("Bearer test-token");
        when(locationRepository.existsById("BR001")).thenReturn(true);
        when(locationRepository.existsById("BR002")).thenReturn(true);
        authenticate("BRANCH_MANAGER", 7L);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        branchServer.stop(0);
    }

    @Test
    void branchManagerIsScopedToVerifiedAssignedBranch() {
        assertEquals("BR001", accessService.resolveLocation(null));
        assertEquals("BR001", accessService.resolveLocation("BR001"));
        assertThrows(
                AccessDeniedException.class,
                () -> accessService.resolveLocation("BR002")
        );
    }

    private void authenticate(String role, Long branchId) {
        ProductUserPrincipal principal =
                new ProductUserPrincipal(91L, role, branchId);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.getAuthorities()
                )
        );
    }
}
