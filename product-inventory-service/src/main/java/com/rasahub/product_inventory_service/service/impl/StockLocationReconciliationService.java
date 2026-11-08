
package com.rasahub.product_inventory_service.service.impl;

import com.rasahub.product_inventory_service.dto.stocklocation.AuthBranchSnapshot;
import com.rasahub.product_inventory_service.entity.StockLocation;
import com.rasahub.product_inventory_service.enums.StockLocationType;
import com.rasahub.product_inventory_service.repository.StockLocationRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StockLocationReconciliationService {

    private final StockLocationRepository stockLocationRepository;

    public StockLocationReconciliationService(
            StockLocationRepository stockLocationRepository
    ) {
        this.stockLocationRepository = stockLocationRepository;
    }

    @Transactional
    public void reconcile(
            List<AuthBranchSnapshot> branches
    ) {
        if (branches == null || branches.isEmpty()) {
            throw new IllegalStateException(
                    "Branch snapshot must not be empty"
            );
        }

        Map<String, AuthBranchSnapshot> branchMap =
                new HashMap<>();

        for (AuthBranchSnapshot branch : branches) {

            if (branch == null
                    || branch.branchCode() == null
                    || branch.name() == null
                    || branch.name().isBlank()
                    || branch.name().length() > 150
                    || branch.type() == null
                    || branch.active() == null) {

                throw new IllegalStateException(
                        "Invalid branch snapshot received"
                );
            }

            String branchCode = branch.branchCode();

            if ("MO001".equals(branchCode)) {

                if (!"MAIN_OFFICE".equals(branch.type())
                        || !branch.active()) {

                    throw new IllegalStateException(
                            "Invalid Main Office data"
                    );
                }

            } else if (branchCode.matches("BR[0-9]{3}")) {

                if (!"RESTAURANT_BRANCH".equals(branch.type())) {
                    throw new IllegalStateException(
                            "Invalid restaurant branch type"
                    );
                }

            } else {
                throw new IllegalStateException(
                        "Invalid branch code: " + branchCode
                );
            }

            if (branchMap.putIfAbsent(
                    branchCode,
                    branch
            ) != null) {

                throw new IllegalStateException(
                        "Duplicate branch code: " + branchCode
                );
            }
        }

        if (!branchMap.containsKey("MO001")) {
            throw new IllegalStateException(
                    "Main Office is missing from branch snapshot"
            );
        }

        for (AuthBranchSnapshot branch : branches) {

            String locationCode =
                    "MO001".equals(branch.branchCode())
                            ? "Main"
                            : branch.branchCode();

            StockLocationType locationType =
                    "MO001".equals(branch.branchCode())
                            ? StockLocationType.MAIN_WAREHOUSE
                            : StockLocationType.BRANCH;

            String locationName =
                    "Main".equals(locationCode)
                            ? "Main Office Warehouse"
                            : branch.name().trim();

            StockLocation location = stockLocationRepository
                    .findById(locationCode)
                    .orElse(null);

            if (location == null) {

                StockLocation newLocation =
                        new StockLocation(
                                locationCode,
                                locationName,
                                locationType
                        );

                newLocation.setActive(branch.active());

                stockLocationRepository.save(newLocation);

            } else {

                if (location.getLocationType() != locationType) {
                    throw new IllegalStateException(
                            "Stock location type mismatch: "
                                    + locationCode
                    );
                }

                if (!location.getLocationName()
                        .equals(locationName)) {

                    location.setLocationName(locationName);
                }

                if (location.isActive() != branch.active()) {
                    location.setActive(branch.active());
                }
            }
        }

        List<StockLocation> existingLocations =
                stockLocationRepository.findAll();

        for (StockLocation location : existingLocations) {

            if ("Main".equals(location.getLocationCode())) {
                continue;
            }

            if (!branchMap.containsKey(
                    location.getLocationCode()
            )) {

                if (location.isActive()) {
                    location.setActive(false);
                }
            }
        }
    }
}
