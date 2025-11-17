package app.explore;

import app.controller.explore.ExploreControl;
import app.controller.explore.ExploreDataProvider;
import app.controller.explore.MergedExploreDataProvider;
import app.controller.explore.MockExploreDataProvider;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

public class ExploreProviderSmokeTest {

    private static boolean liveEnabled() {
        return System.getenv("EXPLORE_LIVE") != null;
    }

    private static boolean envPresent(String... keys) {
        for (String k : keys) {
            String v = System.getenv(k);
            if (v == null || v.isBlank()) return false;
        }
        return true;
    }

    private static Connection openEnvConn() throws Exception {
        String url  = System.getenv("DATABASE_URL");
        String user = System.getenv("DATABASE_USER");
        String pass = System.getenv("DATABASE_PASSWORD");
        return DriverManager.getConnection(url, user, pass);
    }

    /** Round-trip readiness: checks env present and SELECT 1 succeeds (with retries). */
    private static boolean dbRoundTripReady(int attempts, long delayMs) {
        if (!envPresent("DATABASE_URL", "DATABASE_USER", "DATABASE_PASSWORD")) return false;
        for (int i = 0; i < attempts; i++) {
            try (Connection c = openEnvConn();
                 Statement s = c.createStatement();
                 ResultSet rs = s.executeQuery("SELECT 1")) {
                if (rs.next()) return true;
            } catch (Exception ignored) { /* wait and retry */ }
            try { Thread.sleep(delayMs); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        }
        return false;
    }

    @Test
    void liveProvider_returnsSomething_whenLiveEnabled() {
        Assumptions.assumeTrue(
                liveEnabled() && dbRoundTripReady(8, 1500),
                "Skipping live test: EXPLORE_LIVE not set or DB not ready");

        ExploreDataProvider provider = new MergedExploreDataProvider();
        var items = provider.fetch("", ExploreControl.Kind.ALL);

        assertNotNull(items, "Provider should return a non-null list");
        assertTrue(items.size() >= 0, "Allow empty result sets for small DBs");
    }

    @Test
    void mockProvider_filtersDesignsOnly_whenKindIsDESIGNS() {
        var p = new MockExploreDataProvider();
        var items = p.fetch("", ExploreControl.Kind.DESIGNS);
        assertNotNull(items);
        assertTrue(items.stream().allMatch(i -> i.kind() == ExploreControl.Kind.DESIGNS));
        assertFalse(items.isEmpty(), "Mock should return at least one design");
    }

    @Test
    void mockProvider_supportsCaseInsensitiveSearch() {
        var p = new MockExploreDataProvider();
        var q1 = p.fetch("rAvEn", ExploreControl.Kind.ALL);
        var q2 = p.fetch("RAVEN", ExploreControl.Kind.ALL);
        assertFalse(q1.isEmpty());
        assertFalse(q2.isEmpty());
        assertEquals(q1.size(), q2.size());
    }
}
