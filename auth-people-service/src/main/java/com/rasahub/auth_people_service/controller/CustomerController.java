package com.rasahub.auth_people_service.controller;

import com.rasahub.auth_people_service.dto.customer.CustomerCreateRequest;
import com.rasahub.auth_people_service.dto.customer.CustomerResponse;
import com.rasahub.auth_people_service.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping("/register")
    public ResponseEntity<CustomerResponse> registerCustomer(
            @Valid @RequestBody CustomerCreateRequest request
    ) {

        CustomerResponse response =
                customerService.createCustomer(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}