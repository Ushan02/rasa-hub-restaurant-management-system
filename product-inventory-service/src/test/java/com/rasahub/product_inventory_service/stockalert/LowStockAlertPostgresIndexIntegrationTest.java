package com.rasahub.product_inventory_service.stockalert;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
class LowStockAlertPostgresIndexIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:18-alpine")
                    .withDatabaseName("rasa_hub_inventory_migration_it")
                    .withUsername("test")
                    .withPassword("test-only-password");

    @Test
    void migratesFreshPostgresAndEnforcesInventoryConstraints() throws Exception {
        Flyway flyway = Flyway.configure()
                .dataSource(
                        POSTGRES.getJdbcUrl(),
                        POSTGRES.getUsername(),
                        POSTGRES.getPassword()
                )
                .cleanDisabled(true)
                .load();

        assertEquals(4, flyway.migrate().migrationsExecuted);
        flyway.validate();

        try (Connection connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(),
                POSTGRES.getPassword())) {
            assertEquals(4, scalarInt(
                    connection,
                    "SELECT COUNT(*) FROM flyway_schema_history WHERE success"
            ));
            assertTrue(scalarBoolean(
                    connection,
                    "SELECT to_regclass('public.audit_logs') IS NOT NULL"
            ));
            assertTrue(scalarBoolean(
                    connection,
                    "SELECT index_info.indisvalid "
                            + "FROM pg_catalog.pg_index index_info "
                            + "JOIN pg_catalog.pg_class index_relation "
                            + "ON index_relation.oid = index_info.indexrelid "
                            + "WHERE index_relation.relname = "
                            + "'uk_low_stock_alerts_active_location_item'"
            ));
            assertTrue(scalarBoolean(
                    connection,
                    "SELECT pg_catalog.pg_get_expr(index_info.indpred, "
                            + "index_info.indrelid) LIKE '%OPEN%' "
                            + "AND pg_catalog.pg_get_expr(index_info.indpred, "
                            + "index_info.indrelid) LIKE '%ACKNOWLEDGED%' "
                            + "FROM pg_catalog.pg_index index_info "
                            + "JOIN pg_catalog.pg_class index_relation "
                            + "ON index_relation.oid = index_info.indexrelid "
                            + "WHERE index_relation.relname = "
                            + "'uk_low_stock_alerts_active_location_item'"
            ));

            try (PreparedStatement insertLocation = connection.prepareStatement(
                    "INSERT INTO stock_locations "
                            + "(location_code, location_name, location_type, active, "
                            + "created_at, updated_at) "
                            + "VALUES ('Main', 'Test Warehouse', 'MAIN_WAREHOUSE', "
                            + "TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")) {
                insertLocation.executeUpdate();
            }

            long firstItemId = insertInventoryItem(connection, "IT0001");
            long secondItemId = insertInventoryItem(connection, "IT0002");
            assertEquals(18, scalarInt(
                    connection,
                    "SELECT numeric_precision FROM information_schema.columns "
                            + "WHERE table_schema = 'public' "
                            + "AND table_name = 'stock_balances' "
                            + "AND column_name = 'quantity_on_hand'"
            ));
            assertEquals(3, scalarInt(
                    connection,
                    "SELECT numeric_scale FROM information_schema.columns "
                            + "WHERE table_schema = 'public' "
                            + "AND table_name = 'stock_balances' "
                            + "AND column_name = 'quantity_on_hand'"
            ));

            insertStockBalance(connection, firstItemId, "123.456");
            assertEquals(1, scalarInt(
                    connection,
                    "SELECT COUNT(*) FROM stock_balances "
                            + "WHERE quantity_on_hand = 123.456"
            ));
            SQLException duplicateBalance = insertExpectingFailure(
                    connection,
                    "INSERT INTO stock_balances "
                            + "(location_code, inventory_item_id, quantity_on_hand, "
                            + "minimum_stock_level, version, created_at, updated_at) "
                            + "VALUES ('Main', " + firstItemId
                            + ", 1, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"
            );
            assertEquals("23505", duplicateBalance.getSQLState());

            SQLException missingItem = insertExpectingFailure(
                    connection,
                    "INSERT INTO stock_balances "
                            + "(location_code, inventory_item_id, quantity_on_hand, "
                            + "minimum_stock_level, version, created_at, updated_at) "
                            + "VALUES ('Main', 999999999, 1, 0, 0, "
                            + "CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"
            );
            assertEquals("23503", missingItem.getSQLState());

            insertAlert(connection, firstItemId, "RESOLVED");
            insertAlert(connection, firstItemId, "RESOLVED");
            assertEquals(2, scalarInt(
                    connection,
                    "SELECT COUNT(*) FROM low_stock_alerts "
                            + "WHERE inventory_item_id = " + firstItemId
                            + " AND status = 'RESOLVED'"
            ));

            assertConcurrentActiveAlertUniqueness(secondItemId);
        }
    }

    private long insertInventoryItem(Connection connection, String itemCode)
            throws SQLException {
        String sql = "INSERT INTO inventory_items "
                + "(item_code, item_name, item_type, measurement_unit, active, "
                + "created_at, updated_at) "
                + "VALUES (?, 'Test Item', 'RAW_MATERIAL', 'KG', TRUE, "
                + "CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) RETURNING id";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, itemCode);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getLong(1);
            }
        }
    }

    private void insertStockBalance(
            Connection connection,
            long itemId,
            String quantity
    ) throws SQLException {
        String sql = "INSERT INTO stock_balances "
                + "(location_code, inventory_item_id, quantity_on_hand, "
                + "minimum_stock_level, version, created_at, updated_at) "
                + "VALUES ('Main', ?, ?, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, itemId);
            statement.setBigDecimal(2, new java.math.BigDecimal(quantity));
            statement.executeUpdate();
        }
    }

    private void insertAlert(
            Connection connection,
            long itemId,
            String status
    ) throws SQLException {
        String sql = "INSERT INTO low_stock_alerts "
                + "(location_code, inventory_item_id, reorder_level_snapshot, "
                + "current_quantity_snapshot, severity, status, detected_at, "
                + "version, created_at, updated_at) "
                + "VALUES ('Main', ?, 10, 0, 'HIGH', ?, CURRENT_TIMESTAMP, "
                + "0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, itemId);
            statement.setString(2, status);
            statement.executeUpdate();
        }
    }

    private void assertConcurrentActiveAlertUniqueness(long itemId)
            throws Exception {
        ExecutorService workers = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Boolean> open = workers.submit(
                    () -> insertActiveAlert(itemId, "OPEN", ready, start)
            );
            Future<Boolean> acknowledged = workers.submit(
                    () -> insertActiveAlert(itemId, "ACKNOWLEDGED", ready, start)
            );
            ready.await();
            start.countDown();
            assertNotEquals(open.get(), acknowledged.get());
        } finally {
            workers.shutdownNow();
        }
    }

    private boolean insertActiveAlert(
            long itemId,
            String status,
            CountDownLatch ready,
            CountDownLatch start
    ) throws Exception {
        ready.countDown();
        start.await();
        try (Connection connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(),
                POSTGRES.getPassword())) {
            connection.setAutoCommit(false);
            try {
                insertAlert(connection, itemId, status);
                connection.commit();
                return true;
            } catch (SQLException exception) {
                connection.rollback();
                if ("23505".equals(exception.getSQLState())) {
                    return false;
                }
                throw exception;
            }
        }
    }

    private SQLException insertExpectingFailure(
            Connection connection,
            String sql
    ) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        } catch (SQLException exception) {
            return exception;
        }
        throw new AssertionError("Expected the insert to violate a constraint");
    }

    private int scalarInt(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getInt(1);
        }
    }

    private boolean scalarBoolean(Connection connection, String sql)
            throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getBoolean(1);
        }
    }
}
