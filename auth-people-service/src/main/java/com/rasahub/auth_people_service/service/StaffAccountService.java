package com.rasahub.auth_people_service.service;

import com.rasahub.auth_people_service.dto.auth.StaffPasswordChangeRequest;
import com.rasahub.auth_people_service.dto.auth.StaffPasswordResetRequest;
import com.rasahub.auth_people_service.enums.Role;

public interface StaffAccountService {

    void changeOwnPassword(
            Long accountId,
            StaffPasswordChangeRequest request
    );

    void resetEmployeePassword(
            Long targetEmployeeId,
            Long callerAccountId,
            Role callerRole,
            StaffPasswordResetRequest request
    );
}