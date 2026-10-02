package com.rasahub.auth_people_service.service.impl;

import com.rasahub.auth_people_service.dto.customer.CustomerCreateRequest;
import com.rasahub.auth_people_service.dto.customer.CustomerResponse;
import com.rasahub.auth_people_service.entity.AuthAccount;
import com.rasahub.auth_people_service.entity.Customer;
import com.rasahub.auth_people_service.enums.Role;
import com.rasahub.auth_people_service.exception.BusinessRuleException;
import com.rasahub.auth_people_service.exception.ResourceNotFoundException;
import com.rasahub.auth_people_service.repository.AuthAccountRepository;
import com.rasahub.auth_people_service.repository.CustomerRepository;
import com.rasahub.auth_people_service.service.CustomerService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final AuthAccountRepository authAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public CustomerServiceImpl(
            CustomerRepository customerRepository,
            AuthAccountRepository authAccountRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.customerRepository = customerRepository;
        this.authAccountRepository = authAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public CustomerResponse createCustomer(CustomerCreateRequest request) {

        String firstName = request.getFirstName().trim();
        String lastName = request.getLastName().trim();
        String phone = request.getPhone().trim();
        String email = request.getEmail().trim().toLowerCase();

        if (customerRepository.existsByEmail(email)) {
            throw new BusinessRuleException(
                    "A customer with this email already exists"
            );
        }

        if (customerRepository.existsByPhone(phone)) {
            throw new BusinessRuleException(
                    "A customer with this phone number already exists"
            );
        }

        String encodedPassword =
                passwordEncoder.encode(request.getPassword());

        AuthAccount authAccount =
                new AuthAccount(encodedPassword, Role.CUSTOMER);

        authAccount = authAccountRepository.save(authAccount);

        Customer customer = new Customer(
                firstName,
                lastName,
                phone,
                email
        );

        customer.setMarketingConsent(
                request.isMarketingConsent()
        );

        customer.setAuthAccount(authAccount);

        Customer savedCustomer =
                customerRepository.save(customer);

        return new CustomerResponse(
                savedCustomer.getId(),
                savedCustomer.getFirstName(),
                savedCustomer.getLastName(),
                savedCustomer.getPhone(),
                savedCustomer.getEmail(),
                savedCustomer.isMarketingConsent(),
                savedCustomer.getAuthAccount() != null
        );
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getMyProfile(Long accountId) {

        Customer customer =
                customerRepository.findByAuthAccount_Id(accountId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Customer profile not found"
                                )
                        );

        return new CustomerResponse(
                customer.getId(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getPhone(),
                customer.getEmail(),
                customer.isMarketingConsent(),
                customer.getAuthAccount() != null
        );
    }
}