package app.db;

import org.junit.jupiter.api.Test;
import java.sql.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DbSmokeTest {

    private static String env(String key) {
        String v = System.getenv(key);
        if (v == null || v.isBlank()) throw new IllegalStateException("Missing env: " + key);
        return v;
    }

    /** Probe 1: print database, tables, and peek a few rows from common tables. */
    @Test
    public void probeTablesAndRows() throws Exception {
        String url  = env("DATABASE_URL");
        String user = env("DATABASE_USER");
        String pass = env("DATABASE_PASSWORD");

        System.out.println("Connecting: " + url + " as " + user);
        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            assertNotNull(conn);

            // Which DB am I on?
            try (Statement s = conn.createStatement();
                 ResultSet rs = s.executeQuery("SELECT DATABASE()")) {
                if (rs.next()) System.out.println("Current database: " + rs.getString(1));
            }

            // List tables
            System.out.println("\n-- TABLES --");
            try (Statement s = conn.createStatement();
                 ResultSet rs = s.executeQuery("SHOW TABLES")) {
                while (rs.next()) {
                    System.out.println(" • " + rs.getString(1));
                }
            }

            // Peek common tables (ignore if missing)
            peek(conn, "Users", "id, username, profile_picture, biography, longitude, latitude", 5);
            peek(conn, "Posts", "id, caption, postURL", 5);
            peek(conn, "PostOwnerships", "user_id, post_id", 5);
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
                    if (i > 1) row.append(" | ");
                    row.append(md.getColumnLabel(i)).append("=").append(rs.getString(i));
                }
                System.out.println(row);
            }
        } catch (Exception e) {
            System.out.println("\n-- " + table + " -- (not found or query failed: " + e.getMessage() + ")");
        }
    }
}
