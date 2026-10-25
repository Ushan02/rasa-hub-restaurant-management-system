package com.rasahub.product_inventory_service.controller;

import com.rasahub.product_inventory_service.dto.menu.MenuItemResponse;
import com.rasahub.product_inventory_service.service.MenuService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/menu")
public class MenuController {

    private final MenuService menuService;

    public MenuController(
            MenuService menuService
    ) {
        this.menuService = menuService;
    }

    @GetMapping
    @PreAuthorize(
            "hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', 'BRANCH_MANAGER', 'CASHIER', 'CUSTOMER')"
    )
    public ResponseEntity<List<MenuItemResponse>> getMenu(
            @RequestParam
            Long branchId,

            @RequestParam(required = false)
            Long categoryId,

            @RequestParam(required = false)
            String keyword
    ) {

        List<MenuItemResponse> response =
                menuService.getMenu(
                        branchId,
                        categoryId,
                        keyword
                );

        return ResponseEntity.ok(response);
    }
}