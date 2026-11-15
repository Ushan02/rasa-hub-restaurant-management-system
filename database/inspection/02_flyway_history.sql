-- Run only if 01_schema_inventory.sql confirms this table exists.
-- Replace public with the confirmed schema if Flyway uses a non-default schema.
SELECT installed_rank,
       version,
       description,
       checksum,
       success
FROM public.flyway_schema_history
ORDER BY installed_rank;
