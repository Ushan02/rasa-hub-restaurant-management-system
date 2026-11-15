BEGIN;

ALTER TABLE low_stock_alerts
    ADD COLUMN IF NOT EXISTS resolution_reason VARCHAR(30);

COMMIT;
