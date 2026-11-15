package com.rasahub.product_inventory_service.repository;

import com.rasahub.product_inventory_service.entity.StockCountItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockCountItemRepository
        extends JpaRepository<StockCountItem, Long> {
}
