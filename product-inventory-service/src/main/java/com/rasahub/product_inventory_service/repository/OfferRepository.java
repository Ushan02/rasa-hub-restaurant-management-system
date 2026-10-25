package com.rasahub.product_inventory_service.repository;

import com.rasahub.product_inventory_service.entity.Offer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OfferRepository
        extends JpaRepository<Offer, Long> {

    Optional<Offer> findByOfferCodeIgnoreCase(
            String offerCode
    );

    List<Offer> findByActiveOrderByIdAsc(
            boolean active
    );

    List<Offer> findByProductVariantIdOrderByIdAsc(
            Long productVariantId
    );

    List<Offer> findByProductVariantIdAndActiveOrderByIdAsc(
            Long productVariantId,
            boolean active
    );

    @Query("""
            SELECT CASE
                WHEN COUNT(o) > 0 THEN true
                ELSE false
            END
            FROM Offer o
            WHERE o.productVariant.id = :productVariantId
              AND o.active = true
              AND o.startAt < :endAt
              AND o.endAt > :startAt
            """)
    boolean existsActiveOverlappingOffer(
            @Param("productVariantId")
            Long productVariantId,

            @Param("startAt")
            LocalDateTime startAt,

            @Param("endAt")
            LocalDateTime endAt
    );

    @Query("""
            SELECT CASE
                WHEN COUNT(o) > 0 THEN true
                ELSE false
            END
            FROM Offer o
            WHERE o.productVariant.id = :productVariantId
              AND o.active = true
              AND o.id <> :offerId
              AND o.startAt < :endAt
              AND o.endAt > :startAt
            """)
    boolean existsActiveOverlappingOfferExcludingId(
            @Param("productVariantId")
            Long productVariantId,

            @Param("offerId")
            Long offerId,

            @Param("startAt")
            LocalDateTime startAt,

            @Param("endAt")
            LocalDateTime endAt
    );

    @Query("""
            SELECT o
            FROM Offer o
            WHERE o.productVariant.id = :productVariantId
              AND o.active = true
              AND o.startAt <= :currentTime
              AND o.endAt >= :currentTime
            ORDER BY o.id ASC
            """)
    List<Offer> findCurrentActiveOffersForVariant(
            @Param("productVariantId")
            Long productVariantId,

            @Param("currentTime")
            LocalDateTime currentTime
    );

    @Query(
            value = "SELECT nextval('offer_code_seq')",
            nativeQuery = true
    )
    Long getNextOfferCodeSequenceValue();
}