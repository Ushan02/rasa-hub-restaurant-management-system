
package com.rasahub.product_inventory_service.service;

import com.rasahub.product_inventory_service.dto.stocktransfer.StockTransferCancelRequest;
import com.rasahub.product_inventory_service.dto.stocktransfer.StockTransferCreateRequest;
import com.rasahub.product_inventory_service.dto.stocktransfer.StockTransferResponse;
import com.rasahub.product_inventory_service.enums.StockTransferStatus;
import org.springframework.data.domain.Page;

public interface StockTransferService {

    StockTransferResponse createTransfer(
            StockTransferCreateRequest request
    );

    StockTransferResponse getTransferById(
            Long transferId
    );

    Page<StockTransferResponse> getAllTransfers(
            int page,
            int size
    );

    Page<StockTransferResponse> getTransfersByStatus(
            StockTransferStatus status,
            int page,
            int size
    );

    Page<StockTransferResponse> getTransfersByBranch(
            String branchCode,
            int page,
            int size
    );

    Page<StockTransferResponse> getTransfersByBranchAndStatus(
            String branchCode,
            StockTransferStatus status,
            int page,
            int size
    );

    Page<StockTransferResponse> getMyBranchTransfers(
            int page,
            int size
    );

    StockTransferResponse dispatchTransfer(
            Long transferId
    );

    StockTransferResponse confirmReceipt(
            Long transferId
    );

    StockTransferResponse cancelTransfer(
            Long transferId,
            StockTransferCancelRequest request
    );
}
