# Taste Island Flyway Migration Preparation (Stage D2.1)

## Scope and safety

This preparation does not connect to either application database, alter an existing schema, run a migration, or enable Flyway in the default application configuration. The running local PostgreSQL service was not confirmed as disposable and was not accessed. Docker was present but its daemon was unavailable; `TEST_POSTGRES_URL` was unset.

Entity mappings and version-controlled scripts are the evidence for this inventory. The schemas and migration histories of all existing databases remain unverified. The scripts under `database/inspection` are read-only and intentionally do not access application credentials or return customer/employee rows.

## Auth + People schema inventory

All five mapped tables use generated `BIGINT` identity primary keys. Java enums are persisted as `VARCHAR` values (`EnumType.STRING`), not PostgreSQL enum types. No enum `CHECK` constraints are mapped.

| Table | Columns (type from mapping) | Keys and relationships |
|---|---|---|
| `auth_accounts` | `id BIGINT`, `password_hash VARCHAR(255)`, `role VARCHAR(30)`, `status VARCHAR(20)`, `created_at TIMESTAMP`, `updated_at TIMESTAMP` | Primary key `id`; no mapped foreign key or unique constraint. |
| `branches` | `id BIGINT`, `branch_code VARCHAR(5)`, `name VARCHAR(100)`, `type VARCHAR(30)`, `active BOOLEAN` | Primary key `id`; unique `branch_code`. |
| `customers` | `id BIGINT`, `first_name VARCHAR(60)`, `last_name VARCHAR(60)`, `phone VARCHAR(20)`, `email VARCHAR(100)`, `marketing_consent BOOLEAN`, `auth_account_id BIGINT` | Primary key `id`; nullable unique `phone`, `email`, and `auth_account_id`; account FK to `auth_accounts.id`. |
| `employees` | `id BIGINT`, `employee_id VARCHAR(20)`, `first_name VARCHAR(60)`, `last_name VARCHAR(60)`, `nic VARCHAR(20)`, `address VARCHAR(150)`, `email VARCHAR(100)`, `phone VARCHAR(20)`, `position VARCHAR(30)`, `branch_id BIGINT`, `employment_status VARCHAR(20)`, `auth_account_id BIGINT` | Primary key `id`; unique `employee_id`, `nic`, and nullable `auth_account_id`; branch and account FKs. |
| `employee_update_audits` | `id BIGINT`, `target_employee_id VARCHAR(20)`, `updated_by_employee_id VARCHAR(20)`, `updated_at TIMESTAMP` | Primary key `id`; employee ID values are historical string fields, not mapped FKs. |

Entity constructors set timestamps for new records; the mappings do not specify SQL timestamp defaults. `branches` also has no timestamp fields. Unique constraint names for some entity-only `unique=true` mappings are provider-generated; the new migration uses stable descriptive names without changing uniqueness semantics.

## Product + Inventory schema inventory

All generated IDs and `@Version` fields map to `BIGINT`; Java enums use `VARCHAR` with their mapped maximum lengths. Monetary and quantity precision is taken from each `@Column`. Account IDs are scalar references because identity/account ownership is in the separate Auth service. `branch_products.branch_id` likewise has no cross-service FK.

| Table | Mapped columns | Important relationships, uniqueness, indexes |
|---|---|---|
| `categories` | `id`, `category_code`, `name`, `description`, `active`, `created_at`, `updated_at` | Unique category code. |
| `products` | `id`, `product_code`, `name`, `description`, `image_url`, `category_id`, `active`, `created_at`, `updated_at` | Category FK; unique product code (declared both at field and table level). |
| `product_variants` | `id`, `variant_code`, `product_id`, `name`, `price NUMERIC(10,2)`, `active`, `created_at`, `updated_at` | Product FK; unique variant code and `(product_id,name)`; variant-code uniqueness is redundantly declared in the entity. |
| `branch_products` | `id`, `branch_id`, `product_id`, `available`, `created_at`, `updated_at` | Product FK; unique `(branch_id,product_id)`; no cross-service branch FK. |
| `offers` | `id`, `offer_code`, `name`, `description`, `product_variant_id`, `discount_type`, `discount_value NUMERIC(10,2)`, `start_at`, `end_at`, `active`, `created_at`, `updated_at` | Product-variant FK; unique offer code (declared twice in the entity). |
| `inventory_items` | `id`, `item_code`, `item_name`, `description`, `item_type`, `measurement_unit`, `active`, `created_at`, `updated_at` | Unique item code. |
| `stock_locations` | `location_code VARCHAR(5)`, `location_name`, `location_type`, `active`, `created_at`, `updated_at` | Primary key `location_code`; no seeded locations in the migration. Runtime reconciliation provisions locations from Auth branch data. |
| `suppliers` | `id`, `supplier_code`, `supplier_name`, `phone_number`, `email`, `address`, `active`, `created_at`, `updated_at` | Unique supplier code. |
| `supplier_items` | `id`, `supplier_id`, `inventory_item_id`, `unit_price NUMERIC(15,2)`, `minimum_order_quantity NUMERIC(15,3)`, `lead_time_days INTEGER`, `active`, `created_at`, `updated_at` | Supplier/item FKs; unique `(supplier_id,inventory_item_id)`. |
| `purchase_requests` | `id`, `destination_location_code`, `requested_by_account_id`, `status`, `reviewed_by_account_id`, `reviewed_at`, `remarks`, `rejection_reason`, `version`, `created_at`, `updated_at` | Location FK; status and requester indexes. |
| `purchase_request_items` | `id`, `purchase_request_id`, `inventory_item_id`, `requested_quantity NUMERIC(18,3)` | Request/item FKs; unique `(purchase_request_id,inventory_item_id)`. |
| `purchase_orders` | `id`, `purchase_request_id`, `supplier_id`, `destination_location_code`, `created_by_account_id`, `status`, `expected_delivery_date`, `remarks`, `cancellation_reason`, `version`, `created_at`, `updated_at` | Request/supplier/location FKs; unique request; status and supplier indexes. |
| `purchase_order_items` | `id`, `purchase_order_id`, `inventory_item_id`, `ordered_quantity NUMERIC(18,3)`, `unit_price NUMERIC(15,2)`, `received_quantity NUMERIC(18,3)`, `version` | Order/item FKs; unique `(purchase_order_id,inventory_item_id)`. |
| `goods_receipt_notes` | `id`, `purchase_order_id`, `receiving_location_code`, `received_by_account_id`, `received_date`, `delivery_reference`, `status`, `reviewed_by_account_id`, `reviewed_at`, `remarks`, `rejection_reason`, `version`, `created_at`, `updated_at` | Purchase order/location FKs; purchase order, status, and received-date indexes. |
| `goods_receipt_items` | `id`, `goods_receipt_note_id`, `inventory_item_id`, `delivered_quantity NUMERIC(18,3)`, `accepted_quantity NUMERIC(18,3)`, `rejected_quantity NUMERIC(18,3)`, `version` | Receipt/item FKs; unique `(goods_receipt_note_id,inventory_item_id)`. |
| `branch_stock_requests` | `id`, `branch_location_code`, `requested_by_account_id`, `status`, `reviewed_by_account_id`, `reviewed_at`, `remarks`, `rejection_reason`, `version`, `created_at`, `updated_at` | Location FK; branch, status, and creation-time indexes. |
| `branch_stock_request_items` | `id`, `branch_stock_request_id`, `inventory_item_id`, `requested_quantity NUMERIC(18,3)`, `received_quantity NUMERIC(18,3)`, `version` | Request/item FKs; unique `(branch_stock_request_id,inventory_item_id)`. |
| `stock_transfers` | `id`, `branch_stock_request_id`, `source_location_code`, `destination_location_code`, `created_by_account_id`, `status`, `dispatched_by_account_id`, `dispatched_at`, `received_by_account_id`, `received_at`, `cancelled_by_account_id`, `cancelled_at`, `cancellation_reason`, `remarks`, `version`, `created_at`, `updated_at` | Request and two location FKs; request, status, destination, and creation-time indexes. |
| `stock_transfer_items` | `id`, `stock_transfer_id`, `inventory_item_id`, `transfer_quantity NUMERIC(18,3)`, `version` | Transfer/item FKs; unique `(stock_transfer_id,inventory_item_id)`. |
| `daily_raw_material_usages` | `id`, `branch_location_code`, `usage_date`, `created_by_account_id`, `status`, `posted_by_account_id`, `posted_at`, `remarks`, `version`, `created_at`, `updated_at` | Branch location FK; branch, usage-date, status, and creation-time indexes. |
| `daily_raw_material_usage_items` | `id`, `daily_raw_material_usage_id`, `inventory_item_id`, `used_quantity NUMERIC(18,3)`, `version` | Usage/item FKs; unique usage/item pair and inventory-item index. |
| `stock_balances` | `id`, `location_code`, `inventory_item_id`, `quantity_on_hand NUMERIC(18,3)`, `minimum_stock_level NUMERIC(18,3)`, `version`, `created_at`, `updated_at` | Location/item FKs; unique `(location_code,inventory_item_id)`. |
| `stock_movements` | `id`, `location_code`, `inventory_item_id`, `movement_type`, `quantity NUMERIC(18,3)`, `quantity_before NUMERIC(18,3)`, `quantity_after NUMERIC(18,3)`, `reference_type`, `reference_id`, `performed_by_account_id`, `remarks`, `created_at` | Location/item FKs; location/time, item/time, and reference indexes. |
| `stock_reorder_levels` | `id`, `location_code`, `inventory_item_id`, `reorder_level NUMERIC(18,3)`, `recommended_reorder_quantity NUMERIC(18,3)`, `active`, `created_by_account_id`, `updated_by_account_id`, `created_at`, `updated_at`, `version` | Location/item FKs; unique `(location_code,inventory_item_id)`. |
| `low_stock_alerts` | `id`, `location_code`, `inventory_item_id`, `reorder_level_snapshot NUMERIC(18,3)`, `current_quantity_snapshot NUMERIC(18,3)`, `severity`, `status`, `detected_at`, `acknowledged_by_account_id`, `acknowledged_at`, `resolved_at`, `resolution_reason`, `last_evaluated_at`, `remarks`, `version`, `created_at`, `updated_at` | Location/item FKs; location/item/status and location/item/status/detected-time indexes; a partial unique active-alert index is added by V202611150001. |
| `stock_counts` | `id`, `location_code`, `count_date`, `status`, `created_by_account_id`, `submitted_by_account_id`, `submitted_at`, `reviewed_by_account_id`, `reviewed_at`, `rejection_reason`, `reconciled_by_account_id`, `reconciled_at`, `remarks`, `version`, `created_at`, `updated_at` | Location FK; location, status, count-date, and creator indexes. |
| `stock_count_items` | `id`, `stock_count_id`, `inventory_item_id`, `system_quantity NUMERIC(18,3)`, `system_balance_version`, `physical_quantity NUMERIC(18,3)`, `item_remarks`, `version` | Count/item FKs; unique count/item pair and inventory-item index. |
| `stock_adjustments` | `id`, `location_code`, `source_stock_count_id`, `adjustment_reason`, `direction`, `status`, `created_by_account_id`, `submitted_by_account_id`, `submitted_at`, `reviewed_by_account_id`, `reviewed_at`, `rejection_reason`, `posted_by_account_id`, `posted_at`, `remarks`, `version`, `created_at`, `updated_at` | Location and optional source-count FKs; unique `(source_stock_count_id,direction)`; location, status, creation-time, and creator indexes. |
| `stock_adjustment_items` | `id`, `stock_adjustment_id`, `inventory_item_id`, `source_stock_count_item_id`, `adjustment_quantity NUMERIC(18,3)`, `version` | Adjustment/item/count-item FKs; unique adjustment/item and unique optional source count item; inventory-item index. |
| `audit_logs` | `id`, `event_key`, `entity_type`, `entity_id`, `action`, `actor_account_id`, `previous_status`, `new_status`, `occurred_at`, `description`, `business_reference`, `location_code` | Created by V202611150003, not the initial migration; unique event key and entity/actor/location/action indexes. No FK to Auth accounts. |

Entity validation annotations (for example positive quantities and branch-code patterns) are not PostgreSQL `CHECK` constraints. The SQL migrations do not invent such constraints.

## Migration assessment

### Auth + People

- Flyway and PostgreSQL support are already dependencies.
- `V1__init_schema.sql` is empty. It is preserved; `V2__create_initial_schema.sql` provides the fresh-install tables.
- This supports an empty database that records V1 then V2. It does not prove that an existing database can safely run V2. If a database already has Hibernate-created tables but no V2 history, it must be inspected and reconciled; do not run V2 against it as-is.

### Product + Inventory

- The new `V202611150000__initial_schema.sql` sorts before the existing V202611150001, V202611150002, and V202611150003 scripts.
- It represents the pre-incremental schema: it intentionally omits `low_stock_alerts.resolution_reason` and `audit_logs`; V202611150002 and V202611150003 add them.
- No existing migration was changed. V001 and V002 contain explicit `BEGIN`/`COMMIT`. Flyway transaction compatibility has not been verified against PostgreSQL; V001 also raises if duplicate active alerts exist. These are blockers to production adoption until safely tested and reconciled without modifying already-applied migrations.
- The fresh schema creates one unique constraint for each effective code uniqueness rule, even where an entity redundantly declares column-level and table-level uniqueness. Existing Hibernate-generated schemas may therefore contain differently named/redundant unique indexes.
- The migration does not seed `stock_locations`. Runtime reconciliation provisions the canonical `Main` and branch records; the migration creates no alternate location identifiers.
- V202611150003's `occurred_at DESC` indexes differ in sort direction from the entity's default ascending index annotations. The existing migration is preserved; this should be reviewed before schema validation/adoption.

## Read-only PostgreSQL inspection

Run only against a database whose identity and access have been explicitly confirmed by its owner/DBA. These scripts do not connect on their own and contain only `SELECT` statements:

1. `database/inspection/00_database_identity.sql`
2. `database/inspection/01_schema_inventory.sql`
3. If the inventory shows a Flyway history table, run `02_flyway_history.sql`; adjust `public` only to the schema actually discovered.
4. If `low_stock_alerts` exists, run `03_low_stock_compatibility.sql`.
5. If `audit_logs` exists, run `04_audit_log_count.sql`.

From a trusted terminal, use the approved host, port, role, and database, with `psql -W` to prompt for the password rather than putting it in the command line:

```text
psql -X -W -h <approved-host> -p <approved-port> -U <approved-role> -d <confirmed-database> -v ON_ERROR_STOP=1 -f database/inspection/00_database_identity.sql
```

Repeat with each script above. Inspect the first script's database/schema identity before proceeding. The history/data scripts are conditional because they must not assume a table or schema exists. Do not run these scripts against an unconfirmed database.

## Isolated PostgreSQL tests

The migration integration tests use a PostgreSQL 18 Testcontainers instance named `rasa_hub_*_migration_it`, with test-only credentials and a container-local database. They do not consume `.env`, `TEST_POSTGRES_URL`, production credentials, or the host PostgreSQL service. The test container is disposable and must be started by Docker; when Docker is unavailable, the PostgreSQL tests are skipped and are not considered passed.

The inventory migration test applies the fresh baseline and all three existing migrations to an empty container database, validates Flyway history, inspects the active partial index, checks historical RESOLVED rows and duplicate prevention, and verifies numeric precision/FK/unique constraints and audit table presence. The Auth + People test applies V1/V2 to an empty container database and inspects the resulting schema. These tests are preparations only until actually run on an available disposable PostgreSQL 18 environment.

## Safe configuration plan

Default application configuration remains unchanged: Flyway stays disabled and Hibernate remains `ddl-auto=update`. The added opt-in `flyway` profile is not active by default, disables Flyway cleaning and baselining, and requires an explicit `FLYWAY_ENABLED=true` override before migrations can run; it uses Hibernate `validate`. Do not activate that profile against any existing database before its schema and history have been inspected and approved.

## Existing database adoption plan (not executed)

For each database independently:

1. Obtain written authorization and a verified, restorable backup; record the restore procedure and recovery owner.
2. Verify database name, host/cluster identity, schema, role, and environment with the DBA, then run only the read-only inspection scripts.
3. Record all tables, columns, constraints, index definitions/validity, Flyway rows/checksums, and relevant compatibility counts. Do not treat database names or `.env` contents as sufficient identity proof.
4. Compare the observed schema to the entity inventory and the exact migration state. Check constraints/indexes, numeric precision, resolved alerts, duplicate OPEN/ACKNOWLEDGED alerts, `resolution_reason`, and audit table state.
5. Reconcile manually executed migrations with the DBA using independent evidence. Do not fabricate Flyway history or blindly baseline. Any baseline version must describe the schema actually present.
6. If the schema matches the selected baseline, document a database-specific baseline version and approval. If it does not, stop and prepare a reviewed, additive reconciliation plan; do not use `baselineOnMigrate`.
7. For already-run SQL, compare its actual effects and checksum/history. Never rewrite an applied migration. A checksum mismatch requires a reviewed repair plan and backup; do not run `flyway repair` automatically.
8. For partially applied migrations, keep the application stopped for writes, capture a backup, inspect every partial effect, and prepare a DBA-approved recovery or forward-repair plan. Do not rerun or roll back blindly.
9. Test the exact approved adoption plan on a restored copy or disposable PostgreSQL 18 database. Ensure rollback/recovery is practicable before production use.
10. Only after schema alignment is confirmed, use Hibernate `validate`; keep Flyway disabled until the explicit adoption approval and runbook authorize migration.
11. Apply approved migrations in a maintenance window, monitor migration history and application health, and retain the backup until post-migration validation is accepted.
12. If any identity, history, schema, or data compatibility question remains unresolved, stop without migration.

## Remaining blockers and next stage

- Existing Auth and Inventory schemas and migration history remain unknown; no live database was inspected.
- V001/V002 transaction-control compatibility and PostgreSQL partial-index behavior remain unverified because no disposable PostgreSQL 18 runtime was available.
- Any pre-existing unique/index names, duplicate active alerts, partial migrations, and actual numeric precision remain unknown.
- This task does not enable Flyway in default/live profiles or apply migrations.

## Verification performed

- Auth + People `.\mvnw.cmd clean test`: 2 tests, 0 failures, 0 errors, 1 skipped (the PostgreSQL Testcontainers test).
- Product + Inventory `.\mvnw.cmd clean test`: 82 tests, 0 failures, 0 errors, 1 skipped (the PostgreSQL Testcontainers test).
- `.\mvnw.cmd -DskipTests compile`: successful in both services.
- `git diff --check`: successful.
- PostgreSQL migration execution and live-schema validation: **NOT EXECUTED**. Docker was unavailable, and the host PostgreSQL service was not used.

Recommended D2.2, subject to separate explicit approval: first execute the read-only scripts against one explicitly confirmed, backed-up development database; classify its exact schema/history state; then perform only a rehearsed, database-specific adoption on a restored disposable copy. Do not attempt fleet-wide baseline/migration or enable live Flyway until that one-database rehearsal is accepted.
