package com.rasahub.auth_people_service.repository;

import com.rasahub.auth_people_service.entity.Branch;
import com.rasahub.auth_people_service.enums.BranchType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BranchRepository
        extends JpaRepository<Branch, Long> {

    Optional<Branch> findByBranchCode(String branchCode);

    boolean existsByBranchCode(String branchCode);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(
            String name,
            Long id
    );

    boolean existsByType(
            BranchType type
    );

    List<Branch> findByActiveOrderByIdAsc(
            boolean active
    );
}