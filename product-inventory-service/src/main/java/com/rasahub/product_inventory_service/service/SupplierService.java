
package com.rasahub.product_inventory_service.service;

import com.rasahub.product_inventory_service.dto.supplier.SupplierCreateRequest;
import com.rasahub.product_inventory_service.dto.supplier.SupplierUpdateRequest;
import com.rasahub.product_inventory_service.dto.supplier.SupplierResponse;

import java.util.List;

public interface SupplierService {

    SupplierResponse createSupplier(
            SupplierCreateRequest request
    );

    List<SupplierResponse> getSuppliers(
            Boolean active
    );

    SupplierResponse getSupplierById(
            Long id
    );

    SupplierResponse getSupplierByCode(
            String supplierCode
    );

    List<SupplierResponse> searchSuppliersByName(
            String supplierName
    );

    SupplierResponse updateSupplier(
            Long id,
            SupplierUpdateRequest request
    );

    SupplierResponse updateSupplierStatus(
            Long id,
            boolean active
    );
}
