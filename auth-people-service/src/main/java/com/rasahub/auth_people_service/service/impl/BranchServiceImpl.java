package com.rasahub.auth_people_service.service.impl;

import com.rasahub.auth_people_service.dto.branch.BranchRequest;
import com.rasahub.auth_people_service.dto.branch.BranchResponse;
import com.rasahub.auth_people_service.dto.branch.BranchStatusUpdateRequest;
import com.rasahub.auth_people_service.dto.branch.BranchUpdateRequest;
import com.rasahub.auth_people_service.entity.Branch;
import com.rasahub.auth_people_service.entity.Employee;
import com.rasahub.auth_people_service.enums.BranchType;
import com.rasahub.auth_people_service.enums.Role;
import com.rasahub.auth_people_service.exception.BusinessRuleException;
import com.rasahub.auth_people_service.exception.ResourceNotFoundException;
import com.rasahub.auth_people_service.repository.BranchRepository;
import com.rasahub.auth_people_service.repository.EmployeeRepository;
import com.rasahub.auth_people_service.service.BranchService;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BranchServiceImpl implements BranchService {

    private final BranchRepository branchRepository;
    private final EmployeeRepository employeeRepository;

    public BranchServiceImpl(
            BranchRepository branchRepository,
            EmployeeRepository employeeRepository
    ) {
        this.branchRepository = branchRepository;
        this.employeeRepository = employeeRepository;
    }

    @Override
    @Transactional
    public BranchResponse createBranch(
            BranchRequest request
    ) {

        String branchCode =
                request.getBranchCode()
                        .trim()
                        .toUpperCase();

        String name =
                request.getName()
                        .trim();

        if (branchRepository.existsByBranchCode(branchCode)) {
            throw new BusinessRuleException(
                    "A branch with this branch code already exists"
            );
        }

        if (branchRepository.existsByNameIgnoreCase(name)) {
            throw new BusinessRuleException(
                    "A branch with this name already exists"
            );
        }

        if (request.getType() == BranchType.MAIN_OFFICE
                && branchRepository.existsByType(
                BranchType.MAIN_OFFICE
        )) {
            throw new BusinessRuleException(
                    "Main Office already exists"
            );
        }

        Branch branch =
                new Branch(
                        branchCode,
                        name,
                        request.getType()
                );

        Branch savedBranch =
                branchRepository.save(branch);

        return toResponse(savedBranch);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BranchResponse> getBranches(
            Boolean active,
            Role callerRole,
            Long callerAccountId
    ) {

        if (callerRole == Role.BRANCH_MANAGER) {

            Employee branchManager =
                    findEmployeeByAccountId(
                            callerAccountId
                    );

            Branch ownBranch =
                    branchManager.getBranch();

            if (active != null
                    && ownBranch.isActive() != active) {
                return List.of();
            }

            return List.of(
                    toResponse(ownBranch)
            );
        }

        if (canViewAllBranches(callerRole)) {

            List<Branch> branches;

            if (active == null) {

                branches =
                        branchRepository.findAll(
                                Sort.by(
                                        Sort.Direction.ASC,
                                        "id"
                                )
                        );

            } else {

                branches =
                        branchRepository
                                .findByActiveOrderByIdAsc(
                                        active
                                );
            }

            return branches
                    .stream()
                    .map(this::toResponse)
                    .toList();
        }

        throw new AccessDeniedException(
                "Not allowed to view branches"
        );
    }

    @Override
    @Transactional(readOnly = true)
    public BranchResponse getBranchById(
            Long id,
            Role callerRole,
            Long callerAccountId
    ) {

        Branch branch =
                findBranchById(id);

        validateBranchViewAccess(
                branch,
                callerRole,
                callerAccountId
        );

        return toResponse(branch);
    }

    @Override
    @Transactional(readOnly = true)
    public BranchResponse getBranchByCode(
            String branchCode,
            Role callerRole,
            Long callerAccountId
    ) {

        String normalizedBranchCode =
                branchCode
                        .trim()
                        .toUpperCase();

        Branch branch =
                branchRepository
                        .findByBranchCodeIgnoreCase(
                                normalizedBranchCode
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Branch not found"
                                )
                        );

        validateBranchViewAccess(
                branch,
                callerRole,
                callerAccountId
        );

        return toResponse(branch);
    }

    @Override
    @Transactional
    public BranchResponse updateBranch(
            Long id,
            BranchUpdateRequest request
    ) {

        Branch branch =
                findBranchById(id);

        String name =
                request.getName()
                        .trim();

        if (branchRepository
                .existsByNameIgnoreCaseAndIdNot(
                        name,
                        id
                )) {

            throw new BusinessRuleException(
                    "A branch with this name already exists"
            );
        }

        branch.setName(name);

        Branch savedBranch =
                branchRepository.save(branch);

        return toResponse(savedBranch);
    }

    @Override
    @Transactional
    public BranchResponse updateBranchStatus(
            Long id,
            BranchStatusUpdateRequest request
    ) {

        Branch branch =
                findBranchById(id);

        boolean requestedStatus =
                request.getActive();

        if (branch.isActive() == requestedStatus) {

            throw new BusinessRuleException(
                    requestedStatus
                            ? "Branch is already active"
                            : "Branch is already inactive"
            );
        }

        if (branch.getType() == BranchType.MAIN_OFFICE
                && !requestedStatus) {

            throw new BusinessRuleException(
                    "Main Office cannot be deactivated"
            );
        }

        branch.setActive(
                requestedStatus
        );

        Branch savedBranch =
                branchRepository.save(branch);

        return toResponse(savedBranch);
    }

    private void validateBranchViewAccess(
            Branch branch,
            Role callerRole,
            Long callerAccountId
    ) {

        if (canViewAllBranches(callerRole)) {
            return;
        }

        if (callerRole == Role.BRANCH_MANAGER) {

            Employee branchManager =
                    findEmployeeByAccountId(
                            callerAccountId
                    );

            Long ownBranchId =
                    branchManager
                            .getBranch()
                            .getId();

            if (!ownBranchId.equals(
                    branch.getId()
            )) {

                throw new AccessDeniedException(
                        "Branch Manager can only view their own branch"
                );
            }

            return;
        }

        throw new AccessDeniedException(
                "Not allowed to view this branch"
        );
    }

    private boolean canViewAllBranches(
            Role role
    ) {

        return role == Role.OWNER
                || role == Role.MAIN_OFFICE_MANAGER
                || role == Role.ACCOUNTANT;
    }

    private Branch findBranchById(
            Long id
    ) {

        return branchRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Branch not found"
                        )
                );
    }

    private Employee findEmployeeByAccountId(
            Long accountId
    ) {

        return employeeRepository
                .findByAuthAccount_Id(accountId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Employee profile not found"
                        )
                );
    }

    private BranchResponse toResponse(
            Branch branch
    ) {

        return new BranchResponse(
                branch.getId(),
                branch.getBranchCode(),
                branch.getName(),
                branch.getType(),
                branch.isActive()
        );
    }
}