package com.rasahub.product_inventory_service.service;

import com.rasahub.product_inventory_service.dto.stockcount.StockCountCreateRequest;
import com.rasahub.product_inventory_service.dto.stockcount.StockCountRejectRequest;
import com.rasahub.product_inventory_service.dto.stockcount.StockCountResponse;
import com.rasahub.product_inventory_service.dto.stockcount.StockCountUpdateRequest;
import com.rasahub.product_inventory_service.enums.StockCountStatus;
import org.springframework.data.domain.Page;

import java.time.LocalDate;

public interface StockCountService {

    StockCountResponse createStockCount(StockCountCreateRequest request);

    StockCountResponse updateDraftStockCount(
            Long stockCountId,
            StockCountUpdateRequest request
    );

    StockCountResponse submitStockCount(Long stockCountId);

    StockCountResponse approveStockCount(Long stockCountId);

    StockCountResponse rejectStockCount(
            Long stockCountId,
            StockCountRejectRequest request
    );

    StockCountResponse reconcileStockCount(Long stockCountId);

    StockCountResponse getStockCountById(Long stockCountId);

    Page<StockCountResponse> getAllStockCounts(int page, int size);

    Page<StockCountResponse> getMyBranchStockCounts(int page, int size);

    Page<StockCountResponse> getStockCountsByLocation(
            String locationCode,
            int page,
            int size
    );

    Page<StockCountResponse> getStockCountsByStatus(
            StockCountStatus status,
            int page,
            int size
    );

    Page<StockCountResponse> getStockCountsByDateRange(
            LocalDate startDate,
            LocalDate endDate,
            String locationCode,
            StockCountStatus status,
            int page,
            int size
    );
}
