package com.rasahub.auth_people_service.service.impl;

import com.rasahub.auth_people_service.dto.employee.EmployeeCreateRequest;
import com.rasahub.auth_people_service.dto.employee.EmployeeResponse;
import com.rasahub.auth_people_service.entity.AuthAccount;
import com.rasahub.auth_people_service.entity.Branch;
import com.rasahub.auth_people_service.entity.Employee;
import com.rasahub.auth_people_service.enums.EmployeePosition;
import com.rasahub.auth_people_service.enums.Role;
import com.rasahub.auth_people_service.exception.BusinessRuleException;
import com.rasahub.auth_people_service.exception.ResourceNotFoundException;
import com.rasahub.auth_people_service.repository.AuthAccountRepository;
import com.rasahub.auth_people_service.repository.BranchRepository;
import com.rasahub.auth_people_service.repository.EmployeeRepository;
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

    private static final Set<EmployeePosition> NON_LOGIN_POSITIONS = Set.of(
            EmployeePosition.WAITER,
            EmployeePosition.CHEF,
            EmployeePosition.DELIVERY_PERSON
    );

    private static final Set<EmployeePosition> ACCOUNTANT_RESTRICTED_POSITIONS = Set.of(
            EmployeePosition.OWNER,
            EmployeePosition.MAIN_OFFICE_MANAGER,
            EmployeePosition.ACCOUNTANT
    );

    private final EmployeeRepository employeeRepository;
    private final BranchRepository branchRepository;
    private final AuthAccountRepository authAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public EmployeeServiceImpl(
            EmployeeRepository employeeRepository,
            BranchRepository branchRepository,
            AuthAccountRepository authAccountRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.employeeRepository = employeeRepository;
        this.branchRepository = branchRepository;
        this.authAccountRepository = authAccountRepository;
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

        // Only OWNER can create another OWNER
        if (request.getPosition() == EmployeePosition.OWNER
                && callerRole != Role.OWNER) {

            throw new AccessDeniedException(
                    "Only an Owner can register another Owner"
            );
        }

        // ACCOUNTANT cannot create top-level positions
        if (callerRole == Role.ACCOUNTANT
                && ACCOUNTANT_RESTRICTED_POSITIONS.contains(
                request.getPosition()
        )) {

            throw new AccessDeniedException(
                    "Not allowed to register this position"
            );
        }

        Branch branch =
                branchRepository.findById(
                                request.getBranchId()
                        )
                        .orElseThrow(() ->
                                new BusinessRuleException(
                                        "Branch not found"
                                )
                        );

        String nic =
                request.getNic()
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
                            request.getPosition().name()
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
                        request.getFirstName(),
                        request.getLastName(),
                        nic,
                        request.getPosition(),
                        branch
                );

        employee.setAddress(address);
        employee.setEmail(email);
        employee.setPhone(phone);
        employee.setAuthAccount(authAccount);

        Employee savedEmployee =
                employeeRepository.save(employee);

        return toResponse(savedEmployee);
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

        // Branch Manager can see own branch only
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

            // No branch filter -> return all employees
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

            // Verify requested branch exists
            if (!branchRepository.existsById(
                    requestedBranchId
            )) {

                throw new ResourceNotFoundException(
                        "Branch not found"
                );
            }

            // Return selected branch employees
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
                employeeRepository
                        .findById(employeeId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Employee not found"
                                )
                        );

        // Branch Manager can view only employees
        // from their own branch
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

        // These roles can view any employee
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
    // HELPER METHODS
    // =========================================================

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