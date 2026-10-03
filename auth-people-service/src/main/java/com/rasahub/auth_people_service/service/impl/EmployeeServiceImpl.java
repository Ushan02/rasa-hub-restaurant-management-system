package com.rasahub.auth_people_service.service.impl;

import com.rasahub.auth_people_service.dto.employee.EmployeeCreateRequest;
import com.rasahub.auth_people_service.dto.employee.EmployeeResponse;
import com.rasahub.auth_people_service.dto.employee.EmployeeUpdateRequest;
import com.rasahub.auth_people_service.entity.AuthAccount;
import com.rasahub.auth_people_service.entity.Branch;
import com.rasahub.auth_people_service.entity.Employee;
import com.rasahub.auth_people_service.entity.EmployeeUpdateAudit;
import com.rasahub.auth_people_service.enums.EmployeePosition;
import com.rasahub.auth_people_service.enums.Role;
import com.rasahub.auth_people_service.exception.BusinessRuleException;
import com.rasahub.auth_people_service.exception.ResourceNotFoundException;
import com.rasahub.auth_people_service.repository.AuthAccountRepository;
import com.rasahub.auth_people_service.repository.BranchRepository;
import com.rasahub.auth_people_service.repository.EmployeeRepository;
import com.rasahub.auth_people_service.repository.EmployeeUpdateAuditRepository;
import com.rasahub.auth_people_service.service.EmployeeService;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    private static final Set<EmployeePosition> NON_LOGIN_POSITIONS =
            Set.of(
                    EmployeePosition.WAITER,
                    EmployeePosition.CHEF,
                    EmployeePosition.DELIVERY_PERSON
            );

    private static final Set<EmployeePosition> ACCOUNTANT_RESTRICTED_POSITIONS =
            Set.of(
                    EmployeePosition.OWNER,
                    EmployeePosition.MAIN_OFFICE_MANAGER,
                    EmployeePosition.ACCOUNTANT
            );

    private final EmployeeRepository employeeRepository;
    private final BranchRepository branchRepository;
    private final AuthAccountRepository authAccountRepository;
    private final EmployeeUpdateAuditRepository employeeUpdateAuditRepository;
    private final PasswordEncoder passwordEncoder;

    public EmployeeServiceImpl(
            EmployeeRepository employeeRepository,
            BranchRepository branchRepository,
            AuthAccountRepository authAccountRepository,
            EmployeeUpdateAuditRepository employeeUpdateAuditRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.employeeRepository = employeeRepository;
        this.branchRepository = branchRepository;
        this.authAccountRepository = authAccountRepository;
        this.employeeUpdateAuditRepository =
                employeeUpdateAuditRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // =========================================================
    // CREATE EMPLOYEE
    // =========================================================

    @Override
    @Transactional
    public EmployeeResponse createEmployee(
            EmployeeCreateRequest request,
            Role callerRole
    ) {

        if (request.getPosition() == EmployeePosition.OWNER
                && callerRole != Role.OWNER) {

            throw new AccessDeniedException(
                    "Only an Owner can register another Owner"
            );
        }

        if (callerRole == Role.ACCOUNTANT
                && ACCOUNTANT_RESTRICTED_POSITIONS.contains(
                request.getPosition()
        )) {

            throw new AccessDeniedException(
                    "Not allowed to register this position"
            );
        }

        Branch branch =
                branchRepository
                        .findById(
                                request.getBranchId()
                        )
                        .orElseThrow(() ->
                                new BusinessRuleException(
                                        "Branch not found"
                                )
                        );

        String nic =
                request
                        .getNic()
                        .trim()
                        .toUpperCase();

        String email =
                normalizeOptional(
                        request.getEmail()
                );

        String phone =
                normalizeOptional(
                        request.getPhone()
                );

        String address =
                normalizeOptional(
                        request.getAddress()
                );

        if (employeeRepository.existsByNic(nic)) {

            throw new BusinessRuleException(
                    "An employee with this NIC already exists"
            );
        }

        AuthAccount authAccount = null;

        if (request.isCreateLoginAccount()) {

            if (NON_LOGIN_POSITIONS.contains(
                    request.getPosition()
            )) {

                throw new BusinessRuleException(
                        "This position is not eligible for a login account"
                );
            }

            String password =
                    request.getPassword();

            if (password == null
                    || password.isBlank()) {

                throw new BusinessRuleException(
                        "Password is required when creating a login account"
                );
            }

            Role role =
                    Role.valueOf(
                            request
                                    .getPosition()
                                    .name()
                    );

            String hashedPassword =
                    passwordEncoder.encode(
                            password
                    );

            authAccount =
                    authAccountRepository.save(
                            new AuthAccount(
                                    hashedPassword,
                                    role
                            )
                    );
        }

        String employeeId =
                generateNextEmployeeId();

        Employee employee =
                new Employee(
                        employeeId,
                        request.getFirstName().trim(),
                        request.getLastName().trim(),
                        nic,
                        request.getPosition(),
                        branch
                );

        employee.setAddress(address);
        employee.setEmail(email);
        employee.setPhone(phone);
        employee.setAuthAccount(authAccount);

        Employee savedEmployee =
                employeeRepository.save(
                        employee
                );

        return toResponse(
                savedEmployee
        );
    }

    // =========================================================
    // GET EMPLOYEE LIST
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeResponse> getEmployees(
            Role callerRole,
            Long callerAccountId,
            Long requestedBranchId
    ) {

        // BRANCH MANAGER
        // Can view only own branch
        if (callerRole == Role.BRANCH_MANAGER) {

            Employee branchManager =
                    findEmployeeByAccountId(
                            callerAccountId
                    );

            Long ownBranchId =
                    branchManager
                            .getBranch()
                            .getId();

            if (requestedBranchId != null
                    && !requestedBranchId.equals(
                    ownBranchId
            )) {

                throw new AccessDeniedException(
                        "Branch Manager can only view employees from their own branch"
                );
            }

            return employeeRepository
                    .findByBranch_IdOrderByIdAsc(
                            ownBranchId
                    )
                    .stream()
                    .map(this::toResponse)
                    .toList();
        }

        // OWNER / MAIN OFFICE MANAGER / ACCOUNTANT
        if (callerRole == Role.OWNER
                || callerRole == Role.MAIN_OFFICE_MANAGER
                || callerRole == Role.ACCOUNTANT) {

            if (requestedBranchId == null) {

                return employeeRepository
                        .findAll(
                                Sort.by(
                                        Sort.Direction.ASC,
                                        "id"
                                )
                        )
                        .stream()
                        .map(this::toResponse)
                        .toList();
            }

            if (!branchRepository.existsById(
                    requestedBranchId
            )) {

                throw new ResourceNotFoundException(
                        "Branch not found"
                );
            }

            return employeeRepository
                    .findByBranch_IdOrderByIdAsc(
                            requestedBranchId
                    )
                    .stream()
                    .map(this::toResponse)
                    .toList();
        }

        throw new AccessDeniedException(
                "Not allowed to view employees"
        );
    }

    // =========================================================
    // GET SINGLE EMPLOYEE
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public EmployeeResponse getEmployeeById(
            Long employeeId,
            Role callerRole,
            Long callerAccountId
    ) {

        Employee targetEmployee =
                findEmployeeById(
                        employeeId
                );

        if (callerRole == Role.BRANCH_MANAGER) {

            Employee branchManager =
                    findEmployeeByAccountId(
                            callerAccountId
                    );

            Long managerBranchId =
                    branchManager
                            .getBranch()
                            .getId();

            Long targetBranchId =
                    targetEmployee
                            .getBranch()
                            .getId();

            if (!managerBranchId.equals(
                    targetBranchId
            )) {

                throw new AccessDeniedException(
                        "Branch Manager can only view employees from their own branch"
                );
            }

            return toResponse(
                    targetEmployee
            );
        }

        if (callerRole == Role.OWNER
                || callerRole == Role.MAIN_OFFICE_MANAGER
                || callerRole == Role.ACCOUNTANT) {

            return toResponse(
                    targetEmployee
            );
        }

        throw new AccessDeniedException(
                "Not allowed to view this employee"
        );
    }

    // =========================================================
    // UPDATE EMPLOYEE
    // =========================================================

    @Override
    @Transactional
    public EmployeeResponse updateEmployee(
            Long employeeId,
            EmployeeUpdateRequest request,
            Role callerRole,
            Long callerAccountId
    ) {

        // Target employee
        Employee targetEmployee =
                findEmployeeById(
                        employeeId
                );

        // Logged-in employee
        // This identity comes from JWT accountId
        Employee updatedByEmployee =
                findEmployeeByAccountId(
                        callerAccountId
                );

        // Check whether caller can update target
        validateEmployeeUpdateAccess(
                callerRole,
                targetEmployee
        );

        String nic =
                request
                        .getNic()
                        .trim()
                        .toUpperCase();

        String address =
                normalizeOptional(
                        request.getAddress()
                );

        String email =
                normalizeOptional(
                        request.getEmail()
                );

        String phone =
                normalizeOptional(
                        request.getPhone()
                );

        // Employee NIC must remain unique
        // but current employee's own NIC is allowed
        if (employeeRepository
                .existsByNicAndIdNot(
                        nic,
                        targetEmployee.getId()
                )) {

            throw new BusinessRuleException(
                    "An employee with this NIC already exists"
            );
        }

        Branch branch =
                branchRepository
                        .findById(
                                request.getBranchId()
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Branch not found"
                                )
                        );

        EmployeePosition currentPosition =
                targetEmployee.getPosition();

        EmployeePosition requestedPosition =
                request.getPosition();

        boolean positionChanged =
                currentPosition != requestedPosition;

        // Only OWNER / MAIN_OFFICE_MANAGER
        // can change position
        if (positionChanged) {

            validatePositionChange(
                    callerRole,
                    requestedPosition
            );

            updateEmployeePosition(
                    targetEmployee,
                    requestedPosition
            );
        }

        // employeeId is intentionally NOT changed
        targetEmployee.setFirstName(
                request.getFirstName().trim()
        );

        targetEmployee.setLastName(
                request.getLastName().trim()
        );

        targetEmployee.setNic(
                nic
        );

        targetEmployee.setAddress(
                address
        );

        targetEmployee.setEmail(
                email
        );

        targetEmployee.setPhone(
                phone
        );

        targetEmployee.setBranch(
                branch
        );

        Employee savedEmployee =
                employeeRepository.save(
                        targetEmployee
                );

        // Audit record
        EmployeeUpdateAudit audit =
                new EmployeeUpdateAudit(
                        savedEmployee.getEmployeeId(),
                        updatedByEmployee.getEmployeeId()
                );

        employeeUpdateAuditRepository.save(
                audit
        );

        return toResponse(
                savedEmployee
        );
    }

    // =========================================================
    // UPDATE AUTHORIZATION
    // =========================================================

    private void validateEmployeeUpdateAccess(
            Role callerRole,
            Employee targetEmployee
    ) {

        EmployeePosition targetPosition =
                targetEmployee.getPosition();

        // OWNER can update anyone
        if (callerRole == Role.OWNER) {
            return;
        }

        // MAIN OFFICE MANAGER
        // Can update everyone except OWNER
        if (callerRole == Role.MAIN_OFFICE_MANAGER) {

            if (targetPosition == EmployeePosition.OWNER) {

                throw new AccessDeniedException(
                        "Main Office Manager cannot update an Owner"
                );
            }

            return;
        }

        // ACCOUNTANT
        // Cannot update OWNER or MAIN OFFICE MANAGER
        if (callerRole == Role.ACCOUNTANT) {

            if (targetPosition == EmployeePosition.OWNER
                    || targetPosition ==
                    EmployeePosition.MAIN_OFFICE_MANAGER) {

                throw new AccessDeniedException(
                        "Accountant cannot update Owner or Main Office Manager"
                );
            }

            return;
        }

        throw new AccessDeniedException(
                "Not allowed to update employee details"
        );
    }

    // =========================================================
    // POSITION / ROLE CHANGE AUTHORIZATION
    // =========================================================

    private void validatePositionChange(
            Role callerRole,
            EmployeePosition requestedPosition
    ) {

        // OWNER can change positions
        if (callerRole == Role.OWNER) {
            return;
        }

        // MAIN OFFICE MANAGER
        // can change positions but cannot assign OWNER
        if (callerRole == Role.MAIN_OFFICE_MANAGER) {

            if (requestedPosition == EmployeePosition.OWNER) {

                throw new AccessDeniedException(
                        "Main Office Manager cannot assign the Owner position"
                );
            }

            return;
        }

        // ACCOUNTANT cannot change position
        throw new AccessDeniedException(
                "Only Owner or Main Office Manager can change employee position"
        );
    }

    // =========================================================
    // POSITION + AUTH ACCOUNT ROLE SYNC
    // =========================================================

    private void updateEmployeePosition(
            Employee employee,
            EmployeePosition requestedPosition
    ) {

        AuthAccount authAccount =
                employee.getAuthAccount();

        /*
         * If this employee already has a login account,
         * we do not directly change them to WAITER/CHEF/
         * DELIVERY_PERSON.
         *
         * Account disabling/removal will be handled
         * separately in account-management functionality.
         */
        if (authAccount != null
                && NON_LOGIN_POSITIONS.contains(
                requestedPosition
        )) {

            throw new BusinessRuleException(
                    "Employee with a login account cannot be changed directly to a non-login position"
            );
        }

        employee.setPosition(
                requestedPosition
        );

        /*
         * If employee has login account,
         * Employee.position and AuthAccount.role
         * must remain synchronized.
         */
        if (authAccount != null) {

            Role newRole =
                    Role.valueOf(
                            requestedPosition.name()
                    );

            authAccount.setRole(
                    newRole
            );

            authAccountRepository.save(
                    authAccount
            );
        }
    }

    // =========================================================
    // HELPER METHODS
    // =========================================================

    private Employee findEmployeeById(
            Long employeeId
    ) {

        return employeeRepository
                .findById(
                        employeeId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Employee not found"
                        )
                );
    }

    private Employee findEmployeeByAccountId(
            Long accountId
    ) {

        return employeeRepository
                .findByAuthAccount_Id(
                        accountId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Employee profile not found"
                        )
                );
    }

    private String generateNextEmployeeId() {

        Long nextValue =
                employeeRepository
                        .getNextEmployeeIdSequenceValue();

        return String.format(
                "EMP%03d",
                nextValue
        );
    }

    private String normalizeOptional(
            String value
    ) {

        if (value == null
                || value.isBlank()) {

            return null;
        }

        return value.trim();
    }

    private EmployeeResponse toResponse(
            Employee employee
    ) {

        return new EmployeeResponse(
                employee.getId(),
                employee.getEmployeeId(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getNic(),
                employee.getAddress(),
                employee.getEmail(),
                employee.getPhone(),
                employee.getPosition(),
                employee.getBranch().getBranchCode(),
                employee.getEmploymentStatus(),
                employee.getAuthAccount() != null
        );
    }
}