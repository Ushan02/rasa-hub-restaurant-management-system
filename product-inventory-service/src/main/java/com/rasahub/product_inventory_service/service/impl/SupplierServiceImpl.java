
package com.rasahub.product_inventory_service.service.impl;

import com.rasahub.product_inventory_service.dto.supplier.SupplierCreateRequest;
import com.rasahub.product_inventory_service.dto.supplier.SupplierUpdateRequest;
import com.rasahub.product_inventory_service.dto.supplier.SupplierResponse;
import com.rasahub.product_inventory_service.entity.Supplier;
import com.rasahub.product_inventory_service.exception.BusinessRuleException;
import com.rasahub.product_inventory_service.exception.ResourceNotFoundException;
import com.rasahub.product_inventory_service.repository.SupplierRepository;
import com.rasahub.product_inventory_service.service.SupplierService;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@Transactional
public class SupplierServiceImpl implements SupplierService {

    private static final Pattern SRI_LANKAN_PHONE_PATTERN =
            Pattern.compile(
                    "^(?:" +
                    "(?:070|071|072|075|076|077|078)[0-9]{7}" +
                    "|074[0-4][0-9]{6}" +
                    "|0(?:11|21|23|24|25|26|27|" +
                    "31|32|33|34|35|36|37|38|" +
                    "41|45|47|51|52|54|55|57|" +
                    "63|65|66|67|81|91)[0-9]{7}" +
                    ")$"
            );

    private final SupplierRepository supplierRepository;

    public SupplierServiceImpl(
            SupplierRepository supplierRepository
    ) {
        this.supplierRepository = supplierRepository;
    }

    @Override
    public SupplierResponse createSupplier(
            SupplierCreateRequest request
    ) {

        if (request == null) {
            throw new BusinessRuleException(
                    "Supplier request is required"
            );
        }

        String supplierName = normalizeSupplierName(
                request.getSupplierName()
        );

        String phoneNumber = validatePhoneNumber(
                request.getPhoneNumber()
        );

        String email = normalizeOptionalText(
                request.getEmail(),
                150,
                "Email"
        );

        String address = normalizeOptionalText(
                request.getAddress(),
                500,
                "Address"
        );

        Long sequenceValue = supplierRepository
                .getNextSupplierCodeSequenceValue();

        if (sequenceValue == null || sequenceValue < 1) {
            throw new BusinessRuleException(
                    "Unable to generate supplier code"
            );
        }

        String supplierCode = String.format(
                Locale.ROOT,
                "SUP%03d",
                sequenceValue
        );

        if (supplierCode.length() > 10) {
            throw new BusinessRuleException(
                    "Supplier code exceeds 10 characters"
            );
        }

        if (supplierRepository.existsBySupplierCode(supplierCode)) {
            throw new BusinessRuleException(
                    "Generated supplier code already exists. Check the database sequence"
            );
        }

        Supplier supplier = new Supplier(
                supplierCode,
                supplierName,
                phoneNumber,
                email,
                address
        );

        Supplier savedSupplier = supplierRepository.save(supplier);

        return new SupplierResponse(savedSupplier);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplierResponse> getSuppliers(
            Boolean active
    ) {

        List<Supplier> suppliers;

        if (active == null) {
            suppliers = supplierRepository.findAll(
                    Sort.by(Sort.Direction.ASC, "id")
            );
        } else {
            suppliers = supplierRepository
                    .findByActiveOrderByIdAsc(active);
        }

        return suppliers.stream()
                .map(SupplierResponse::new)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierResponse getSupplierById(
            Long id
    ) {

        Supplier supplier = findSupplierById(id);

        return new SupplierResponse(supplier);
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierResponse getSupplierByCode(
            String supplierCode
    ) {

        if (supplierCode == null || supplierCode.isBlank()) {
            throw new BusinessRuleException(
                    "Supplier code is required"
            );
        }

        String normalizedCode = supplierCode.trim()
                .toUpperCase(Locale.ROOT);

        Supplier supplier = supplierRepository
                .findBySupplierCodeIgnoreCase(normalizedCode)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Supplier not found"
                        )
                );

        return new SupplierResponse(supplier);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplierResponse> searchSuppliersByName(
            String supplierName
    ) {

        String searchName = normalizeSupplierName(supplierName);

        List<Supplier> suppliers = supplierRepository
                .findBySupplierNameContainingIgnoreCaseOrderByIdAsc(
                        searchName
                );

        return suppliers.stream()
                .map(SupplierResponse::new)
                .toList();
    }

    @Override
    public SupplierResponse updateSupplier(
            Long id,
            SupplierUpdateRequest request
    ) {

        if (request == null) {
            throw new BusinessRuleException(
                    "Supplier update request is required"
            );
        }

        Supplier supplier = findSupplierById(id);

        boolean hasUpdate = false;

        if (request.getSupplierName() != null) {
            supplier.setSupplierName(
                    normalizeSupplierName(
                            request.getSupplierName()
                    )
            );
            hasUpdate = true;
        }

        if (request.getPhoneNumber() != null) {
            supplier.setPhoneNumber(
                    validatePhoneNumber(
                            request.getPhoneNumber()
                    )
            );
            hasUpdate = true;
        }

        if (request.getEmail() != null) {
            supplier.setEmail(
                    normalizeOptionalText(
                            request.getEmail(),
                            150,
                            "Email"
                    )
            );
            hasUpdate = true;
        }

        if (request.getAddress() != null) {
            supplier.setAddress(
                    normalizeOptionalText(
                            request.getAddress(),
                            500,
                            "Address"
                    )
            );
            hasUpdate = true;
        }

        if (!hasUpdate) {
            throw new BusinessRuleException(
                    "Provide at least one field to update"
            );
        }

        Supplier updatedSupplier = supplierRepository.save(supplier);

        return new SupplierResponse(updatedSupplier);
    }

    @Override
    public SupplierResponse updateSupplierStatus(
            Long id,
            boolean active
    ) {

        Supplier supplier = findSupplierById(id);

        if (supplier.isActive() == active) {
            throw new BusinessRuleException(
                    active
                            ? "Supplier is already active"
                            : "Supplier is already inactive"
            );
        }

        supplier.setActive(active);

        Supplier updatedSupplier = supplierRepository.save(supplier);

        return new SupplierResponse(updatedSupplier);
    }

    private Supplier findSupplierById(Long id) {

        if (id == null || id < 1) {
            throw new BusinessRuleException(
                    "Valid supplier ID is required"
            );
        }

        return supplierRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Supplier not found"
                        )
                );
    }

    private String normalizeSupplierName(
            String supplierName
    ) {

        if (supplierName == null || supplierName.isBlank()) {
            throw new BusinessRuleException(
                    "Supplier name is required"
            );
        }

        String normalized = supplierName.trim();

        if (normalized.length() > 150) {
            throw new BusinessRuleException(
                    "Supplier name must not exceed 150 characters"
            );
        }

        return normalized;
    }

    private String validatePhoneNumber(
            String phoneNumber
    ) {

        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new BusinessRuleException(
                    "Phone number is required"
            );
        }

        if (phoneNumber.length() != 10) {
            throw new BusinessRuleException(
                    "Phone number must contain exactly 10 digits"
            );
        }

        if (!SRI_LANKAN_PHONE_PATTERN.matcher(phoneNumber).matches()) {
            throw new BusinessRuleException(
                    "Invalid Sri Lankan mobile or landline number"
            );
        }

        return phoneNumber;
    }

    private String normalizeOptionalText(
            String text,
            int maxLength,
            String fieldName
    ) {

        if (text == null || text.isBlank()) {
            return null;
        }

        String normalized = text.trim();

        if (normalized.length() > maxLength) {
            throw new BusinessRuleException(
                    fieldName + " must not exceed "
                            + maxLength + " characters"
            );
        }

        return normalized;
    }
}
