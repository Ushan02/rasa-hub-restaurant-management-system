
package com.rasahub.product_inventory_service.service.impl;

import com.rasahub.product_inventory_service.dto.stocklocation.AuthBranchSnapshot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;

@Service
public class AuthBranchClient {

    private final RestClient restClient;

    public AuthBranchClient(
            @Value("${branch.sync.auth-base-url}") String authBaseUrl,
            @Value("${branch.sync.internal-key}") String internalKey
    ) {
        if (internalKey == null || internalKey.length() < 32) {
            throw new IllegalStateException(
                    "A secure branch synchronization key is required"
            );
        }

        SimpleClientHttpRequestFactory requestFactory =
                new SimpleClientHttpRequestFactory();

        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));

        this.restClient = RestClient.builder()
                .baseUrl(authBaseUrl)
                .defaultHeader("X-Internal-Sync-Key", internalKey)
                .requestFactory(requestFactory)
                .build();
    }

    public List<AuthBranchSnapshot> fetchBranches() {

        List<AuthBranchSnapshot> branches = restClient
                .get()
                .uri("/internal/v1/branches/snapshot")
                .retrieve()
                .body(
                        new ParameterizedTypeReference<
                                List<AuthBranchSnapshot>
                                >() {
                        }
                );

        if (branches == null) {
            throw new IllegalStateException(
                    "Auth Service returned an empty response body"
            );
        }

        return branches;
    }
}
