SELECT schemaname AS schema_name,
       tablename AS relation_name,
       'TABLE' AS relation_type
FROM pg_catalog.pg_tables
WHERE schemaname = ANY (current_schemas(false))
UNION ALL
SELECT schemaname AS schema_name,
       viewname AS relation_name,
       'VIEW' AS relation_type
FROM pg_catalog.pg_views
WHERE schemaname = ANY (current_schemas(false))
ORDER BY schema_name, relation_type, relation_name;

SELECT table_schema,
       table_name,
       ordinal_position,
       column_name,
       data_type,
       character_maximum_length,
       numeric_precision,
       numeric_scale,
       is_nullable,
       is_identity,
       identity_generation
FROM information_schema.columns
WHERE table_schema = ANY (current_schemas(false))
ORDER BY table_schema, table_name, ordinal_position;

SELECT namespace.nspname AS schema_name,
       relation.relname AS table_name,
       constraint_info.conname AS constraint_name,
       constraint_info.contype AS constraint_type,
       pg_catalog.pg_get_constraintdef(constraint_info.oid, true) AS definition,
       constraint_info.convalidated AS is_validated
FROM pg_catalog.pg_constraint AS constraint_info
JOIN pg_catalog.pg_class AS relation
  ON relation.oid = constraint_info.conrelid
JOIN pg_catalog.pg_namespace AS namespace
  ON namespace.oid = relation.relnamespace
WHERE namespace.nspname = ANY (current_schemas(false))
ORDER BY namespace.nspname, relation.relname, constraint_info.conname;

SELECT namespace.nspname AS schema_name,
       relation.relname AS table_name,
       index_relation.relname AS index_name,
       index_info.indisprimary AS is_primary,
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
JOIN pg_catalog.pg_namespace AS namespace
  ON namespace.oid = relation.relnamespace
WHERE namespace.nspname = ANY (current_schemas(false))
ORDER BY namespace.nspname, relation.relname, index_relation.relname;

SELECT namespace.nspname AS schema_name,
       relation.relname AS table_name,
       attribute.attname AS column_name,
       pg_catalog.format_type(attribute.atttypid, attribute.atttypmod) AS data_type,
       attribute.attnotnull AS is_not_null,
       attribute.attidentity AS identity_kind
FROM pg_catalog.pg_attribute AS attribute
JOIN pg_catalog.pg_class AS relation
  ON relation.oid = attribute.attrelid
JOIN pg_catalog.pg_namespace AS namespace
  ON namespace.oid = relation.relnamespace
WHERE namespace.nspname = ANY (current_schemas(false))
  AND relation.relname = 'flyway_schema_history'
  AND attribute.attnum > 0
  AND NOT attribute.attisdropped
ORDER BY namespace.nspname, attribute.attnum;

SELECT namespace.nspname AS schema_name,
       relation.relname AS table_name,
       constraint_info.conname AS old_active_alert_unique_constraint,
       constraint_info.contype AS constraint_type,
       pg_catalog.pg_get_constraintdef(constraint_info.oid, true) AS definition
FROM pg_catalog.pg_constraint AS constraint_info
JOIN pg_catalog.pg_class AS relation
  ON relation.oid = constraint_info.conrelid
JOIN pg_catalog.pg_namespace AS namespace
  ON namespace.oid = relation.relnamespace
WHERE namespace.nspname = ANY (current_schemas(false))
  AND relation.relname = 'low_stock_alerts'
  AND constraint_info.conname = 'uk_low_stock_alerts_location_item_status';

SELECT current_schema() AS inspected_schema,
       to_regclass(format('%I.flyway_schema_history', current_schema())) IS NOT NULL
           AS flyway_history_exists,
       to_regclass(format('%I.low_stock_alerts', current_schema())) IS NOT NULL
           AS low_stock_alerts_exists,
       to_regclass(format('%I.uk_low_stock_alerts_active_location_item', current_schema()))
           IS NOT NULL AS low_stock_partial_index_exists,
       to_regclass(format('%I.audit_logs', current_schema())) IS NOT NULL
           AS audit_logs_exists;

SELECT namespace.nspname AS schema_name,
       relation.relname AS table_name,
       index_relation.relname AS index_name,
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
JOIN pg_catalog.pg_namespace AS namespace
  ON namespace.oid = relation.relnamespace
WHERE namespace.nspname = ANY (current_schemas(false))
  AND relation.relname = 'low_stock_alerts'
  AND index_relation.relname = 'uk_low_stock_alerts_active_location_item';
