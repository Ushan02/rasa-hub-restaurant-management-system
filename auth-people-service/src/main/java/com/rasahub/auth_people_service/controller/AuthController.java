package com.rasahub.auth_people_service.controller;

import com.rasahub.auth_people_service.dto.auth.LoginRequest;
import com.rasahub.auth_people_service.dto.auth.LoginResponse;
import com.rasahub.auth_people_service.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/staff/login")
    public ResponseEntity<LoginResponse> staffLogin(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.loginStaff(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/customer/login")
    public ResponseEntity<LoginResponse> customerLogin(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.loginCustomer(request);
        return ResponseEntity.ok(response);
    }
}