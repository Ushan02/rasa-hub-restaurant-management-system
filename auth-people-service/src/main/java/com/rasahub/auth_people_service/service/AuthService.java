package com.rasahub.auth_people_service.service;

import com.rasahub.auth_people_service.dto.auth.LoginRequest;
import com.rasahub.auth_people_service.dto.auth.LoginResponse;

public interface AuthService {

    LoginResponse loginStaff(LoginRequest request);
    LoginResponse loginCustomer(LoginRequest request);
}