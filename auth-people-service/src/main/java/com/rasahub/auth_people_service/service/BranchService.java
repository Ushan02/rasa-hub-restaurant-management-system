package com.rasahub.auth_people_service.service;

import com.rasahub.auth_people_service.dto.branch.BranchRequest;
import com.rasahub.auth_people_service.dto.branch.BranchResponse;
import com.rasahub.auth_people_service.dto.branch.BranchStatusUpdateRequest;
import com.rasahub.auth_people_service.dto.branch.BranchUpdateRequest;
import com.rasahub.auth_people_service.enums.Role;

import java.util.List;

public interface BranchService {

    BranchResponse createBranch(
            BranchRequest request
    );

    List<BranchResponse> getBranches(
            Boolean active,
            Role callerRole,
            Long callerAccountId
    );

    BranchResponse getBranchById(
            Long id,
            Role callerRole,
            Long callerAccountId
    );

    BranchResponse getBranchByCode(
            String branchCode,
            Role callerRole,
            Long callerAccountId
    );

    BranchResponse updateBranch(
            Long id,
            BranchUpdateRequest request
    );

    BranchResponse updateBranchStatus(
            Long id,
            BranchStatusUpdateRequest request
    );
}