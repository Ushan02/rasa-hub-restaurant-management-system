
package com.rasahub.auth_people_service.controller;

import com.rasahub.auth_people_service.dto.branch.BranchResponse;
import com.rasahub.auth_people_service.entity.Branch;
import com.rasahub.auth_people_service.repository.BranchRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@RestController
@RequestMapping("/internal/v1/branches")
public class InternalBranchSyncController {

    private final BranchRepository branchRepository;
    private final byte[] expectedKey;

    public InternalBranchSyncController(
            BranchRepository branchRepository,
            @Value("${branch.sync.internal-key}") String internalKey
    ) {
        if (internalKey == null || internalKey.length() < 32) {
            throw new IllegalStateException(
                    "A secure branch synchronization key is required"
            );
        }

        this.branchRepository = branchRepository;
        this.expectedKey =
                internalKey.getBytes(StandardCharsets.UTF_8);
    }

    @GetMapping("/snapshot")
    public List<BranchResponse> getBranchSnapshot(
            @RequestHeader(
                    name = "X-Internal-Sync-Key",
                    required = false
            ) String providedKey
    ) {
        if (providedKey == null
                || !MessageDigest.isEqual(
                expectedKey,
                providedKey.getBytes(StandardCharsets.UTF_8)
        )) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Unauthorized"
            );
        }

        List<Branch> branches = branchRepository.findAll(
                Sort.by(Sort.Direction.ASC, "id")
        );

        return branches.stream()
                .map(branch -> new BranchResponse(
                        branch.getId(),
                        branch.getBranchCode(),
                        branch.getName(),
                        branch.getType(),
                        branch.isActive()
                ))
                .toList();
    }
}
