package com.rasahub.auth_people_service.service.impl;

import com.rasahub.auth_people_service.dto.employee.EmployeeCreateRequest;
import com.rasahub.auth_people_service.dto.employee.EmployeeResponse;
import com.rasahub.auth_people_service.entity.AuthAccount;
import com.rasahub.auth_people_service.entity.Branch;
import com.rasahub.auth_people_service.entity.Employee;
import com.rasahub.auth_people_service.enums.EmployeePosition;
import com.rasahub.auth_people_service.enums.Role;
import com.rasahub.auth_people_service.exception.BusinessRuleException;
import com.rasahub.auth_people_service.repository.AuthAccountRepository;
import com.rasahub.auth_people_service.repository.BranchRepository;
import com.rasahub.auth_people_service.repository.EmployeeRepository;
import com.rasahub.auth_people_service.service.EmployeeService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public EmployeeServiceImpl(EmployeeRepository employeeRepository,
                               BranchRepository branchRepository,
                               AuthAccountRepository authAccountRepository,
                               PasswordEncoder passwordEncoder) {
        this.employeeRepository = employeeRepository;
        this.branchRepository = branchRepository;
        this.authAccountRepository = authAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public EmployeeResponse createEmployee(EmployeeCreateRequest request, Role callerRole) {

        // Privilege-escalation guards run first because they are the cheapest checks.

        // Only an Owner can register another Owner
        if (request.getPosition() == EmployeePosition.OWNER
                && callerRole != Role.OWNER) {
            throw new AccessDeniedException("Only an Owner can register another Owner");
        }

        // An Accountant cannot register top-level positions
        if (callerRole == Role.ACCOUNTANT
                && ACCOUNTANT_RESTRICTED_POSITIONS.contains(request.getPosition())) {
            throw new AccessDeniedException("Not allowed to register this position");
        }

        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new BusinessRuleException("Branch not found"));

        String nic = request.getNic().trim().toUpperCase();
        String email = normalizeOptional(request.getEmail());
        String phone = normalizeOptional(request.getPhone());
        String address = normalizeOptional(request.getAddress());

        if (employeeRepository.existsByNic(nic)) {
            throw new BusinessRuleException("An employee with this NIC already exists");
        }

        AuthAccount authAccount = null;

        if (request.isCreateLoginAccount()) {

            if (NON_LOGIN_POSITIONS.contains(request.getPosition())) {
                throw new BusinessRuleException(
                        "This position is not eligible for a login account");
            }

            String password = request.getPassword();
            if (password == null || password.isBlank()) {
                throw new BusinessRuleException(
                        "Password is required when creating a login account");
            }

            Role role = Role.valueOf(request.getPosition().name());
            String hashedPassword = passwordEncoder.encode(password);

            authAccount = authAccountRepository.save(new AuthAccount(hashedPassword, role));
        }

        String employeeId = generateNextEmployeeId();

        Employee employee = new Employee(
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

        employee = employeeRepository.save(employee);

        return toResponse(employee);
    }

    private String generateNextEmployeeId() {
        Long nextValue = employeeRepository.getNextEmployeeIdSequenceValue();
        return String.format("EMP%03d", nextValue);
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private EmployeeResponse toResponse(Employee employee) {
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