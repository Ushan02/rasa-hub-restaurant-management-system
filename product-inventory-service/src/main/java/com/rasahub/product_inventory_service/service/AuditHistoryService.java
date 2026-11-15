package com.rasahub.product_inventory_service.service;

import com.rasahub.product_inventory_service.dto.audit.AuditLogResponse;
import com.rasahub.product_inventory_service.dto.report.PageResponse;
import com.rasahub.product_inventory_service.exception.BusinessRuleException;
import com.rasahub.product_inventory_service.repository.AuditLogRepository;
import com.rasahub.product_inventory_service.service.impl.InventoryReportAccessService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@Transactional(readOnly = true)
public class AuditHistoryService {

    private static final Pattern FILTER_TOKEN =
            Pattern.compile("[A-Z][A-Z_]{0,39}");

    private final AuditLogRepository auditLogRepository;
    private final InventoryReportAccessService accessService;

    public AuditHistoryService(
            AuditLogRepository auditLogRepository,
            InventoryReportAccessService accessService
    ) {
        this.auditLogRepository = auditLogRepository;
        this.accessService = accessService;
    }

    public PageResponse<AuditLogResponse> search(
            String entityType,
            Long entityId,
            Long actorAccountId,
            String action,
            String locationCode,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size
    ) {
        if (entityId != null && entityId < 1
                || actorAccountId != null && actorAccountId < 1) {
            throw new BusinessRuleException(
                    "Entity and actor IDs must be positive"
            );
        }
        if (page < 0 || size < 1 || size > 100) {
            throw new BusinessRuleException(
                    "Page must be non-negative and size must be between 1 and 100"
            );
        }
        validateDateRange(startDate, endDate);
        String normalizedType = normalizeToken(entityType, "entityType");
        String normalizedAction = normalizeToken(action, "action");
        String scopedLocation = accessService.resolveLocation(locationCode);

        return PageResponse.from(auditLogRepository.findReportRows(
                normalizedType,
                entityId,
                actorAccountId,
                normalizedAction,
                scopedLocation,
                startDate == null ? null : startDate.atStartOfDay(),
                exclusiveEnd(endDate),
                PageRequest.of(page, size)
        ));
    }

    private String normalizeToken(String value, String field) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!FILTER_TOKEN.matcher(normalized).matches()) {
            throw new BusinessRuleException("Invalid " + field);
        }
        return normalized;
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null
                && endDate.isBefore(startDate)) {
            throw new BusinessRuleException(
                    "endDate must not be before startDate"
            );
        }
        if (endDate != null && endDate.equals(LocalDate.MAX)) {
            throw new BusinessRuleException(
                    "endDate is outside the supported range"
            );
        }
    }

    private LocalDateTime exclusiveEnd(LocalDate endDate) {
        return endDate == null ? null : endDate.plusDays(1).atStartOfDay();
    }
}
