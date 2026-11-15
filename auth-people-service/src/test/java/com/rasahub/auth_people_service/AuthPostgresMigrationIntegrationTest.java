package com.rasahub.auth_people_service;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
class AuthPostgresMigrationIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:18-alpine")
                    .withDatabaseName("rasa_hub_auth_migration_it")
                    .withUsername("test")
                    .withPassword("test-only-password");

    @Test
    void appliesFreshInstallMigrationsToDisposablePostgres() throws Exception {
        Flyway flyway = Flyway.configure()
                .dataSource(
                        POSTGRES.getJdbcUrl(),
                        POSTGRES.getUsername(),
                        POSTGRES.getPassword()
                )
                .cleanDisabled(true)
                .load();

        assertEquals(2, flyway.migrate().migrationsExecuted);
        flyway.validate();

        try (Connection connection = DriverManager.getConnection(
                     POSTGRES.getJdbcUrl(),
                     POSTGRES.getUsername(),
                     POSTGRES.getPassword());
             Statement statement = connection.createStatement()) {
            try (ResultSet migrations = statement.executeQuery(
                    "SELECT COUNT(*) FROM flyway_schema_history WHERE success")) {
                migrations.next();
                assertEquals(2, migrations.getInt(1));
            }
            try (ResultSet tables = statement.executeQuery(
                    "SELECT COUNT(*) FROM information_schema.tables "
                            + "WHERE table_schema = 'public' "
                            + "AND table_name IN ('auth_accounts', 'branches', "
                            + "'customers', 'employees', 'employee_update_audits')")) {
                tables.next();
                assertEquals(5, tables.getInt(1));
            }
            try (ResultSet constraints = statement.executeQuery(
                    "SELECT COUNT(*) FROM information_schema.table_constraints "
                            + "WHERE table_schema = 'public' "
                            + "AND constraint_name IN ('fk_employees_branch', "
                            + "'fk_customers_auth_account')")) {
                constraints.next();
                assertEquals(2, constraints.getInt(1));
            }
        }
    }
}
