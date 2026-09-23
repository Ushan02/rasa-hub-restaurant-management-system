package com.rasahub.auth_people_service.entity;

import com.rasahub.auth_people_service.enums.BranchType;
import jakarta.persistence.*;

@Entity
@Table(name = "branches")
public class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "branch_code", unique = true, nullable = false, updatable = false, length = 20)
    private String branchCode;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BranchType type;

    @Column(nullable = false)
    private boolean active = true;

    protected Branch() {
        // required by JPA — never call this directly yourself
    }

    public Branch(String branchCode, String name, BranchType type) {
        this.branchCode = branchCode;
        this.name = name;
        this.type = type;
    }

    public Long getId() { return id; }
    public String getBranchCode() { return branchCode; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BranchType getType() { return type; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}