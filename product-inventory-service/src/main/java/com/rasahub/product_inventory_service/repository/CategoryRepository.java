package com.rasahub.product_inventory_service.repository;

import com.rasahub.product_inventory_service.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface CategoryRepository
        extends JpaRepository<Category, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(
            String name,
            Long id
    );

    Optional<Category> findByCategoryCodeIgnoreCase(
            String categoryCode
    );

    List<Category> findByActiveOrderByIdAsc(boolean active);

    @Query(
            value = "SELECT nextval('category_code_seq')",
            nativeQuery = true
    )
    Long getNextCategoryCodeSequenceValue();
}