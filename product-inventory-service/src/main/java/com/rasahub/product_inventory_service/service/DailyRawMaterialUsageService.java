package com.rasahub.product_inventory_service.service;

import com.rasahub.product_inventory_service.dto.dailyrawmaterialusage.DailyRawMaterialUsageCreateRequest;
import com.rasahub.product_inventory_service.dto.dailyrawmaterialusage.DailyRawMaterialUsageResponse;
import com.rasahub.product_inventory_service.dto.dailyrawmaterialusage.DailyRawMaterialUsageUpdateRequest;
import com.rasahub.product_inventory_service.enums.DailyRawMaterialUsageStatus;

import org.springframework.data.domain.Page;

import java.time.LocalDate;

public interface DailyRawMaterialUsageService {

    DailyRawMaterialUsageResponse createDailyUsage(
            DailyRawMaterialUsageCreateRequest request
    );

    DailyRawMaterialUsageResponse updateDraftUsage(
            Long usageId,
            DailyRawMaterialUsageUpdateRequest request
    );

    DailyRawMaterialUsageResponse postDailyUsage(Long usageId);

    DailyRawMaterialUsageResponse getDailyUsageById(Long usageId);

    Page<DailyRawMaterialUsageResponse> getAllDailyUsages(
            int page,
            int size
    );

    Page<DailyRawMaterialUsageResponse> getMyBranchDailyUsages(
            int page,
            int size
    );

    Page<DailyRawMaterialUsageResponse> getDailyUsagesByBranch(
            String branchLocationCode,
            int page,
            int size
    );

    Page<DailyRawMaterialUsageResponse> getDailyUsagesByStatus(
            DailyRawMaterialUsageStatus status,
            int page,
            int size
    );

    Page<DailyRawMaterialUsageResponse> getDailyUsagesByDateRange(
            String branchLocationCode,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size
    );
}
