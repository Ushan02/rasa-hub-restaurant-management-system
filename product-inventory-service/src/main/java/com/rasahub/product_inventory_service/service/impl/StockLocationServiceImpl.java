
package com.rasahub.product_inventory_service.service.impl;

import com.rasahub.product_inventory_service.dto.stocklocation.StockLocationResponse;
import com.rasahub.product_inventory_service.dto.stocklocation.StockLocationUpdateRequest;
import com.rasahub.product_inventory_service.entity.StockLocation;
import com.rasahub.product_inventory_service.enums.StockLocationType;
import com.rasahub.product_inventory_service.exception.BusinessRuleException;
import com.rasahub.product_inventory_service.exception.ResourceNotFoundException;
import com.rasahub.product_inventory_service.repository.StockLocationRepository;
import com.rasahub.product_inventory_service.service.StockLocationService;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class StockLocationServiceImpl
        implements StockLocationService {

    private final StockLocationRepository stockLocationRepository;

    public StockLocationServiceImpl(
            StockLocationRepository stockLocationRepository
    ) {
        this.stockLocationRepository = stockLocationRepository;
    }

    @Override
    public List<StockLocationResponse> getStockLocations(
            StockLocationType locationType,
            Boolean active
    ) {
        List<StockLocation> stockLocations;

        if (locationType == null && active == null) {

            stockLocations = stockLocationRepository.findAll(
                    Sort.by(Sort.Direction.ASC, "locationCode")
            );

        } else if (locationType != null && active == null) {

            stockLocations = stockLocationRepository
                    .findByLocationTypeOrderByLocationCodeAsc(
                            locationType
                    );

        } else if (locationType == null) {

            stockLocations = stockLocationRepository
                    .findByActiveOrderByLocationCodeAsc(active);

        } else {

            stockLocations = stockLocationRepository
                    .findByLocationTypeAndActiveOrderByLocationCodeAsc(
                            locationType,
                            active
                    );
        }

        return stockLocations.stream()
                .map(StockLocationResponse::new)
                .toList();
    }

    @Override
    public StockLocationResponse getStockLocationByCode(
            String locationCode
    ) {
        StockLocation stockLocation = stockLocationRepository
                .findById(locationCode)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Stock location not found"
                        )
                );

        return new StockLocationResponse(stockLocation);
    }

    @Override
    @Transactional
    public StockLocationResponse updateStockLocation(
            String locationCode,
            StockLocationUpdateRequest request
    ) {
        StockLocation stockLocation = stockLocationRepository
                .findById(locationCode)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Stock location not found"
                        )
                );

        if (request == null
                || request.getLocationName() == null
                || request.getLocationName().isBlank()) {
            throw new BusinessRuleException(
                    "Location name is required"
            );
        }

        String locationName = request.getLocationName().trim();

        if (locationName.length() > 150) {
            throw new BusinessRuleException(
                    "Location name must not exceed 150 characters"
            );
        }

        if (stockLocation.getLocationName().equals(locationName)) {
            return new StockLocationResponse(stockLocation);
        }

        stockLocation.setLocationName(locationName);

        StockLocation updatedLocation =
                stockLocationRepository.saveAndFlush(stockLocation);

        return new StockLocationResponse(updatedLocation);
    }

    @Override
    @Transactional
    public StockLocationResponse updateStockLocationStatus(
            String locationCode,
            boolean active
    ) {
        StockLocation stockLocation = stockLocationRepository
                .findById(locationCode)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Stock location not found"
                        )
                );

        if (!active) {

            if ("Main".equals(stockLocation.getLocationCode())) {
                throw new BusinessRuleException(
                        "Main warehouse cannot be deactivated"
                );
            }

            throw new BusinessRuleException(
                    "Stock location deactivation is disabled until "
                            + "stock balance and pending operation checks "
                            + "are implemented"
            );
        }

        if (stockLocation.isActive()) {
            return new StockLocationResponse(stockLocation);
        }

        stockLocation.setActive(true);

        StockLocation updatedLocation =
                stockLocationRepository.saveAndFlush(stockLocation);

        return new StockLocationResponse(updatedLocation);
    }
}
