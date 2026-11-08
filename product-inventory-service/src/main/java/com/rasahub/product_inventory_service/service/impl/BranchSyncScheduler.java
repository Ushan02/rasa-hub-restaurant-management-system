
package com.rasahub.product_inventory_service.service.impl;

import com.rasahub.product_inventory_service.dto.stocklocation.AuthBranchSnapshot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BranchSyncScheduler {

    private static final Logger log =
            LoggerFactory.getLogger(
                    BranchSyncScheduler.class
            );

    private final AuthBranchClient authBranchClient;

    private final StockLocationReconciliationService reconciliationService;

    public BranchSyncScheduler(
            AuthBranchClient authBranchClient,
            StockLocationReconciliationService reconciliationService
    ) {
        this.authBranchClient = authBranchClient;
        this.reconciliationService = reconciliationService;
    }

    @Scheduled(
            initialDelayString = "${branch.sync.initial-delay-ms:5000}",
            fixedDelayString = "${branch.sync.interval-ms:30000}"
    )
    public void synchronizeBranches() {

        try {

            List<AuthBranchSnapshot> branches =
                    authBranchClient.fetchBranches();

            reconciliationService.reconcile(branches);

            log.info(
                    "Branch synchronization completed: {} branches",
                    branches.size()
            );

        } catch (Exception exception) {

            log.error(
                    "Branch synchronization failed",
                    exception
            );
        }
    }
}
