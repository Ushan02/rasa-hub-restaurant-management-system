-- Run only if 01_schema_inventory.sql confirms audit_logs exists.
SELECT COUNT(*) AS audit_log_row_count
FROM audit_logs;
