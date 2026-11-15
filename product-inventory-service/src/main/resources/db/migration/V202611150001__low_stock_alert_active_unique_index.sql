BEGIN;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM low_stock_alerts
        WHERE status IN ('OPEN', 'ACKNOWLEDGED')
        GROUP BY location_code, inventory_item_id
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Duplicate active low-stock alerts exist; reconcile them before applying the active-alert index';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE table_schema = current_schema()
          AND table_name = 'low_stock_alerts'
          AND constraint_name = 'uk_low_stock_alerts_location_item_status'
    ) THEN
        ALTER TABLE low_stock_alerts DROP CONSTRAINT uk_low_stock_alerts_location_item_status;
    END IF;
END $$;

DROP INDEX IF EXISTS idx_low_stock_alerts_active;

CREATE UNIQUE INDEX IF NOT EXISTS uk_low_stock_alerts_active_location_item
    ON low_stock_alerts (location_code, inventory_item_id)
    WHERE status IN ('OPEN', 'ACKNOWLEDGED');

COMMIT;
