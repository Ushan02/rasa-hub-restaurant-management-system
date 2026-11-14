
package com.rasahub.product_inventory_service.dto.stocktransfer;

import com.rasahub.product_inventory_service.entity.StockTransfer;
import com.rasahub.product_inventory_service.enums.StockTransferStatus;
import java.time.LocalDateTime;
import java.util.List;

public class StockTransferResponse {

    private final Long id;
    private final Long branchStockRequestId;
    private final String sourceLocationCode;
    private final String destinationLocationCode;
    private final String destinationLocationName;
    private final Long createdByAccountId;
    private final StockTransferStatus status;
    private final Long dispatchedByAccountId;
    private final LocalDateTime dispatchedAt;
    private final Long receivedByAccountId;
    private final LocalDateTime receivedAt;
    private final Long cancelledByAccountId;
    private final LocalDateTime cancelledAt;
    private final String cancellationReason;
    private final String remarks;
    private final List<StockTransferItemResponse> items;
    private final Long version;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public StockTransferResponse(StockTransfer transfer) {
        id = transfer.getId();

        branchStockRequestId =
                transfer.getBranchStockRequest().getId();

        sourceLocationCode =
                transfer.getSourceLocation().getLocationCode();

        destinationLocationCode =
                transfer.getDestinationLocation().getLocationCode();

        destinationLocationName =
                transfer.getDestinationLocation().getLocationName();

        createdByAccountId = transfer.getCreatedByAccountId();
        status = transfer.getStatus();

        dispatchedByAccountId =
                transfer.getDispatchedByAccountId();

        dispatchedAt = transfer.getDispatchedAt();

        receivedByAccountId =
                transfer.getReceivedByAccountId();

        receivedAt = transfer.getReceivedAt();

        cancelledByAccountId =
                transfer.getCancelledByAccountId();

        cancelledAt = transfer.getCancelledAt();

        cancellationReason = transfer.getCancellationReason();
        remarks = transfer.getRemarks();

        items = transfer.getItems().stream()
                .map(StockTransferItemResponse::new)
                .toList();

        version = transfer.getVersion();
        createdAt = transfer.getCreatedAt();
        updatedAt = transfer.getUpdatedAt();
    }

    public Long getId() { return id; }
    public Long getBranchStockRequestId() { return branchStockRequestId; }
    public String getSourceLocationCode() { return sourceLocationCode; }
    public String getDestinationLocationCode() { return destinationLocationCode; }
    public String getDestinationLocationName() { return destinationLocationName; }
    public Long getCreatedByAccountId() { return createdByAccountId; }
    public StockTransferStatus getStatus() { return status; }
    public Long getDispatchedByAccountId() { return dispatchedByAccountId; }
    public LocalDateTime getDispatchedAt() { return dispatchedAt; }
    public Long getReceivedByAccountId() { return receivedByAccountId; }
    public LocalDateTime getReceivedAt() { return receivedAt; }
    public Long getCancelledByAccountId() { return cancelledByAccountId; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public String getCancellationReason() { return cancellationReason; }
    public String getRemarks() { return remarks; }
    public List<StockTransferItemResponse> getItems() { return items; }
    public Long getVersion() { return version; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
