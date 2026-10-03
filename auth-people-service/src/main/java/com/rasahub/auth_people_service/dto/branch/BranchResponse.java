package com.rasahub.auth_people_service.dto.branch;

import com.rasahub.auth_people_service.enums.BranchType;

public class BranchResponse {

    private final Long id;
    private final String branchCode;
    private final String name;
    private final BranchType type;
    private final boolean active;

    public BranchResponse(
            Long id,
            String branchCode,
            String name,
            BranchType type,
            boolean active
    ) {
        this.id = id;
        this.branchCode = branchCode;
        this.name = name;
        this.type = type;
        this.active = active;
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

    public BranchType getType() {
        return type;
    }

    public boolean isActive() {
        return active;
    }
}