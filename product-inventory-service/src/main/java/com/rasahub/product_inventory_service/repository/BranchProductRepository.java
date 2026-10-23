package com.rasahub.product_inventory_service.repository;

import com.rasahub.product_inventory_service.entity.BranchProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BranchProductRepository
        extends JpaRepository<BranchProduct, Long> {

    boolean existsByBranchIdAndProductId(
            Long branchId,
            Long productId
    );

    Optional<BranchProduct> findByBranchIdAndProductId(
            Long branchId,
            Long productId
    );

    List<BranchProduct> findByBranchIdOrderByIdAsc(
            Long branchId
    );

    List<BranchProduct> findByBranchIdAndAvailableOrderByIdAsc(
            Long branchId,
            boolean available
    );

    List<BranchProduct> findByProductIdOrderByIdAsc(
            Long productId
    );

    List<BranchProduct> findByAvailableOrderByIdAsc(
            boolean available
    );
}