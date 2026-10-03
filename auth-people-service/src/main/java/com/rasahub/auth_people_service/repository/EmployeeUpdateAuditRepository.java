package com.rasahub.auth_people_service.repository;

import com.rasahub.auth_people_service.entity.EmployeeUpdateAudit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeUpdateAuditRepository
        extends JpaRepository<EmployeeUpdateAudit, Long> {
}