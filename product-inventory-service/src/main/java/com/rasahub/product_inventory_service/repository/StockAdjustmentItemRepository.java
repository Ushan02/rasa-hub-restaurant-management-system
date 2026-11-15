package com.rasahub.product_inventory_service.repository;

import com.rasahub.product_inventory_service.entity.StockAdjustmentItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockAdjustmentItemRepository
        extends JpaRepository<StockAdjustmentItem, Long> {
}
