package com.clothingstore.inventory.dao;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Owns the single SQLite connection for the whole app.
 * The .db file lives next to the running jar/exe, so the client's data
 * travels with the app and needs no separate database server.
 */
public final class DatabaseManager {

    private static final String DB_FILE = "inventory.db";
    private static final String URL = "jdbc:sqlite:" + DB_FILE;
    private static Connection connection;

    private DatabaseManager() {}

    public static synchronized Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                // Explicitly loading the driver class (rather than relying on
                // DriverManager's ServiceLoader auto-discovery) matters here:
                // JavaFX runs start() on the "JavaFX Application Thread", whose
                // context classloader can differ from the one that loaded the
                // SQLite jar, which makes auto-discovery silently miss it.
                Class.forName("org.sqlite.JDBC");
                connection = DriverManager.getConnection(URL);
                connection.createStatement().execute("PRAGMA foreign_keys = ON;");
            }
            return connection;
        } catch (SQLException | ClassNotFoundException e) {
            throw new DataAccessException("Could not open database connection", e);
        }
    }

    /** Runs schema.sql once at startup. CREATE TABLE IF NOT EXISTS makes this safe to repeat. */
    public static void initializeSchema() {
        try (InputStream in = DatabaseManager.class.getResourceAsStream("/schema.sql")) {
            if (in == null) {
                throw new IllegalStateException("schema.sql not found on classpath");
            }
            String script = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            try (Statement stmt = getConnection().createStatement()) {
                for (String statement : script.split(";")) {
                    String trimmed = statement.trim();
                    if (!trimmed.isEmpty()) {
                        stmt.execute(trimmed);
                    }
                }
            }
        } catch (IOException | SQLException e) {
            throw new DataAccessException("Failed to initialize database schema", e);
        }
        migrateAddImagePathColumn();
        migrateAddRemovedColumn();
    }

    /**
     * CREATE TABLE IF NOT EXISTS doesn't add new columns to a table that
     * already existed before this feature was added, so anyone with a
     * pre-existing inventory.db needs this one-time ALTER TABLE. Safe to
     * run every startup -- the "duplicate column" failure just means it
     * already ran before, so it's swallowed rather than treated as an error.
     */
    private static void migrateAddImagePathColumn() {
        try (Statement stmt = getConnection().createStatement()) {
            stmt.execute("ALTER TABLE items ADD COLUMN image_path TEXT");
        } catch (SQLException alreadyExists) {
            // Expected on every run after the first -- the column is already there.
        }
    }

    /**
     * Same idea for the "removed" flag (items taken out of inventory by a sale).
     * When the column is added for the first time, items that were already sold
     * out through a completed order are flagged removed too, so an existing
     * database matches the new behaviour straight away.
     */
    private static void migrateAddRemovedColumn() {
        try (Statement stmt = getConnection().createStatement()) {
            stmt.execute("ALTER TABLE items ADD COLUMN removed INTEGER NOT NULL DEFAULT 0");
        } catch (SQLException alreadyExists) {
            return; // column already there -> migration already ran
        }
        try (Statement stmt = getConnection().createStatement()) {
            stmt.execute("""
                UPDATE items SET removed = 1
                WHERE quantity <= 0
                  AND item_id IN (SELECT oi.item_id FROM order_items oi
                                  JOIN orders o ON o.order_id = oi.order_id
                                  WHERE o.status = 'COMPLETED')
                """);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to back-fill removed items", e);
        }
    }

    /** Called on app shutdown so the SQLite file is released cleanly. */
    public static synchronized void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ignored) {
        }
    }
}
