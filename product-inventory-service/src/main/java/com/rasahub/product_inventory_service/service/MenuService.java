package com.rasahub.product_inventory_service.service;

import com.rasahub.product_inventory_service.dto.menu.MenuItemResponse;

import java.util.List;

public interface MenuService {

    List<MenuItemResponse> getMenu(
            Long branchId,
            Long categoryId,
            String keyword
    );
}