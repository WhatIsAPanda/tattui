package app.db;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DbSmokeTest {
    final String sqliteDbFilename = "tattui.db";
    final String sqliteDbUrl = "jdbc:sqlite:" + sqliteDbFilename;

    /** Probe 1: print database, tables, and peek a few rows from common tables. */
    @Test
    public void probeTablesAndRows() throws Exception {

        String dbUrl = sqliteDbUrl;
        Assumptions.assumeTrue(
                Files.exists(Path.of(
                        sqliteDbUrl)),
                "Skipping smoke test: local SQLite database not found");

        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            assertNotNull(conn, "Connection should be established");
            assertFalse(conn.isClosed(), "Connection should be open");

            try (Statement pragmaStmt = conn.createStatement()) {
                pragmaStmt.execute("PRAGMA foreign_keys = ON;");
            }

            assertTrue(tableExists(conn, "Accounts"), "Accounts table should exist");
            assertTrue(tableExists(conn, "Posts2"), "Posts2 table should exist");
            assertTrue(tableHasData(conn, "Accounts"), "Accounts table should have at least one row");
            assertTrue(tableHasData(conn, "Posts2"), "Posts2 table should have at least one row");

            // Peek common tables (ignore if missing)
            peek(conn, "Accounts", "account_id, username, profile_picture_url, home_longitude, home_latitude", 5);
            peek(conn, "Posts2", "post_id, account_id, post_picture_url, caption, keywords", 5);
        }
    }

    private static void peek(Connection conn, String table, String cols, int limit) {
        String sql = "SELECT " + cols + " FROM " + table + " LIMIT " + limit;
        try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            System.out.println("\n-- " + table + " (up to " + limit + " rows) --");
            ResultSetMetaData md = rs.getMetaData();
            int n = md.getColumnCount();
            while (rs.next()) {
                StringBuilder row = new StringBuilder();
                for (int i = 1; i <= n; i++) {
                    if (i > 1)
                        row.append(" | ");
                    row.append(md.getColumnLabel(i)).append("=").append(rs.getString(i));
                }
                System.out.println(row);
            }
        } catch (Exception e) {
            System.out.println("\n-- " + table + " -- (not found or query failed: " + e.getMessage() + ")");
        }
    }

    private static boolean tableExists(Connection conn, String table) {
        try (ResultSet rs = conn.getMetaData().getTables(null, null, table, null)) {
            return rs.next();
        } catch (SQLException e) {
            System.out.println("[DbSmokeTest] Unable to check for table " + table + ": " + e.getMessage());
            return false;
        }
    }

    private static boolean tableHasData(Connection conn, String table) {
        String sql = "SELECT 1 FROM " + table + " LIMIT 1";
        try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            return rs.next();
        } catch (Exception e) {
            System.out.println("[DbSmokeTest] Unable to read from " + table + ": " + e.getMessage());
            return false;
        }
    }
}
