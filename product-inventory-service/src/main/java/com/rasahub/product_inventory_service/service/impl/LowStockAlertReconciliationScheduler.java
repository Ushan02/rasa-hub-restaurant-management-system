package com.rasahub.product_inventory_service.service.impl;

import com.rasahub.product_inventory_service.entity.StockReorderLevel;
import com.rasahub.product_inventory_service.repository.StockReorderLevelRepository;
import com.rasahub.product_inventory_service.service.LowStockAlertEvaluationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LowStockAlertReconciliationScheduler {

    private static final Logger log = LoggerFactory.getLogger(LowStockAlertReconciliationScheduler.class);

    private final StockReorderLevelRepository stockReorderLevelRepository;
    private final LowStockAlertEvaluationService lowStockAlertEvaluationService;
    private final int pageSize;

    public LowStockAlertReconciliationScheduler(
            StockReorderLevelRepository stockReorderLevelRepository,
            LowStockAlertEvaluationService lowStockAlertEvaluationService,
            @Value("${low-stock.reconciliation.page-size:200}") int pageSize
    ) {
        if (pageSize < 1) {
            throw new IllegalArgumentException("Low-stock reconciliation page size must be positive");
        }
        this.stockReorderLevelRepository = stockReorderLevelRepository;
        this.lowStockAlertEvaluationService = lowStockAlertEvaluationService;
        this.pageSize = pageSize;
    }

    @Scheduled(
            initialDelayString = "${low-stock.reconciliation.initial-delay-ms:10000}",
            fixedDelayString = "${low-stock.reconciliation.interval-ms:60000}"
    )
    public void reconcileActiveConfigurations() {
        long afterId = 0;
        List<StockReorderLevel> page;
        do {
            page = stockReorderLevelRepository.findByActiveTrueAndIdGreaterThanOrderByIdAsc(
                    afterId,
                    PageRequest.of(0, pageSize)
            );
            for (StockReorderLevel config : page) {
                afterId = config.getId();
                try {
                    lowStockAlertEvaluationService.evaluateItem(
                            config.getStockLocation().getLocationCode(),
                            config.getInventoryItem().getId()
                    );
                } catch (RuntimeException exception) {
                    log.error(
                            "Low-stock reconciliation failed for location {} and item {}; it will be retried on the next run",
                            config.getStockLocation().getLocationCode(),
                            config.getInventoryItem().getId(),
                            exception
                    );
                }
            }
        } while (page.size() == pageSize);
    }
}
