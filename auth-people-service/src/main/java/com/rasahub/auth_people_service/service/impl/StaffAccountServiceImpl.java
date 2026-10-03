package com.rasahub.auth_people_service.service.impl;

import com.rasahub.auth_people_service.dto.auth.StaffPasswordChangeRequest;
import com.rasahub.auth_people_service.dto.auth.StaffPasswordResetRequest;
import com.rasahub.auth_people_service.entity.AuthAccount;
import com.rasahub.auth_people_service.entity.Employee;
import com.rasahub.auth_people_service.enums.EmployeePosition;
import com.rasahub.auth_people_service.enums.Role;
import com.rasahub.auth_people_service.exception.BusinessRuleException;
import com.rasahub.auth_people_service.exception.ResourceNotFoundException;
import com.rasahub.auth_people_service.repository.AuthAccountRepository;
import com.rasahub.auth_people_service.repository.EmployeeRepository;
import com.rasahub.auth_people_service.service.StaffAccountService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StaffAccountServiceImpl implements StaffAccountService {

    private final EmployeeRepository employeeRepository;
    private final AuthAccountRepository authAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public StaffAccountServiceImpl(
            EmployeeRepository employeeRepository,
            AuthAccountRepository authAccountRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.employeeRepository = employeeRepository;
        this.authAccountRepository = authAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void changeOwnPassword(
            Long accountId,
            StaffPasswordChangeRequest request
    ) {

        Employee employee =
                employeeRepository
                        .findByAuthAccount_Id(accountId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Employee profile not found"
                                )
                        );

        AuthAccount authAccount =
                employee.getAuthAccount();

        if (authAccount == null) {
            throw new BusinessRuleException(
                    "Employee does not have a login account"
            );
        }

        if (!passwordEncoder.matches(
                request.getCurrentPassword(),
                authAccount.getPasswordHash()
        )) {
            throw new BusinessRuleException(
                    "Current password is incorrect"
            );
        }

        if (passwordEncoder.matches(
                request.getNewPassword(),
                authAccount.getPasswordHash()
        )) {
            throw new BusinessRuleException(
                    "New password must be different from the current password"
            );
        }

        authAccount.setPasswordHash(
                passwordEncoder.encode(
                        request.getNewPassword()
                )
        );

        authAccountRepository.save(
                authAccount
        );
    }

    @Override
    @Transactional
    public void resetEmployeePassword(
            Long targetEmployeeId,
            Long callerAccountId,
            Role callerRole,
            StaffPasswordResetRequest request
    ) {

        Employee callerEmployee =
                employeeRepository
                        .findByAuthAccount_Id(callerAccountId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Employee profile not found"
                                )
                        );

        Employee targetEmployee =
                employeeRepository
                        .findById(targetEmployeeId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Employee not found"
                                )
                        );

        if (callerEmployee.getId().equals(
                targetEmployee.getId()
        )) {
            throw new BusinessRuleException(
                    "Use the own password change endpoint to change your password"
            );
        }

        validatePasswordResetAccess(
                callerRole,
                targetEmployee
        );

        AuthAccount targetAccount =
                targetEmployee.getAuthAccount();

        if (targetAccount == null) {
            throw new BusinessRuleException(
                    "Employee does not have a login account"
            );
        }

        if (passwordEncoder.matches(
                request.getNewPassword(),
                targetAccount.getPasswordHash()
        )) {
            throw new BusinessRuleException(
                    "New password must be different from the current password"
            );
        }

        targetAccount.setPasswordHash(
                passwordEncoder.encode(
                        request.getNewPassword()
                )
        );

        authAccountRepository.save(
                targetAccount
        );
    }

    private void validatePasswordResetAccess(
            Role callerRole,
            Employee targetEmployee
    ) {

        EmployeePosition targetPosition =
                targetEmployee.getPosition();

        if (callerRole == Role.OWNER) {
            return;
        }

        if (callerRole == Role.MAIN_OFFICE_MANAGER) {

            if (targetPosition == EmployeePosition.OWNER) {
                throw new AccessDeniedException(
                        "Main Office Manager cannot reset an Owner password"
                );
            }

            return;
        }

        if (callerRole == Role.ACCOUNTANT) {

            if (targetPosition == EmployeePosition.OWNER
                    || targetPosition
                    == EmployeePosition.MAIN_OFFICE_MANAGER) {

                throw new AccessDeniedException(
                        "Accountant cannot reset Owner or Main Office Manager password"
                );
            }

            return;
        }

        throw new AccessDeniedException(
                "Not allowed to reset employee password"
        );
    }
}