package com.rasahub.auth_people_service.service;

import com.rasahub.auth_people_service.dto.customer.CustomerCreateRequest;
import com.rasahub.auth_people_service.dto.customer.CustomerPasswordChangeRequest;
import com.rasahub.auth_people_service.dto.customer.CustomerResponse;
import com.rasahub.auth_people_service.dto.customer.CustomerUpdateRequest;

public interface CustomerService {

    CustomerResponse createCustomer(
            CustomerCreateRequest request
    );

    CustomerResponse getMyProfile(
            Long accountId
    );

    CustomerResponse updateMyProfile(
            Long accountId,
            CustomerUpdateRequest request
    );

    void changePassword(
            Long accountId,
            CustomerPasswordChangeRequest request
    );
}