package com.rasahub.product_inventory_service.repository;

import com.rasahub.product_inventory_service.entity.BranchProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query("""
            SELECT bp
            FROM BranchProduct bp
            JOIN FETCH bp.product p
            JOIN FETCH p.category c
            WHERE bp.branchId = :branchId
              AND bp.available = true
              AND p.active = true
              AND c.active = true
              AND (
                    :categoryId IS NULL
                    OR c.id = :categoryId
              )
              AND (
                    :keyword IS NULL
                    OR LOWER(p.name)
                        LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(p.productCode)
                        LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            ORDER BY p.name ASC
            """)
    List<BranchProduct> findMenuItems(
            @Param("branchId")
            Long branchId,

            @Param("categoryId")
            Long categoryId,

            @Param("keyword")
            String keyword
    );
}