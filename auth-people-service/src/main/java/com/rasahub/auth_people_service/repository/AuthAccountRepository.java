package com.rasahub.auth_people_service.repository;

import com.rasahub.auth_people_service.entity.AuthAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthAccountRepository
        extends JpaRepository<AuthAccount, Long> {
}