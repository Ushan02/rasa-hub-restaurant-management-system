package com.rasahub.product_inventory_service.service;

import com.rasahub.product_inventory_service.dto.offer.OfferCreateRequest;
import com.rasahub.product_inventory_service.dto.offer.OfferResponse;
import com.rasahub.product_inventory_service.dto.offer.OfferStatusUpdateRequest;
import com.rasahub.product_inventory_service.dto.offer.OfferUpdateRequest;

import java.util.List;

public interface OfferService {

    OfferResponse createOffer(
            OfferCreateRequest request
    );

    List<OfferResponse> getOffers(
            Boolean active,
            Long productVariantId
    );

    OfferResponse getOfferById(
            Long id
    );

    OfferResponse getOfferByCode(
            String offerCode
    );

    OfferResponse updateOffer(
            Long id,
            OfferUpdateRequest request
    );

    OfferResponse updateOfferStatus(
            Long id,
            OfferStatusUpdateRequest request
    );
}