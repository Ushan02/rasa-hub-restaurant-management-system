package com.rasahub.product_inventory_service.controller;

import com.rasahub.product_inventory_service.dto.audit.AuditLogResponse;
import com.rasahub.product_inventory_service.dto.report.PageResponse;
import com.rasahub.product_inventory_service.service.AuditHistoryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/audit-history")
@PreAuthorize("hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', 'STOCK_KEEPER', 'ACCOUNTANT', 'BRANCH_MANAGER')")
public class AuditHistoryController {

    private final AuditHistoryService auditHistoryService;

    public AuditHistoryController(AuditHistoryService auditHistoryService) {
        this.auditHistoryService = auditHistoryService;
    }

    @GetMapping
    public PageResponse<AuditLogResponse> search(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Long entityId,
            @RequestParam(required = false) Long actorAccountId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String locationCode,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return auditHistoryService.search(
                entityType,
                entityId,
                actorAccountId,
                action,
                locationCode,
                startDate,
                endDate,
                page,
                size
        );
    }

    @GetMapping("/entity/{type}/{id}")
    public PageResponse<AuditLogResponse> byEntity(
            @PathVariable String type,
            @PathVariable Long id,
            @RequestParam(required = false) String locationCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return auditHistoryService.search(
                type,
                id,
                null,
                null,
                locationCode,
                null,
                null,
                page,
                size
        );
    }

    @GetMapping("/account/{accountId}")
    public PageResponse<AuditLogResponse> byAccount(
            @PathVariable Long accountId,
            @RequestParam(required = false) String locationCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return auditHistoryService.search(
                null,
                null,
                accountId,
                null,
                locationCode,
                null,
                null,
                page,
                size
        );
    }
}
