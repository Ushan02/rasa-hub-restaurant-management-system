package com.rasahub.auth_people_service.security;

import com.rasahub.auth_people_service.entity.AuthAccount;
import com.rasahub.auth_people_service.repository.AuthAccountRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final AuthAccountRepository authAccountRepository;

    public CustomUserDetailsService(
            AuthAccountRepository authAccountRepository
    ) {
        this.authAccountRepository = authAccountRepository;
    }

    @Override
    public UserDetails loadUserByUsername(
            String subject
    ) throws UsernameNotFoundException {

        Long accountId;

        try {
            accountId = Long.parseLong(subject);
        } catch (NumberFormatException e) {
            throw new UsernameNotFoundException(
                    "Invalid account identifier"
            );
        }

        AuthAccount account =
                authAccountRepository.findById(accountId)
                        .orElseThrow(() ->
                                new UsernameNotFoundException(
                                        "Account not found"
                                )
                        );

        return new CustomUserPrincipal(account);
    }
}