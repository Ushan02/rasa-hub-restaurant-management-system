package com.rasahub.product_inventory_service.stockalert;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class LowStockAlertPostgresIndexIntegrationTest {

    @Test
    @EnabledIfEnvironmentVariable(named = "TEST_POSTGRES_URL", matches = "jdbc:postgresql:.+")
    void partialIndexAllowsResolvedHistoryAndPreventsConcurrentActiveAlerts() throws Exception {
        String url = System.getenv("TEST_POSTGRES_URL");
        String username = System.getenv().getOrDefault("TEST_POSTGRES_USERNAME", "");
        String password = System.getenv().getOrDefault("TEST_POSTGRES_PASSWORD", "");
        String schema = "low_stock_it_" + UUID.randomUUID().toString().replace("-", "");

        try (Connection setup = DriverManager.getConnection(url, username, password);
             Statement statement = setup.createStatement()) {
            statement.execute("CREATE SCHEMA " + schema);
            statement.execute("CREATE TABLE " + schema
                    + ".low_stock_alerts (location_code VARCHAR(5) NOT NULL, "
                    + "inventory_item_id BIGINT NOT NULL, status VARCHAR(20) NOT NULL)");
            statement.execute("CREATE UNIQUE INDEX uk_active_alert ON " + schema
                    + ".low_stock_alerts (location_code, inventory_item_id) "
                    + "WHERE status IN ('OPEN', 'ACKNOWLEDGED')");
            statement.execute("INSERT INTO " + schema
                    + ".low_stock_alerts VALUES ('Main', 1, 'RESOLVED'), ('Main', 1, 'RESOLVED')");
            assertEquals(2, scalarInt(statement, "SELECT COUNT(*) FROM " + schema + ".low_stock_alerts"));
        } catch (Exception exception) {
            dropSchema(url, username, password, schema);
            throw exception;
        }

        ExecutorService workers = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Boolean> first = workers.submit(
                    () -> insertActive(url, username, password, schema, "OPEN", ready, start)
            );
            Future<Boolean> second = workers.submit(
                    () -> insertActive(url, username, password, schema, "ACKNOWLEDGED", ready, start)
            );
            ready.await();
            start.countDown();
            boolean firstInserted = first.get();
            boolean secondInserted = second.get();
            assertNotEquals(firstInserted, secondInserted);
        } finally {
            workers.shutdownNow();
            dropSchema(url, username, password, schema);
        }
    }

    private boolean insertActive(
            String url,
            String username,
            String password,
            String schema,
            String status,
            CountDownLatch ready,
            CountDownLatch start
    ) throws InterruptedException, SQLException {
        ready.countDown();
        start.await();
        try (Connection connection = DriverManager.getConnection(url, username, password);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO " + schema
                    + ".low_stock_alerts VALUES ('Main', 1, '" + status + "')");
            return true;
        } catch (SQLException exception) {
            if ("23505".equals(exception.getSQLState())) {
                return false;
            }
            throw exception;
        }
    }

    private int scalarInt(Statement statement, String query) throws SQLException {
        try (var result = statement.executeQuery(query)) {
            result.next();
            return result.getInt(1);
        }
    }

    private void dropSchema(String url, String username, String password, String schema)
            throws SQLException {
        try (Connection connection = DriverManager.getConnection(url, username, password);
             Statement statement = connection.createStatement()) {
            statement.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
        }
    }
}
