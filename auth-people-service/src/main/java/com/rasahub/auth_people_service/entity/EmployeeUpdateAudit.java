package com.rasahub.auth_people_service.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "employee_update_audits")
public class EmployeeUpdateAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "target_employee_id",
            nullable = false,
            length = 20
    )
    private String targetEmployeeId;

    @Column(
            name = "updated_by_employee_id",
            nullable = false,
            length = 20
    )
    private String updatedByEmployeeId;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;

    protected EmployeeUpdateAudit() {
    }

    public EmployeeUpdateAudit(
            String targetEmployeeId,
            String updatedByEmployeeId
    ) {
        this.targetEmployeeId = targetEmployeeId;
        this.updatedByEmployeeId = updatedByEmployeeId;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getTargetEmployeeId() {
        return targetEmployeeId;
    }

    public String getUpdatedByEmployeeId() {
        return updatedByEmployeeId;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}