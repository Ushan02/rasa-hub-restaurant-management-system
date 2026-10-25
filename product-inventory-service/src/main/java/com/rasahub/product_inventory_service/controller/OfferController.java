package com.rasahub.product_inventory_service.controller;

import com.rasahub.product_inventory_service.dto.offer.OfferCreateRequest;
import com.rasahub.product_inventory_service.dto.offer.OfferResponse;
import com.rasahub.product_inventory_service.dto.offer.OfferStatusUpdateRequest;
import com.rasahub.product_inventory_service.dto.offer.OfferUpdateRequest;
import com.rasahub.product_inventory_service.service.OfferService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/offers")
public class OfferController {

    private final OfferService offerService;

    public OfferController(
            OfferService offerService
    ) {
        this.offerService = offerService;
    }

    @PostMapping
    @PreAuthorize(
            "hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER')"
    )
    public ResponseEntity<OfferResponse> createOffer(
            @Valid
            @RequestBody
            OfferCreateRequest request
    ) {

        OfferResponse response =
                offerService.createOffer(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    @PreAuthorize(
            "hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', 'BRANCH_MANAGER', 'CASHIER')"
    )
    public ResponseEntity<List<OfferResponse>> getOffers(
            @RequestParam(required = false)
            Boolean active,

            @RequestParam(required = false)
            Long productVariantId
    ) {

        List<OfferResponse> response =
                offerService.getOffers(
                        active,
                        productVariantId
                );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize(
            "hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', 'BRANCH_MANAGER', 'CASHIER')"
    )
    public ResponseEntity<OfferResponse> getOfferById(
            @PathVariable
            Long id
    ) {

        OfferResponse response =
                offerService.getOfferById(id);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/by-code/{offerCode}")
    @PreAuthorize(
            "hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER', 'BRANCH_MANAGER', 'CASHIER')"
    )
    public ResponseEntity<OfferResponse> getOfferByCode(
            @PathVariable
            String offerCode
    ) {

        OfferResponse response =
                offerService.getOfferByCode(
                        offerCode
                );

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize(
            "hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER')"
    )
    public ResponseEntity<OfferResponse> updateOffer(
            @PathVariable
            Long id,

            @Valid
            @RequestBody
            OfferUpdateRequest request
    ) {

        OfferResponse response =
                offerService.updateOffer(
                        id,
                        request
                );

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize(
            "hasAnyRole('OWNER', 'MAIN_OFFICE_MANAGER')"
    )
    public ResponseEntity<OfferResponse> updateOfferStatus(
            @PathVariable
            Long id,

            @Valid
            @RequestBody
            OfferStatusUpdateRequest request
    ) {

        OfferResponse response =
                offerService.updateOfferStatus(
                        id,
                        request
                );

        return ResponseEntity.ok(response);
    }
}