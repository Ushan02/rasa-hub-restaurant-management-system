
package com.rasahub.auth_people_service.entity;

import com.rasahub.auth_people_service.enums.BranchType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "branches")
public class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Branch code is required")
    @Size(
            min = 5,
            max = 5,
            message = "Branch code must contain exactly 5 characters"
    )
    @Pattern(
            regexp = "^(MO[0-9]{3}|BR[0-9]{3})$",
            message = "Branch code must follow MO001 or BR001 format"
    )
    @Column(
            name = "branch_code",
            unique = true,
            nullable = false,
            updatable = false,
            length = 5
    )
    private String branchCode;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BranchType type;

    @Column(nullable = false)
    private boolean active = true;

    protected Branch() {
    }

    public Branch(
            String branchCode,
            String name,
            BranchType type
    ) {
        this.branchCode = branchCode;
        this.name = name;
        this.type = type;
    }

    public Long getId() {
        return id;
    }

    public String getBranchCode() {
        return branchCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BranchType getType() {
        return type;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
