package com.rasahub.product_inventory_service.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class ProductUserPrincipal implements UserDetails {

    private final Long accountId;
    private final String role;

    public ProductUserPrincipal(
            Long accountId,
            String role
    ) {
        this.accountId = accountId;
        this.role = role;
    }

    public Long getAccountId() {
        return accountId;
    }

    public String getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {

        return List.of(
                new SimpleGrantedAuthority(
                        "ROLE_" + role
                )
        );
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return accountId.toString();
    }
}