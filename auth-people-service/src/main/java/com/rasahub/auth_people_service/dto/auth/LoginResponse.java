package com.rasahub.auth_people_service.dto.auth;

import com.rasahub.auth_people_service.enums.Role;

public class LoginResponse {

    private final String token;
    private final Role role;
    private final Long accountId;

    public LoginResponse(String token, Role role, Long accountId) {
        this.token = token;
        this.role = role;
        this.accountId = accountId;
    }

    public String getToken() {
        return token;
    }
    public Role getRole() {
        return role;
    }
    public Long getAccountId() {
        return accountId;
    }
}