package com.rasahub.product_inventory_service.service;

import com.rasahub.product_inventory_service.dto.stockadjustment.StockAdjustmentCreateRequest;
import com.rasahub.product_inventory_service.dto.stockadjustment.StockAdjustmentRejectRequest;
import com.rasahub.product_inventory_service.dto.stockadjustment.StockAdjustmentResponse;
import com.rasahub.product_inventory_service.dto.stockadjustment.StockAdjustmentUpdateRequest;
import com.rasahub.product_inventory_service.enums.StockAdjustmentStatus;
import org.springframework.data.domain.Page;

import java.time.LocalDate;

public interface StockAdjustmentService {

    StockAdjustmentResponse createStockAdjustment(
            StockAdjustmentCreateRequest request
    );

    StockAdjustmentResponse updateDraftStockAdjustment(
            Long adjustmentId,
            StockAdjustmentUpdateRequest request
    );

    StockAdjustmentResponse submitStockAdjustment(Long adjustmentId);

    StockAdjustmentResponse approveStockAdjustment(Long adjustmentId);

    StockAdjustmentResponse rejectStockAdjustment(
            Long adjustmentId,
            StockAdjustmentRejectRequest request
    );

    StockAdjustmentResponse postStockAdjustment(Long adjustmentId);

    StockAdjustmentResponse getStockAdjustmentById(Long adjustmentId);

    Page<StockAdjustmentResponse> getAllStockAdjustments(int page, int size);

    Page<StockAdjustmentResponse> getMyStockAdjustments(int page, int size);

    Page<StockAdjustmentResponse> getStockAdjustmentsByLocation(
            String locationCode,
            int page,
            int size
    );

    Page<StockAdjustmentResponse> getStockAdjustmentsByStatus(
            StockAdjustmentStatus status,
            int page,
            int size
    );

    Page<StockAdjustmentResponse> getStockAdjustmentsByDateRange(
            LocalDate startDate,
            LocalDate endDate,
            String locationCode,
            StockAdjustmentStatus status,
            int page,
            int size
    );
}
