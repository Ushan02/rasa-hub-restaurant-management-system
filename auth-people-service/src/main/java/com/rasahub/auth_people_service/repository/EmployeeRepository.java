package com.rasahub.auth_people_service.repository;

import com.rasahub.auth_people_service.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmployeeRepository
        extends JpaRepository<Employee, Long> {

    Optional<Employee> findByEmployeeId(String employeeId);
    Optional<Employee> findByNic(String nic);
    Optional<Employee> findByEmail(String email);
    Optional<Employee> findByPhone(String phone);
    boolean existsByEmployeeId(String employeeId);
    boolean existsByNic(String nic);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
}