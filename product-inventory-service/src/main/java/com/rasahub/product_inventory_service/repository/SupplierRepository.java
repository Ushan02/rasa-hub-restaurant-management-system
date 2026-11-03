
package com.rasahub.product_inventory_service.repository;

import com.rasahub.product_inventory_service.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SupplierRepository
        extends JpaRepository<Supplier, Long> {

    @Query(
            value = "SELECT nextval('supplier_code_seq')",
            nativeQuery = true
    )
    Long getNextSupplierCodeSequenceValue();

    boolean existsBySupplierCode(String supplierCode);

    Optional<Supplier> findBySupplierCodeIgnoreCase(
            String supplierCode
    );

    List<Supplier> findByActiveOrderByIdAsc(
            boolean active
    );

    List<Supplier> findBySupplierNameContainingIgnoreCaseOrderByIdAsc(
            String supplierName
    );
}
