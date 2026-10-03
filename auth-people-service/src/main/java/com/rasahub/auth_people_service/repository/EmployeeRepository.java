package com.rasahub.auth_people_service.repository;

import com.rasahub.auth_people_service.entity.Employee;
import com.rasahub.auth_people_service.enums.EmployeePosition;
import com.rasahub.auth_people_service.enums.EmploymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository
        extends JpaRepository<Employee, Long> {

    Optional<Employee> findByEmployeeId(
            String employeeId
    );

    Optional<Employee> findByNic(
            String nic
    );

    boolean existsByEmployeeId(
            String employeeId
    );

    boolean existsByNic(
            String nic
    );

    boolean existsByNicAndIdNot(
            String nic,
            Long id
    );

    boolean existsByEmail(
            String email
    );

    boolean existsByPhone(
            String phone
    );

    List<Employee> findByBranch_Id(
            Long branchId
    );

    List<Employee> findByBranch_IdAndEmploymentStatus(
            Long branchId,
            EmploymentStatus employmentStatus
    );

    List<Employee> findByPosition(
            EmployeePosition position
    );

    Optional<Employee> findByAuthAccount_Id(
            Long accountId
    );

    List<Employee> findByBranch_IdOrderByIdAsc(
            Long branchId
    );

    @Query(
            value = "SELECT nextval('employee_id_seq')",
            nativeQuery = true
    )
    Long getNextEmployeeIdSequenceValue();
}