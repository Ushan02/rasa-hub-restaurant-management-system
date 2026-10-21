package com.rasahub.product_inventory_service.repository;

import com.rasahub.product_inventory_service.entity.ProductVariant;
import com.rasahub.product_inventory_service.enums.ProductVariantName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductVariantRepository
        extends JpaRepository<ProductVariant, Long> {

    boolean existsByProductIdAndName(
            Long productId,
            ProductVariantName name
    );

    boolean existsByProductIdAndNameAndIdNot(
            Long productId,
            ProductVariantName name,
            Long id
    );

    Optional<ProductVariant> findByVariantCodeIgnoreCase(
            String variantCode
    );

    List<ProductVariant> findByProductIdOrderByIdAsc(
            Long productId
    );

    List<ProductVariant> findByProductIdAndActiveOrderByIdAsc(
            Long productId,
            boolean active
    );

    List<ProductVariant> findByActiveOrderByIdAsc(
            boolean active
    );

    @Query(
            value = "SELECT nextval('variant_code_seq')",
            nativeQuery = true
    )
    Long getNextVariantCodeSequenceValue();
}