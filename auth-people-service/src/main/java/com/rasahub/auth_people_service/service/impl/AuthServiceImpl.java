package com.rasahub.auth_people_service.service.impl;

import com.rasahub.auth_people_service.dto.auth.LoginRequest;
import com.rasahub.auth_people_service.dto.auth.LoginResponse;
import com.rasahub.auth_people_service.entity.AuthAccount;
import com.rasahub.auth_people_service.entity.Customer;
import com.rasahub.auth_people_service.entity.Employee;
import com.rasahub.auth_people_service.enums.AccountStatus;
import com.rasahub.auth_people_service.repository.CustomerRepository;
import com.rasahub.auth_people_service.repository.EmployeeRepository;
import com.rasahub.auth_people_service.security.JwtService;
import com.rasahub.auth_people_service.service.AuthService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AuthServiceImpl implements AuthService {

    private static final String INVALID_CREDENTIALS = "Invalid credentials";

    private final EmployeeRepository employeeRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthServiceImpl(EmployeeRepository employeeRepository,
                           CustomerRepository customerRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService) {
        this.employeeRepository = employeeRepository;
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Override
    public LoginResponse loginStaff(LoginRequest request) {
        Employee employee = employeeRepository.findByEmployeeId(request.getIdentifier())
                .orElseThrow(() -> new BadCredentialsException(INVALID_CREDENTIALS));

        AuthAccount account = employee.getAuthAccount();

        return authenticate(account, request.getPassword());
    }

    @Override
    public LoginResponse loginCustomer(LoginRequest request) {
        String identifier = request.getIdentifier();

        Customer customer = customerRepository.findByPhone(identifier)
                .or(() -> customerRepository.findByEmail(identifier))
                .orElseThrow(() -> new BadCredentialsException(INVALID_CREDENTIALS));

        AuthAccount account = customer.getAuthAccount();

        return authenticate(account, request.getPassword());
    }

    private LoginResponse authenticate(AuthAccount account, String rawPassword) {
        if (account == null) {
            throw new BadCredentialsException(INVALID_CREDENTIALS);
        }

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new BadCredentialsException(INVALID_CREDENTIALS);
        }

        if (!passwordEncoder.matches(rawPassword, account.getPasswordHash())) {
            throw new BadCredentialsException(INVALID_CREDENTIALS);
        }

        String token = jwtService.generateToken(
                account.getId().toString(),
                Map.of("role", account.getRole().name())
        );

        return new LoginResponse(token, account.getRole(), account.getId());
    }
}