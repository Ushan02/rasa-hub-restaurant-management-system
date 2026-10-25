package com.rasahub.product_inventory_service.service.impl;

import com.rasahub.product_inventory_service.dto.offer.OfferCreateRequest;
import com.rasahub.product_inventory_service.dto.offer.OfferResponse;
import com.rasahub.product_inventory_service.dto.offer.OfferStatusUpdateRequest;
import com.rasahub.product_inventory_service.dto.offer.OfferUpdateRequest;
import com.rasahub.product_inventory_service.entity.Offer;
import com.rasahub.product_inventory_service.entity.Product;
import com.rasahub.product_inventory_service.entity.ProductVariant;
import com.rasahub.product_inventory_service.enums.OfferDiscountType;
import com.rasahub.product_inventory_service.exception.BusinessRuleException;
import com.rasahub.product_inventory_service.exception.ResourceNotFoundException;
import com.rasahub.product_inventory_service.repository.OfferRepository;
import com.rasahub.product_inventory_service.repository.ProductVariantRepository;
import com.rasahub.product_inventory_service.service.OfferService;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class OfferServiceImpl
        implements OfferService {

    private final OfferRepository offerRepository;
    private final ProductVariantRepository productVariantRepository;

    public OfferServiceImpl(
            OfferRepository offerRepository,
            ProductVariantRepository productVariantRepository
    ) {
        this.offerRepository = offerRepository;
        this.productVariantRepository = productVariantRepository;
    }

    @Override
    public OfferResponse createOffer(
            OfferCreateRequest request
    ) {

        ProductVariant productVariant =
                findProductVariantById(
                        request.getProductVariantId()
                );

        validateVariantAndProductAreActive(
                productVariant
        );

        validateDateRange(
                request.getStartAt(),
                request.getEndAt()
        );

        validateDiscount(
                productVariant,
                request.getDiscountType(),
                request.getDiscountValue()
        );

        validateNoOverlappingOffer(
                productVariant.getId(),
                request.getStartAt(),
                request.getEndAt()
        );

        Long sequenceValue =
                offerRepository
                        .getNextOfferCodeSequenceValue();

        String offerCode =
                String.format(
                        "O%03d",
                        sequenceValue
                );

        Offer offer =
                new Offer(
                        offerCode,
                        request.getName().trim(),
                        normalizeDescription(
                                request.getDescription()
                        ),
                        productVariant,
                        request.getDiscountType(),
                        request.getDiscountValue(),
                        request.getStartAt(),
                        request.getEndAt()
                );

        Offer savedOffer =
                offerRepository.save(offer);

        return toResponse(savedOffer);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OfferResponse> getOffers(
            Boolean active,
            Long productVariantId
    ) {

        if (productVariantId != null) {
            validateProductVariantExists(
                    productVariantId
            );
        }

        List<Offer> offers;

        if (active == null
                && productVariantId == null) {

            offers =
                    offerRepository.findAll(
                            Sort.by(
                                    Sort.Direction.ASC,
                                    "id"
                            )
                    );

        } else if (active != null
                && productVariantId == null) {

            offers =
                    offerRepository
                            .findByActiveOrderByIdAsc(
                                    active
                            );

        } else if (active == null) {

            offers =
                    offerRepository
                            .findByProductVariantIdOrderByIdAsc(
                                    productVariantId
                            );

        } else {

            offers =
                    offerRepository
                            .findByProductVariantIdAndActiveOrderByIdAsc(
                                    productVariantId,
                                    active
                            );
        }

        return offers
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OfferResponse getOfferById(
            Long id
    ) {

        Offer offer =
                findOfferById(id);

        return toResponse(offer);
    }

    @Override
    @Transactional(readOnly = true)
    public OfferResponse getOfferByCode(
            String offerCode
    ) {

        if (offerCode == null
                || offerCode.isBlank()) {

            throw new BusinessRuleException(
                    "Offer code is required"
            );
        }

        String normalizedOfferCode =
                offerCode
                        .trim()
                        .toUpperCase(
                                Locale.ROOT
                        );

        Offer offer =
                offerRepository
                        .findByOfferCodeIgnoreCase(
                                normalizedOfferCode
                        )
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Offer not found"
                                        )
                        );

        return toResponse(offer);
    }

    @Override
    public OfferResponse updateOffer(
            Long id,
            OfferUpdateRequest request
    ) {

        Offer offer =
                findOfferById(id);

        ProductVariant productVariant =
                offer.getProductVariant();

        validateDateRange(
                request.getStartAt(),
                request.getEndAt()
        );

        validateDiscount(
                productVariant,
                request.getDiscountType(),
                request.getDiscountValue()
        );

        if (offer.isActive()) {

            validateVariantAndProductAreActive(
                    productVariant
            );

            validateNoOverlappingOfferExcludingCurrent(
                    productVariant.getId(),
                    offer.getId(),
                    request.getStartAt(),
                    request.getEndAt()
            );
        }

        offer.setName(
                request.getName().trim()
        );

        offer.setDescription(
                normalizeDescription(
                        request.getDescription()
                )
        );

        offer.setDiscountType(
                request.getDiscountType()
        );

        offer.setDiscountValue(
                request.getDiscountValue()
        );

        offer.setStartAt(
                request.getStartAt()
        );

        offer.setEndAt(
                request.getEndAt()
        );

        Offer updatedOffer =
                offerRepository.save(offer);

        return toResponse(updatedOffer);
    }

    @Override
    public OfferResponse updateOfferStatus(
            Long id,
            OfferStatusUpdateRequest request
    ) {

        Offer offer =
                findOfferById(id);

        boolean requestedStatus =
                request.getActive();

        if (offer.isActive()
                == requestedStatus) {

            String status =
                    requestedStatus
                            ? "active"
                            : "inactive";

            throw new BusinessRuleException(
                    "Offer is already "
                            + status
            );
        }

        if (requestedStatus) {

            ProductVariant productVariant =
                    offer.getProductVariant();

            validateVariantAndProductAreActive(
                    productVariant
            );

            validateDateRange(
                    offer.getStartAt(),
                    offer.getEndAt()
            );

            validateDiscount(
                    productVariant,
                    offer.getDiscountType(),
                    offer.getDiscountValue()
            );

            validateNoOverlappingOfferExcludingCurrent(
                    productVariant.getId(),
                    offer.getId(),
                    offer.getStartAt(),
                    offer.getEndAt()
            );
        }

        offer.setActive(
                requestedStatus
        );

        Offer updatedOffer =
                offerRepository.save(offer);

        return toResponse(updatedOffer);
    }

    private Offer findOfferById(
            Long id
    ) {

        return offerRepository
                .findById(id)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Offer not found"
                                )
                );
    }

    private ProductVariant findProductVariantById(
            Long productVariantId
    ) {

        return productVariantRepository
                .findById(productVariantId)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Product variant not found"
                                )
                );
    }

    private void validateProductVariantExists(
            Long productVariantId
    ) {

        if (!productVariantRepository
                .existsById(productVariantId)) {

            throw new ResourceNotFoundException(
                    "Product variant not found"
            );
        }
    }

    private void validateVariantAndProductAreActive(
            ProductVariant productVariant
    ) {

        if (!productVariant.isActive()) {

            throw new BusinessRuleException(
                    "Cannot use an inactive product variant for an offer"
            );
        }

        Product product =
                productVariant.getProduct();

        if (!product.isActive()) {

            throw new BusinessRuleException(
                    "Cannot use a variant of an inactive product for an offer"
            );
        }
    }

    private void validateDateRange(
            java.time.LocalDateTime startAt,
            java.time.LocalDateTime endAt
    ) {

        if (!startAt.isBefore(endAt)) {

            throw new BusinessRuleException(
                    "Offer start time must be before end time"
            );
        }
    }

    private void validateDiscount(
            ProductVariant productVariant,
            OfferDiscountType discountType,
            BigDecimal discountValue
    ) {

        if (discountType
                == OfferDiscountType.PERCENTAGE) {

            if (discountValue.compareTo(
                    BigDecimal.valueOf(100)
            ) > 0) {

                throw new BusinessRuleException(
                        "Percentage discount cannot exceed 100"
                );
            }

            return;
        }

        if (discountType
                == OfferDiscountType.FIXED_AMOUNT) {

            if (discountValue.compareTo(
                    productVariant.getPrice()
            ) >= 0) {

                throw new BusinessRuleException(
                        "Fixed discount must be less than the product variant price"
                );
            }
        }
    }

    private void validateNoOverlappingOffer(
            Long productVariantId,
            java.time.LocalDateTime startAt,
            java.time.LocalDateTime endAt
    ) {

        boolean overlappingOfferExists =
                offerRepository
                        .existsActiveOverlappingOffer(
                                productVariantId,
                                startAt,
                                endAt
                        );

        if (overlappingOfferExists) {

            throw new BusinessRuleException(
                    "An active overlapping offer already exists for this product variant"
            );
        }
    }

    private void validateNoOverlappingOfferExcludingCurrent(
            Long productVariantId,
            Long offerId,
            java.time.LocalDateTime startAt,
            java.time.LocalDateTime endAt
    ) {

        boolean overlappingOfferExists =
                offerRepository
                        .existsActiveOverlappingOfferExcludingId(
                                productVariantId,
                                offerId,
                                startAt,
                                endAt
                        );

        if (overlappingOfferExists) {

            throw new BusinessRuleException(
                    "An active overlapping offer already exists for this product variant"
            );
        }
    }

    private String normalizeDescription(
            String description
    ) {

        if (description == null
                || description.isBlank()) {

            return null;
        }

        return description.trim();
    }

    private OfferResponse toResponse(
            Offer offer
    ) {

        ProductVariant variant =
                offer.getProductVariant();

        Product product =
                variant.getProduct();

        return new OfferResponse(
                offer.getId(),
                offer.getOfferCode(),
                product.getProductCode(),
                product.getName(),
                variant.getVariantCode(),
                variant.getName().name(),
                offer.getName(),
                offer.getDescription(),
                offer.getDiscountType(),
                offer.getDiscountValue(),
                offer.getStartAt(),
                offer.getEndAt(),
                offer.isActive(),
                offer.getCreatedAt(),
                offer.getUpdatedAt()
        );
    }
}