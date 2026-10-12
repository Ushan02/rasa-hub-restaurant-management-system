package com.rasahub.product_inventory_service.repository;

import com.rasahub.product_inventory_service.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(
            String name,
            Long id
    );

    Optional<Product> findByProductCodeIgnoreCase(
            String productCode
    );

    List<Product> findByActiveOrderByIdAsc(
            boolean active
    );

    List<Product> findByCategoryIdOrderByIdAsc(
            Long categoryId
    );

    List<Product> findByCategoryIdAndActiveOrderByIdAsc(
            Long categoryId,
            boolean active
    );

    @Query(
            value = "SELECT nextval('product_code_seq')",
            nativeQuery = true
    )
    Long getNextProductCodeSequenceValue();
}
