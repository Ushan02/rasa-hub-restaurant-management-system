
package com.rasahub.product_inventory_service.service;

import com.rasahub.product_inventory_service.dto.branchstockrequest.BranchStockRequestCreateRequest;
import com.rasahub.product_inventory_service.dto.branchstockrequest.BranchStockRequestRejectRequest;
import com.rasahub.product_inventory_service.dto.branchstockrequest.BranchStockRequestResponse;

import com.rasahub.product_inventory_service.enums.BranchStockRequestStatus;

import org.springframework.data.domain.Page;

public interface BranchStockRequestService {

    BranchStockRequestResponse createBranchStockRequest(
            BranchStockRequestCreateRequest request
    );

    BranchStockRequestResponse getBranchStockRequestById(
            Long branchStockRequestId
    );

    Page<BranchStockRequestResponse> getAllBranchStockRequests(
            int page,
            int size
    );

    Page<BranchStockRequestResponse> getBranchStockRequestsByStatus(
            BranchStockRequestStatus status,
            int page,
            int size
    );

    Page<BranchStockRequestResponse> getBranchStockRequestsByBranch(
            String branchLocationCode,
            int page,
            int size
    );

    Page<BranchStockRequestResponse>
    getBranchStockRequestsByBranchAndStatus(
            String branchLocationCode,
            BranchStockRequestStatus status,
            int page,
            int size
    );

    Page<BranchStockRequestResponse> getMyBranchStockRequests(
            int page,
            int size
    );

    BranchStockRequestResponse approveBranchStockRequest(
            Long branchStockRequestId
    );

    BranchStockRequestResponse rejectBranchStockRequest(
            Long branchStockRequestId,
            BranchStockRequestRejectRequest request
    );
}
