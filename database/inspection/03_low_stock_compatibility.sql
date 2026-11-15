-- Run only if 01_schema_inventory.sql confirms low_stock_alerts exists.
SELECT 'duplicate_active_alert_groups' AS check_name,
       COUNT(*) AS finding_count
FROM (
    SELECT location_code, inventory_item_id
    FROM low_stock_alerts
    WHERE status IN ('OPEN', 'ACKNOWLEDGED')
    GROUP BY location_code, inventory_item_id
    HAVING COUNT(*) > 1
) AS duplicate_groups
UNION ALL
SELECT 'historical_resolved_alerts',
       COUNT(*)
FROM low_stock_alerts
WHERE status = 'RESOLVED';

SELECT EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = current_schema()
      AND table_name = 'low_stock_alerts'
      AND column_name = 'resolution_reason'
) AS resolution_reason_column_exists,
       EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = current_schema()
      AND table_name = 'audit_logs'
) AS audit_logs_table_exists,
       EXISTS (
    SELECT 1
    FROM pg_catalog.pg_constraint
    WHERE conrelid = to_regclass(format('%I.low_stock_alerts', current_schema()))
      AND conname = 'uk_low_stock_alerts_location_item_status'
) AS old_active_alert_unique_constraint_exists;

SELECT index_relation.relname AS index_name,
       index_info.indisunique AS is_unique,
       index_info.indisvalid AS is_valid,
       index_info.indisready AS is_ready,
       pg_catalog.pg_get_indexdef(index_info.indexrelid) AS definition,
       pg_catalog.pg_get_expr(index_info.indpred, index_info.indrelid) AS predicate
FROM pg_catalog.pg_index AS index_info
JOIN pg_catalog.pg_class AS relation
  ON relation.oid = index_info.indrelid
JOIN pg_catalog.pg_class AS index_relation
  ON index_relation.oid = index_info.indexrelid
WHERE relation.oid = to_regclass(format('%I.low_stock_alerts', current_schema()))
  AND index_relation.relname = 'uk_low_stock_alerts_active_location_item';
