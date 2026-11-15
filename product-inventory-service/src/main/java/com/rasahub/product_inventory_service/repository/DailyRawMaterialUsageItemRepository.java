package com.rasahub.product_inventory_service.repository;

import com.rasahub.product_inventory_service.entity.DailyRawMaterialUsageItem;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyRawMaterialUsageItemRepository
        extends JpaRepository<DailyRawMaterialUsageItem, Long> {
}
